# Historical device smoke test — 2026-03-27

> This is a retained point-in-time note, not current release qualification. It was not rerun against the current commit, did not cover all 14 reliability scenarios, and includes observations that still require independent confirmation.

## 測試環境
- 裝置：Xiaomi 24129PN74G (Android 15)
- 測試時間：2026-03-27
- APK 版本：1.0.0 (versionCode=1)
- ADB 限制：裝置安全政策封鎖 INJECT_EVENTS（無法模擬觸控），改用 Deep Link + am start + logcat

---

## 當時觀察到的項目

| 項目 | 結果 |
|------|------|
| App 安裝 | 成功 |
| App 啟動 | 正常，無崩潰 |
| Crashlytics 初始化 | 已修復（Build ID 正確注入） |
| crash log (logcat -b crash) | 該次觀察期間未見 crash |
| Error log (nexalarm 相關) | 零錯誤 |
| Deep Link: 新增鬧鐘 | ✅ 07:00 / 08:00 / 22:30 成功新增並顯示 |
| Deep Link: 刪除鬧鐘 | ✅ 09:30 成功刪除，UI 即時更新 |
| 倒數計時顯示 | ✅「6 小時 X 分鐘後響鈴」正確計算 |
| AlarmManager 排程 | ✅ com.nexalarm.app 出現在 alarm 統計，07:00 RTC_WAKEUP 已排程 |
| SCHEDULE_EXACT_ALARM | 當時裝置顯示於申請／active UID 清單；未保存完整權限輸出 |
| 通知頻道 alarm_channel | ✅ importance=4(HIGH)，正確建立 |
| POST_NOTIFICATIONS 權限 | ✅ granted=true |
| 電池優化白名單 | ✅ WHITELISTED（WorkManager 確認） |
| WorkManager 同步排程 | ✅ 上次執行 11 分鐘前，下次約 4 分鐘後，狀態 RUNNABLE |
| 底部導覽列 4 個 Tab | ✅ 鬧鐘、資料夾、碼錶、計時 均顯示 |
| 碼錶頁面 | ✅ 00:00.00 + 開始按鈕正確顯示 |
| 版本資訊 | ✅ versionCode=1, versionName=1.0.0, minSdk=26, targetSdk=35 |

---

## ⚠️ 已知限制（非 App Bug）

| 項目 | 說明 |
|------|------|
| Deep Link 標題參數 | 透過 ADB shell 傳送 URL 時，`&` 後的參數在 Windows 環境可能被截斷，title 無法傳入。**這是 ADB/Windows 的 shell 問題，不是 App bug**；實際用 Shortcut/自動化 App 觸發時正常。 |
| UI 觸控測試 | 裝置封鎖 INJECT_EVENTS，無法自動化點擊；設定頁、帳號頁、側邊欄需手動驗證 |
| DND 音訊行為 | `bypassDnd=false`；此記錄沒有保留足夠證據證明所有 DND 設定下都會發聲，需重新測試 |

---

## 🔧 本次修復項目

1. **Firebase Crashlytics 崩潰** - 移除錯誤 afterEvaluate workaround，改用官方 `mappingFileUploadEnabled=false` 設定
2. **帳號頁修改密碼功能** - 新增完整 Dialog + API + 字串支援
