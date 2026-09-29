# NexAlarm — 專案工作守則

開工先讀同目錄的 `PROJECT_MAP.md`，依功能入口定位；共同合作方式、獨立判斷及交付規則由 `~/.codex/AGENTS.md` 管理。此檔只補充本專案規則。

## 特殊規則

- Kotlin／Compose；資料透過 Repository、Room、Flow／StateFlow，UI 文字集中 Strings.kt。
- 修改 Entity 時遞增 Room version、新增並註冊 migration，更新 schema 匯出。版本以程式為準（本次核對為 8），不可沿用舊文件 v7。
- 鬧鐘排程透過 AlarmScheduler；保留精確排程權限 fallback、開機恢復與時區／重複日轉換。
- Compose 設定透過 AppSettingsProvider；背景程序先同步偏好設定，不直接寫全域 Compose 狀態。
- 可靠性需實體裝置測試；缺少裝置時清楚回報未驗證 Doze／省電等情境。
- Firebase 設定、簽章 keystore 與密鑰不提交；正式簽章／Play 發佈沿用既有授權流程。

## 驗證與交付

`./gradlew test assembleDebug`；裝置驗證見 TESTING.md，具備連接裝置時用 `./gradlew connectedAndroidTest`。

Android 交付 APK 與測試結果；release workflow 見 .github/workflows/release.yml，不因文件提交建立 release tag。介紹頁目前由 /var/www/alarm.nex11.me 提供，網站修改需先核對 website/ 與該發布目錄的對照。
