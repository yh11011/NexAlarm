/**
 * 共用的 ADB / DB 操作，供 stdio 和 HTTP server 共用
 */
import { execFileSync } from "child_process";
import { existsSync, unlinkSync, readFileSync } from "fs";
import { tmpdir } from "os";
import { join } from "path";
import initSqlJs from "sql.js";

export const PACKAGE = "com.nexalarm.app";
const TEMP_DB = join(tmpdir(), "nexalarm_temp.db");

// ── ADB ───────────────────────────────────────────────────────────────────────

export function adb(args) {
  try {
    return execFileSync("adb", args, { encoding: "utf8", timeout: 10000 });
  } catch (e) {
    throw new Error(`ADB 錯誤: ${e.stderr || e.message}`);
  }
}

export function deepLink(uri) {
  adb(["shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", uri]);
}

// ── DB ────────────────────────────────────────────────────────────────────────

function pullDatabase() {
  if (existsSync(TEMP_DB)) unlinkSync(TEMP_DB);
  adb(["shell", `run-as ${PACKAGE} cp databases/nexalarm_database /sdcard/nexalarm_mcp.db`]);
  adb(["pull", "/sdcard/nexalarm_mcp.db", TEMP_DB]);
  adb(["shell", "rm -f /sdcard/nexalarm_mcp.db"]);
}

export async function openDB() {
  pullDatabase();
  const SQL = await initSqlJs();
  const buf = readFileSync(TEMP_DB);
  return new SQL.Database(buf);
}

export function queryAll(db, sql, params = []) {
  const stmt = db.prepare(sql);
  stmt.bind(params);
  const rows = [];
  while (stmt.step()) rows.push(stmt.getAsObject());
  stmt.free();
  return rows;
}

export function queryOne(db, sql, params = []) {
  const stmt = db.prepare(sql);
  stmt.bind(params);
  const row = stmt.step() ? stmt.getAsObject() : null;
  stmt.free();
  return row;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const DAY_NAMES = ["", "週一", "週二", "週三", "週四", "週五", "週六", "週日"];

export function formatRepeatDays(raw) {
  if (!raw) return "不重複";
  const days = raw.split(",").map(Number).filter(Boolean);
  if (days.length === 0) return "不重複";
  if (days.length === 7) return "每天";
  return days.map((d) => DAY_NAMES[d] ?? d).join("、");
}

export function parseRepeat(input) {
  if (!input) return "";
  if (input === "每天") return "1,2,3,4,5,6,7";
  return input.toString().replace(/[^0-9,]/g, "");
}

// ── 業務操作 ──────────────────────────────────────────────────────────────────

/** 列出所有鬧鐘 */
export async function listAlarms({ enabledOnly = false } = {}) {
  const db = await openDB();
  try {
    const where = enabledOnly ? "AND a.isEnabled = 1" : "";
    return queryAll(
      db,
      `SELECT a.id, a.hour, a.minute, a.title, a.isEnabled,
              a.repeatDays, a.isSilent, a.snoozeEnabled, f.name AS folderName
       FROM alarms a
       LEFT JOIN folders f ON a.folderId = f.id
       WHERE (a.isDeleted IS NULL OR a.isDeleted = 0) ${where}
       ORDER BY a.hour, a.minute`
    );
  } finally {
    db.close();
  }
}

/** 取得單一鬧鐘 */
export async function getAlarm(id) {
  const db = await openDB();
  try {
    return queryOne(
      db,
      `SELECT a.*, f.name AS folderName FROM alarms a
       LEFT JOIN folders f ON a.folderId = f.id WHERE a.id = ?`,
      [id]
    );
  } finally {
    db.close();
  }
}

/** 列出使用者資料夾 */
export async function listFolders() {
  const db = await openDB();
  try {
    return queryAll(
      db,
      `SELECT f.id, f.name, f.emoji, f.isEnabled, COUNT(a.id) AS alarmCount
       FROM folders f
       LEFT JOIN alarms a ON a.folderId = f.id
         AND (a.isDeleted IS NULL OR a.isDeleted = 0)
       WHERE f.isSystem = 0
       GROUP BY f.id ORDER BY f.id`
    );
  } finally {
    db.close();
  }
}

/** 新增鬧鐘（透過 Deep Link） */
export function addAlarm({ time, title, folder, repeat, silent }) {
  const params = new URLSearchParams();
  params.set("time", time);
  if (title) params.set("title", title);
  if (folder) params.set("folder", folder);
  if (repeat) params.set("repeat", parseRepeat(repeat));
  if (silent) params.set("silent", "true");
  deepLink(`nexalarm://add?${params.toString()}`);
}

/** 刪除鬧鐘（透過 Deep Link） */
export function deleteAlarm(id) {
  deepLink(`nexalarm://delete?id=${id}`);
}

/** 切換資料夾開關（透過 Deep Link） */
export function toggleFolder(name) {
  deepLink(`nexalarm://toggle_folder?name=${encodeURIComponent(name)}`);
}
