/**
 * NexAlarm MCP Server — 雲端版（無需 ADB）
 * 透過 alarm.nex11.me 後端 API，手機只要有網路就自動同步
 *
 * 啟動方式：
 *   NEXALARM_USER=你的帳號 NEXALARM_PASS=你的密碼 node index-cloud.js
 */
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import {
  listAlarms, getAlarm, addAlarm, deleteAlarm,
  toggleAlarm, loginWithCredentials, logout,
  formatRepeatDays,
} from "./alarm-ops-cloud.js";

const server = new McpServer({ name: "NexAlarm Cloud", version: "2.0.0" });

// ── login ───────────────────────────────────────────────────────────────────
server.tool(
  "nexalarm_login",
  "登入 NexAlarm 帳號（取得 token 後快取 7 天，之後不需重複登入）",
  {
    username_or_email: z.string().describe("帳號或 Email"),
    password: z.string().describe("密碼"),
  },
  async ({ username_or_email, password }) => {
    const data = await loginWithCredentials(username_or_email, password);
    return {
      content: [{
        type: "text",
        text: `✅ 登入成功！歡迎 ${data.user?.display_name || data.user?.username || username_or_email}`,
      }],
    };
  }
);

// ── logout ──────────────────────────────────────────────────────────────────
server.tool("nexalarm_logout", "登出 NexAlarm", {}, async () => {
  logout();
  return { content: [{ type: "text", text: "✅ 已登出" }] };
});

// ── list_alarms ─────────────────────────────────────────────────────────────
server.tool(
  "list_alarms",
  "列出所有鬧鐘（從雲端取得，手機自動同步）",
  { enabled_only: z.boolean().optional().describe("true = 只列啟用中的鬧鐘") },
  async ({ enabled_only = false }) => {
    const alarms = await listAlarms({ enabledOnly: enabled_only });
    if (alarms.length === 0)
      return { content: [{ type: "text", text: "目前沒有鬧鐘。" }] };

    const lines = alarms.map((a) => {
      const h = String(a.hour).padStart(2, "0");
      const m = String(a.minute).padStart(2, "0");
      const tags = [a.isSilent ? "🔇" : "", a.snoozeEnabled ? "💤" : ""].filter(Boolean).join("");
      return `[${a.clientId.slice(0, 8)}] ${h}:${m} ${a.isEnabled ? "✅" : "⭕"} "${a.title || "（無標題）"}" | ${formatRepeatDays(a.repeatDays)}${tags ? " " + tags : ""}`;
    });
    return {
      content: [{
        type: "text",
        text: `共 ${alarms.length} 個鬧鐘：\n\n${lines.join("\n")}\n\n（ID 為 clientId 前 8 碼，刪除時需提供完整 UUID）`,
      }],
    };
  }
);

// ── get_alarm ───────────────────────────────────────────────────────────────
server.tool(
  "get_alarm",
  "取得單一鬧鐘詳細資訊",
  { client_id: z.string().describe("鬧鐘的 clientId（UUID）") },
  async ({ client_id }) => {
    const a = await getAlarm(client_id);
    if (!a) return { content: [{ type: "text", text: `找不到 ${client_id}` }] };
    const h = String(a.hour).padStart(2, "0");
    const m = String(a.minute).padStart(2, "0");
    return {
      content: [{
        type: "text",
        text: [
          `clientId: ${a.clientId}`, `時間: ${h}:${m}`,
          `標題: ${a.title || "（無）"}`, `狀態: ${a.isEnabled ? "開啟" : "關閉"}`,
          `重複: ${formatRepeatDays(a.repeatDays)}`,
          `靜音: ${a.isSilent ? "是" : "否"}`, `貪睡: ${a.snoozeEnabled ? "是" : "否"}`,
        ].join("\n"),
      }],
    };
  }
);

// ── add_alarm ───────────────────────────────────────────────────────────────
server.tool(
  "add_alarm",
  "新增鬧鐘（上傳到雲端，手機 App 15 分鐘內自動同步）",
  {
    time: z.string().describe('時間 HHMM，例如 "0730"'),
    title: z.string().optional().describe("鬧鐘名稱（可選）"),
    repeat: z.string().optional().describe('"每天" 或 "1,2,3,4,5"（可選）'),
    silent: z.boolean().optional().describe("靜音（可選）"),
  },
  async (args) => {
    const clientId = await addAlarm(args);
    const h = args.time.slice(0, 2).padStart(2, "0");
    const m = args.time.slice(2, 4).padStart(2, "0");
    return {
      content: [{
        type: "text",
        text: `✅ 已新增：${h}:${m}${args.title ? ` "${args.title}"` : ""}\n手機 App 15 分鐘內會自動同步。`,
      }],
    };
  }
);

// ── delete_alarm ────────────────────────────────────────────────────────────
server.tool(
  "delete_alarm",
  "刪除鬧鐘（by clientId UUID）",
  { client_id: z.string().describe("鬧鐘的完整 clientId UUID") },
  async ({ client_id }) => {
    await deleteAlarm(client_id);
    return { content: [{ type: "text", text: `✅ 已刪除` }] };
  }
);

// ── toggle_alarm ────────────────────────────────────────────────────────────
server.tool(
  "toggle_alarm",
  "開啟或關閉某個鬧鐘",
  {
    client_id: z.string().describe("鬧鐘的 clientId UUID"),
    enabled: z.boolean().describe("true = 開啟，false = 關閉"),
  },
  async ({ client_id, enabled }) => {
    await toggleAlarm(client_id, enabled);
    return {
      content: [{
        type: "text",
        text: `✅ 已${enabled ? "開啟" : "關閉"}鬧鐘`,
      }],
    };
  }
);

// ── Start ───────────────────────────────────────────────────────────────────
const transport = new StdioServerTransport();
await server.connect(transport);
