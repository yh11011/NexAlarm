# NexAlarm 後端修改需求紀錄

日期：2026-04-08

用途：
- 集中記錄所有需要後端配合的修改
- 讓 App 端與後端實作有一致的接口規格
- 後續如有新的後端需求，直接追加在此文件

---

## 2026-04-08 AI 整合安全修正

背景：
- Android App 原本在 AI 整合流程中，直接把使用者登入 JWT token 放進 `https://login.nex11.me/ai-setup?...` 的 URL query string。
- 這會讓 token 暴露在瀏覽器歷史、server access log、代理層與第三方觀測資料中，屬於高風險安全問題。
- App 端已改為不再傳遞 token query string，改成先向後端交換一次性 setup session。

### 需要新增的 API

`POST https://alarm.nex11.me/auth/ai/setup-session`

### 驗證方式

- Header:
  - `Authorization: Bearer <jwt>`
  - `Content-Type: application/json`

### Request Body

```json
{
  "model": "chatgpt"
}
```

說明：
- `model` 為目標 AI 服務代號。
- 目前 App 端可能傳入的值包含：
  - `claude`
  - `chatgpt`
  - `gemini`
  - `copilot`
  - `grok`
  - `cursor`
  - `perplexity`
  - `deepseek`
  - `kimi`
  - `doubao`
  - `qwen`
  - `wenxin`
  - `chatglm`

### 成功回應格式

後端至少支援以下其中一種：

方案 A，直接回傳可開啟的安全 URL：

```json
{
  "setup_url": "https://login.nex11.me/ai-setup?model=chatgpt&code=short_lived_code"
}
```

方案 B，回傳一次性短期授權碼：

```json
{
  "code": "short_lived_code"
}
```

若採方案 B，App 端會自行組成：

```text
https://login.nex11.me/ai-setup?model=<model>&code=<code>
```

### 授權碼要求

- 必須為一次性使用
- 必須有短效期限，建議 1 到 5 分鐘
- 必須和登入使用者綁定
- 必須可被後端兌換成 server-side session，不能直接回傳原始 JWT
- 使用後立即失效
- 過期後不可重用

### 建議後端流程

1. 驗證 Bearer token 對應的登入使用者
2. 驗證 `model` 是否為允許清單中的值
3. 建立一次性 code 或短期 session
4. 將 code 與 user/session/model 綁定
5. 回傳 `setup_url` 或 `code`
6. `login.nex11.me/ai-setup` 收到 code 後，在伺服器端完成 session exchange
7. 完成後清除該 code，避免重放

### 失敗回應建議

未授權：

```json
{
  "detail": "Unauthorized"
}
```

參數錯誤：

```json
{
  "detail": "Invalid model"
}
```

伺服器錯誤：

```json
{
  "detail": "Unable to create AI setup session"
}
```

### 注意事項

- 不可再接受 `token` query string 作為登入依據
- 若現有 `https://login.nex11.me/ai-setup` 仍支援 `token` query，應盡快下線
- 伺服器 access log 不應記錄敏感憑證內容
- 若有 redirect，請避免在 redirect URL 中帶入長期憑證

### App 端目前狀態

- App 已改為呼叫 `POST /auth/ai/setup-session`
- 若後端尚未實作，AI 整合頁會顯示錯誤訊息，不會再退回不安全的 token URL 流程

