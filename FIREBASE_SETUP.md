# Firebase Crashlytics 配置指南

本文檔說明如何配置 Firebase Crashlytics 以啟用生產環境的遠程崩潰報告。

## 前置條件

1. 有效的 Google 帳號
2. Firebase 專案（可免費建立）
3. Google Play Console 帳號（上線後需要）

## 快速開始

### 步驟 1: 建立 Firebase 專案

1. 訪問 [Firebase Console](https://console.firebase.google.com/)
2. 點擊 **「新增專案」**
3. 輸入專案名稱：`NexAlarm` (或任意名稱)
4. 按照指引完成設定
5. 選擇或建立 Google Cloud 資源位置（建議選擇距您最近的區域）

### 步驟 2: 新增 Android 應用

1. 在 Firebase Console 中，點擊 **「新增應用」** > **「Android」**
2. 填入應用資訊：
   - **Android 套件名稱：** `com.nexalarm.app`
   - **應用暱稱：** `NexAlarm` (選填)
   - **簽署憑證 SHA-1：** (暫時留空，稍後補充)
3. 點擊 **「註冊應用」**

### 步驟 3: 下載 google-services.json

1. 完成上一步後，Firebase 會提示下載 `google-services.json`
2. **下載檔案**
3. **複製到 `app/` 目錄中**（與 `build.gradle.kts` 同層級）
4. **不要將此檔案提交到公開倉庫**（已在 .gitignore 中）

### 步驟 4: 配置構建腳本

已在 `app/build.gradle.kts` 中添加依賴：

```gradle
// Firebase Crashlytics
implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
implementation("com.google.firebase:firebase-crashlytics-ktx")
implementation("com.google.firebase:firebase-analytics-ktx")
```

### 步驟 5: 配置簽署憑證（用於 Release 構建）

當準備發佈到 Google Play 時，需要配置簽署憑證以供 Crashlytics 使用：

1. 生成簽署金鑰（如果還沒有）：
   ```bash
   keytool -genkey -v -keystore release.keystore -alias nexalarm \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

2. 取得 SHA-1 指紋：
   ```bash
   keytool -list -v -keystore release.keystore -alias nexalarm
   ```

3. 複製 SHA-1 值到 Firebase Console 應用設定

### 步驟 6: 驗證集成

1. **構建應用：**
   ```bash
   ./gradlew assembleDebug
   ```

2. **安裝並執行：**
   ```bash
   ./gradlew installDebug
   adb shell am start -n com.nexalarm.app/.MainActivity
   ```

3. **測試崩潰報告（可選）：**
   - 在 App 中點擊某個功能
   - Firebase Crashlytics 可自動捕獲未捕獲的異常
   - 或透過 `CrashReportingManager.recordException(throwable)` 手動上報

4. **檢查 Firebase Console：**
   - 訪問 [Firebase Console](https://console.firebase.google.com/) > 您的專案
   - 進入 **Crashlytics** 標籤
   - 等待 2-5 分鐘，崩潰報告會出現在此

## 使用方式

### 自動捕獲

所有未捕獲的異常會自動上報到 Firebase Crashlytics。無需額外配置。

### 手動記錄異常

```kotlin
try {
    // 某些操作
} catch (e: Exception) {
    CrashReportingManager.recordException(e)
}
```

### 記錄自定義日誌

```kotlin
CrashReportingManager.log("User started timer at ${System.currentTimeMillis()}")
CrashReportingManager.logWarning("Alarm scheduling failed for ID: $alarmId")
CrashReportingManager.logError("Database migration error", exception)
```

### 設定自定義鍵值

```kotlin
CrashReportingManager.setCustomKey("alarm_type", "recurring")
CrashReportingManager.setCustomKey("snooze_count", 3)
CrashReportingManager.setCustomKey("is_silent_mode", true)
```

## 隱私與安全

- 崩潰報告在 Firebase 伺服器上加密存儲
- 預設不收集個人身份信息（PII）
- 應用 ID 自動設為 "anonymous"（可在代碼中修改）
- 遵守 Google 隱私政策和 Android 開發者計畫政策

## 常見問題

### Q: 崩潰報告沒有出現在 Firebase Console

**A:**
- 確認 `google-services.json` 已正確複製到 `app/` 目錄
- 確認已執行 `./gradlew clean build` 強制重新編譯
- 等待 2-5 分鐘（Firebase 有延遲）
- 檢查應用網路連線正常

### Q: 開發過程中如何禁用崩潰報告？

**A:** 在 `CrashReportingManager.init()` 中修改：
```kotlin
crashlytics.setCrashlyticsCollectionEnabled(BuildConfig.IS_PRODUCTION)
```

### Q: 如何測試崩潰報告功能？

**A:** 可以在 `SettingsScreen` 中添加一個測試按鈕：
```kotlin
Button(onClick = { throw RuntimeException("Test crash") }) {
    Text("Test Crash Report")
}
```

### Q: 如何在生產構建中禁用本地 crash logs？

**A:** 修改 `CrashReportingManager` 中的 `install()` 方法，根據 `BuildConfig.IS_PRODUCTION` 有條件地調用。

## 後續步驟

1. ✅ Firebase Crashlytics 已集成
2. ⏳ 建立 GitHub Actions CI/CD Pipeline（Task #1）
3. ⏳ 測試遠程崩潰報告功能
4. ⏳ 監控生產環境的崩潰率和用戶影響

## 相關文件

- [Firebase Crashlytics 文檔](https://firebase.google.com/docs/crashlytics)
- [Firebase Crashlytics Kotlin 指南](https://firebase.google.com/docs/crashlytics/get-started?platform=android)
- [NexAlarm CrashReportingManager 源代碼](app/src/main/java/com/nexalarm/app/util/CrashReportingManager.kt)

---

**最後更新：** 2026-03-27
