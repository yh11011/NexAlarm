# NexAlarm Launch Readiness

**更新日期：** 2026-04-08
**狀態：** 不可作為上線依據

這份文件曾宣稱「完全準備就緒」，但內容已失真，且未反映實際阻塞項目。自 2026-04-08 起，這份文件改為保守版摘要，只記錄目前仍會影響上架與發佈品質的事實。

## 目前已知阻塞

- Android lint 曾有阻塞性錯誤，必須保持 `./gradlew lintDebug` 為綠燈。
- AI 整合流程不得再把 token 放進 URL query string。
- 首頁不應預設觸發電池白名單要求，避免踩到 Play 政策。
- 開機廣播處理必須和 Manifest 宣告一致，包含 `QUICKBOOT_POWERON`。
- CI 不可使用 `continue-on-error: true` 掩蓋測試失敗。

## 使用規則

- 任何「可上線」判定，必須以當前 CI、lint、測試結果與人工檢查為準。
- 若有新風險被發現，直接更新這份文件，不要保留過時的樂觀結論。
- 若要恢復正式 readiness checklist，應重建為可驗證條目，而不是敘述式宣告。
