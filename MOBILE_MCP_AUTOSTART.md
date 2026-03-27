# NexAlarm Mobile MCP 自動啟動說明

本專案已設定自動啟動 mobile-mcp，進入專案根目錄時可執行下列指令：

```
powershell -ExecutionPolicy Bypass -File .\start-mobile-mcp.ps1
```

或將此指令加入 Copilot CLI 啟動流程。

---

start-mobile-mcp.ps1 內容：
```
npx @mobilenext/mobile-mcp@latest
```
