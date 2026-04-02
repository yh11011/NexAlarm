/**
 * 雲端版 alarm 操作 — 透過 alarm.nex11.me 後端 API
 * 不需要 ADB，手機只要有網路就能同步
 */
import { randomUUID } from "crypto";
import { readFileSync, writeFileSync, existsSync } from "fs";
import { join, dirname } from "path";
import { fileURLToPath } from "url";

const __dir = dirname(fileURLToPath(import.meta.url));
const TOKEN_FILE = join(__dir, ".token-cache.json");

const BASE_URL = "https://alarm.nex11.me/auth";
const SYNC_URL = `${BASE_URL}/alarms/sync`;

// ── Token 管理 ─────────────────────────────────────────────────────────────

let _token = null;
let _credentials = null; // { usernameOrEmail, password }

/** 從快取讀取 token */
function loadCachedToken() {
  try {
    if (existsSync(TOKEN_FILE)) {
      const data = JSON.parse(readFileSync(TOKEN_FILE, "utf8"));
      if (data.token && data.expiresAt > Date.now()) {
        _token = data.token;
        return true;
      }
    }
  } catch {}
  return false;
}

/** 儲存 token 到快取（JWT 通常 30 天有效，這裡保守設 7 天） */
function saveToken(token) {
  _token = token;
  writeFileSync(
    TOKEN_FILE,
    JSON.stringify({ token, expiresAt: Date.now() + 7 * 24 * 60 * 60 * 1000 }),
    "utf8"
  );
}

/** 登入取得 token */
async function login(usernameOrEmail, password) {
  const res = await fetch(`${BASE_URL}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username_or_email: usernameOrEmail, password }),
  });
  if (!res.ok) {
    const err = await res.text();
    throw new Error(`登入失敗 (${res.status}): ${err}`);
  }
  const data = await res.json();
  saveToken(data.access_token);
  return data;
}

/** 取得有效 token（已快取則直接用，否則重新登入） */
async function getToken() {
  if (_token) return _token;
  if (loadCachedToken()) return _token;

  // 從環境變數讀取帳密
  const user = process.env.NEXALARM_USER || _credentials?.usernameOrEmail;
  const pass = process.env.NEXALARM_PASS || _credentials?.password;

  if (!user || !pass) {
    throw new Error(
      "請設定環境變數 NEXALARM_USER 和 NEXALARM_PASS，或先呼叫 nexalarm_login 工具"
    );
  }

  await login(user, pass);
  return _token;
}

/** 手動設定帳密（供 login tool 使用） */
export async function loginWithCredentials(usernameOrEmail, password) {
  _credentials = { usernameOrEmail, password };
  const data = await login(usernameOrEmail, password);
  return data;
}

/** 清除登入狀態 */
export function logout() {
  _token = null;
  _credentials = null;
  try {
    if (existsSync(TOKEN_FILE)) {
      writeFileSync(TOKEN_FILE, JSON.stringify({}), "utf8");
    }
  } catch {}
}

// ── Sync API 操作 ──────────────────────────────────────────────────────────

/**
 * 呼叫 sync endpoint
 * 傳入空陣列 → 取得所有伺服器鬧鐘（不覆蓋任何資料）
 * 傳入鬧鐘陣列 → 上傳新/更新的鬧鐘
 */
async function callSync(alarms = []) {
  const token = await getToken();
  const res = await fetch(SYNC_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ alarms }),
  });

  // token 過期則重新登入一次
  if (res.status === 401) {
    _token = null;
    const token2 = await getToken();
    const res2 = await fetch(SYNC_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token2}`,
      },
      body: JSON.stringify({ alarms }),
    });
    if (!res2.ok) throw new Error(`Sync 失敗 (${res2.status})`);
    return (await res2.json()).alarms ?? [];
  }

  if (!res.ok) throw new Error(`Sync 失敗 (${res.status}): ${await res.text()}`);
  return (await res.json()).alarms ?? [];
}

