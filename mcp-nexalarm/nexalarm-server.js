/**
 * 建立並回傳已掛載所有工具的 McpServer（不含 transport，供 stdio / HTTP 共用）
 */
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import {
  listAlarms, getAlarm, listFolders,
  addAlarm, deleteAlarm, toggleFolder,
  formatRepeatDays,
} from "./alarm-ops.js";

export function createServer() {
  const server = new McpServer({ name: "NexAlarm", version: "1.0.0" });

  // ── list_alarms ─────────────────────────────────────────────────────────────
  server.tool(
    "list_alarms",
    "列出 NexAlarm 中所有鬧鐘（ID、時間、標題、資料夾、重複日、狀態）",
    { enabled_only: z.boolean().optional().describe("true = 只列已啟用") },
    async ({ enabled_only = false }) => {
      const rows = await listAlarms({ enabledOnly: enabled_only });
      if (rows.length === 0)
        return { content: [{ type: "text", text: "目前沒有鬧鐘。" }] };

      const lines = rows.map((r) => {
        const h = String(r.hour).padStart(2, "0");
        const m = String(r.minute).padStart(2, "0");
        const tags = [r.isSilent ? "🔇" : "", r.snoozeEnabled ? "💤" : ""].filter(Boolean).join("");
        return `[ID:${r.id}] ${h}:${m} ${r.isEnabled ? "✅" : "⭕"} "${r.title || "（無標題）"}" | ${r.folderName ? "📁 " + r.folderName : "📂 無資料夾"} | ${formatRepeatDays(r.repeatDays)}${tags ? " " + tags : ""}`;
      });
      return { content: [{ type: "text", text: `共 ${rows.length} 個鬧鐘：\n\n${lines.join("\n")}` }] };
    }
  );

  // ── get_alarm ───────────────────────────────────────────────────────────────
  server.tool(
    "get_alarm",
    "取得單一鬧鐘的完整設定",
    { id: z.number().describe("鬧鐘 ID") },
    async ({ id }) => {
      const r = await getAlarm(id);
      if (!r) return { content: [{ type: "text", text: `找不到 ID:${id}` }] };
      const h = String(r.hour).padStart(2, "0");
      const m = String(r.minute).padStart(2, "0");
      return {
        content: [{
          type: "text",
          text: [
            `ID: ${r.id}`, `時間: ${h}:${m}`, `標題: ${r.title || "（無）"}`,
            `狀態: ${r.isEnabled ? "開啟" : "關閉"}`, `資料夾: ${r.folderName || "無"}`,
            `重複: ${formatRepeatDays(r.repeatDays)}`, `靜音: ${r.isSilent ? "是" : "否"}`,
            `貪睡: ${r.snoozeEnabled ? "開啟" : "關閉"}`,
            `貪睡間隔: ${r.snoozeIntervalMinutes ?? 5} 分鐘`,
            `最多貪睡: ${r.maxSnoozeCount === 0 ? "無限" : (r.maxSnoozeCount ?? 3) + " 次"}`,
          ].join("\n"),
        }],
      };
    }
  );

  // ── list_folders ────────────────────────────────────────────────────────────
  server.tool(
    "list_folders",
    "列出 NexAlarm 中所有使用者資料夾",
    {},
    async () => {
      const rows = await listFolders();
      if (rows.length === 0)
        return { content: [{ type: "text", text: "目前沒有使用者資料夾。" }] };
      const lines = rows.map((r) => `[ID:${r.id}] ${r.emoji} ${r.name} ${r.isEnabled ? "✅" : "⭕"} | ${r.alarmCount} 個鬧鐘`);
      return { content: [{ type: "text", text: `共 ${rows.length} 個資料夾：\n\n${lines.join("\n")}` }] };
    }
  );

  // ── add_alarm ───────────────────────────────────────────────────────────────
  server.tool(
    "add_alarm",
    "新增一個鬧鐘到 NexAlarm",
    {
      time: z.string().describe('時間 HHMM，例如 "0730"'),
      title: z.string().optional().describe("鬧鐘名稱（可選）"),
      folder: z.string().optional().describe("資料夾名稱（可選）"),
      repeat: z.string().optional().describe('重複日 "1,2,3,4,5" 或 "每天"（可選）'),
      silent: z.boolean().optional().describe("靜音（可選）"),
    },
    async (args) => {
      addAlarm(args);
      const h = args.time.slice(0, 2).padStart(2, "0");
      const m = args.time.slice(2, 4).padStart(2, "0");
      return {
        content: [{
          type: "text",
          text: `✅ 已新增：${h}:${m}${args.title ? ` "${args.title}"` : ""}${args.folder ? ` 📁 ${args.folder}` : ""}`,
        }],
      };
    }
  );

  // ── delete_alarm ────────────────────────────────────────────────────────────
  server.tool(
    "delete_alarm",
    "刪除指定 ID 的鬧鐘",
    { id: z.number().describe("鬧鐘 ID") },
    async ({ id }) => {
      deleteAlarm(id);
      return { content: [{ type: "text", text: `✅ 已刪除 ID:${id}` }] };
    }
  );

  // ── toggle_folder ───────────────────────────────────────────────────────────
  server.tool(
    "toggle_folder",
    "開啟或關閉指定資料夾（切換其中所有鬧鐘的啟用狀態）",
    { name: z.string().describe("資料夾名稱") },
    async ({ name }) => {
      toggleFolder(name);
      return { content: [{ type: "text", text: `✅ 已切換資料夾「${name}」` }] };
    }
  );

  return server;
}