// ── 對外 API ───────────────────────────────────────────────────────────────

const DAY_NAMES = ["", "週一", "週二", "週三", "週四", "週五", "週六", "週日"];

export function formatRepeatDays(repeatDays) {
  if (!repeatDays || repeatDays.length === 0) return "不重複";
  if (repeatDays.length === 7) return "每天";
  return repeatDays.map((d) => DAY_NAMES[d] ?? d).join("、");
}

export function parseRepeat(input) {
  if (!input) return [];
  if (input === "每天") return [1, 2, 3, 4, 5, 6, 7];
  return input
    .toString()
    .split(",")
    .map(Number)
    .filter((n) => n >= 1 && n <= 7);
}

/** 列出所有鬧鐘（從伺服器取得） */
export async function listAlarms({ enabledOnly = false } = {}) {
  const serverAlarms = await callSync([]);
  return serverAlarms
    .filter((a) => !a.is_deleted)
    .filter((a) => !enabledOnly || a.data?.isEnabled)
    .sort((a, b) => {
      const ah = (a.data?.hour ?? 0) * 60 + (a.data?.minute ?? 0);
      const bh = (b.data?.hour ?? 0) * 60 + (b.data?.minute ?? 0);
      return ah - bh;
    })
    .map((a) => ({
      clientId: a.client_id,
      title: a.data?.title ?? "",
      hour: a.data?.hour ?? 0,
      minute: a.data?.minute ?? 0,
      isEnabled: a.data?.isEnabled ?? true,
      repeatDays: a.data?.repeatDays ?? [],
      isSilent: a.data?.vibrateOnly ?? false,
      snoozeEnabled: a.data?.snoozeEnabled ?? false,
      folderName: null, // 伺服器不儲存資料夾名稱，只有 folderId
      updatedAt: a.updated_at,
    }));
}

/** 取得單一鬧鐘 */
export async function getAlarm(clientId) {
  const all = await listAlarms();
  return all.find((a) => a.clientId === clientId) ?? null;
}

/** 新增鬧鐘 */
export async function addAlarm({ time, title, repeat, silent }) {
  const hour = parseInt(time.slice(0, 2), 10);
  const minute = parseInt(time.slice(2, 4), 10);
  const repeatDays = parseRepeat(repeat);
  const now = Date.now();

  const alarm = {
    client_id: randomUUID(),
    updated_at: now,
    is_deleted: false,
    data: {
      title: title || "",
      hour,
      minute,
      isEnabled: true,
      isRecurring: repeatDays.length > 0,
      repeatDays,
      vibrateOnly: silent ?? false,
      volume: 80,
      snoozeDelay: 10,
      maxSnoozeCount: 3,
      keepAfterRinging: false,
      snoozeEnabled: true,
      createdAt: now,
    },
  };

  await callSync([alarm]);
  return alarm.client_id;
}

/** 刪除鬧鐘（by clientId） */
export async function deleteAlarm(clientId) {
  const alarm = {
    client_id: clientId,
    updated_at: Date.now(),
    is_deleted: true,
    data: {},
  };
  await callSync([alarm]);
}

/** 更新鬧鐘啟用狀態 */
export async function toggleAlarm(clientId, enabled) {
  const existing = await getAlarm(clientId);
  if (!existing) throw new Error(`找不到 clientId: ${clientId}`);

  const alarm = {
    client_id: clientId,
    updated_at: Date.now(),
    is_deleted: false,
    data: {
      title: existing.title,
      hour: existing.hour,
      minute: existing.minute,
      isEnabled: enabled,
      isRecurring: existing.repeatDays.length > 0,
      repeatDays: existing.repeatDays,
      vibrateOnly: existing.isSilent,
      volume: 80,
      snoozeDelay: 10,
      maxSnoozeCount: 3,
      keepAfterRinging: false,
      snoozeEnabled: existing.snoozeEnabled,
      createdAt: existing.updatedAt,
    },
  };
  await callSync([alarm]);
}
