# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案說明

NexAlarm 是一個功能完整的現代 Android 鬧鐘應用程式，採用 Kotlin 和 Jetpack Compose 開發。**重要：使用者完全不懂 Android 開發，所有開發工作都由 AI 完成。AI 必須使用中文回答所有問題，包括技術解釋和程式碼說明。**

**核心特色**：
- 單次/重複鬧鐘管理，支援週期性排程
- 資料夾分類系統，含免費/付費版本限制（`FeatureFlags.FREE_FOLDER_LIMIT = 10`）
- 會議模式快捷磚片（震動模式切換）
- Deep Link 整合（`nexalarm://` URI scheme）
- 雙語支援（中文/英文）+ 深色/淺色主題 + 時區選擇
- 使用者帳號系統（登入/註冊）+ 雲端鬧鐘同步
- 付費版（Google Play Billing + 優惠碼）

## 技術棧

| 組件 | 技術 | 版本 |
|------|------|------|
| 程式語言 | Kotlin | 2.1.0 |
| UI 框架 | Jetpack Compose | 2025.01.00 BOM |
| 設計系統 | Material Design 3 | Latest |
| 架構模式 | MVVM (ViewModel + Repository + Room) | - |
| 資料庫 | Room Database | 2.6.1 |
| 導航 | Navigation Compose | 2.8.5 |
| 構建工具 | Android Gradle Plugin | 8.7.3 |
| 符號處理 | KSP | 2.1.0-1.0.29 |
| 背景任務 | WorkManager | Latest |
| 崩潰報告 | Firebase Crashlytics | BOM 33.0.0 |
| 加密儲存 | EncryptedSharedPreferences | security-crypto |
| 最低 SDK | API 26 (Android 8.0) | - |
| 目標 SDK | API 35 | - |
| 包名 | `com.nexalarm.app` | - |

## 開發命令

```bash
# Debug 構建
./gradlew assembleDebug

# 安裝到已連接的設備
./gradlew installDebug

# 清理後重新構建
./gradlew clean assembleDebug

# Release 構建（需 signing config）
./gradlew assembleRelease

# 本地單元測試（不需裝置）
./gradlew test

# 儀器測試（需連接實體裝置）
./gradlew connectedAndroidTest

# 執行特定測試類
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nexalarm.app.test.AlarmReliabilityTest
```

### ADB Deep Link 測試

```bash
adb shell am start -a android.intent.action.VIEW -d "nexalarm://add?time=0730&title=WakeUp&folder=Study&repeat=1,2,3,4,5&silent=true"
adb shell am start -a android.intent.action.VIEW -d "nexalarm://add?time=0700&title=Morning"
adb shell am start -a android.intent.action.VIEW -d "nexalarm://delete?id=1"
adb shell am start -a android.intent.action.VIEW -d "nexalarm://toggle_folder?name=Study"
```

## 架構設計

### 分層架構 (MVVM)

**資料層 (Data Layer)**
- `NexAlarmDatabase`：Room 資料庫，**目前版本 7**，含 `AlarmEntity`、`FolderEntity` 及 6 個遷移腳本（MIGRATION_1_2 ～ MIGRATION_6_7）
- `PrepopulateCallback`：首次建立 DB 時插入系統資料夾（"單次鬧鐘"🔔、"重複鬧鐘"↻）
- `RepeatDaysConverter`：TypeConverter，`List<Int>` ↔ 逗號分隔字串
- `AlarmEntity` 欄位包含 `clientId`（UUID，雲端同步用）和 `updatedAt`（Unix ms）

**倉庫層 (Repository)**
- `AlarmRepository.insertOrUpdate()`：含去重邏輯（依 hour + minute + title + folderId + repeatDays 比對），防止 Deep Link 重複新增
- `FolderRepository`：含 `FeatureFlags` 付費檢查
- `AuthRepository`：登入/註冊，透過 `ApiClient` 呼叫後端，JWT token 存入 `SettingsManager`（EncryptedSharedPreferences）
- `AlarmSyncRepository`：序列化本地鬧鐘並與伺服器雙向合併

**展示層 (Presentation)**
- `AlarmViewModel`, `FolderViewModel`：使用 `StateFlow`，繼承 `AndroidViewModel`
- `TimerViewModel` / `StopwatchViewModel`：計時器和碼錶

### 全域狀態管理（重要）

`isDarkTheme` 和 `isAppEnglish` 是全域 Compose 狀態，定義在 `ui/theme/Theme.kt`，但實際由 `AppSettingsProvider` 管理：

```kotlin
// Theme.kt 中的委派
var isDarkTheme: Boolean
    get() = AppSettingsProvider.isDarkThemeMutableState.value
    set(value) = AppSettingsProvider.setDarkMode(value)
```

**規則**：
- Compose UI：直接讀寫 `isDarkTheme` / `isAppEnglish`（觸發重組）
- 非 Compose 背景（AlarmService、BroadcastReceiver）：使用 `AppSettingsProvider.syncFromSharedPreferences()` 確保狀態同步，不可直接寫全域變數

### 國際化

所有使用者可見文字放在 `ui/theme/Strings.kt` 的 `object S`。動態字串用函數形式：`fun alarmCount(count: Int): String`。新增文字一律加到 `S` 而非 hardcode。

### 導航架構（三層結構）

所有導航邏輯集中在 `AppNavigation.kt` 的 `NexAlarmMainContent()`，`MainActivity.kt` 只負責權限請求和 Deep Link 入口。

1. **`ModalNavigationDrawer`**（側邊抽屜）：Home、Settings、Account
2. **`HorizontalPager`**（4 個主 Tab，路由 `"tabs"`）：
   - 頁 0：`AlarmScreen`（鬧鐘列表）
   - 頁 1：`FolderManageScreen`（資料夾管理）
   - 頁 2：`StopwatchScreen`（碼錶）
   - 頁 3：`TimerScreen`（計時器）
3. **`NavController`**（全頁 push）：`alarm_edit/{alarmId}?folderId={folderId}`、`folder_detail/{folderId}`、`home`、`settings`、`account`

**FAB 行為**：Tab 0 → `alarm_edit/-1`；Tab 1 → 新增資料夾對話框；Tab 2/3 無 FAB。

### 鬧鐘系統

**排程流程**：ViewModel → `AlarmRepository` → `AlarmScheduler.schedule(alarm)`
- 優先使用 `AlarmManager.setAlarmClock()`（繞過 Doze，狀態列顯示鬧鐘圖示）
- 無精確排程權限時 fallback 到 `setAndAllowWhileIdle()`

**觸發流程**：`AlarmReceiver` → `AlarmService`（前台服務）→ `AlarmRingingActivity`（全螢幕 UI）

**重複鬧鐘格式**：內部格式 1=週一...7=週日；`Calendar` 格式 1=週日。轉換在 `AlarmScheduler.calculateNextTriggerTime()`。

**`BootReceiver`**：開機後重新排程所有 `isEnabled=true` 的鬧鐘。

### 雲端同步

`AlarmSyncWorker`（WorkManager）每 15 分鐘執行一次，僅在已登入（`SettingsManager.authToken != null`）且有網路時運行。衝突解決策略：`serverUpdatedAt > localUpdatedAt` 時伺服器版本勝出。同步失敗返回 `Result.retry()`（指數退避）。

### 付費功能

- `FeatureFlags.isPremium`：全域付費狀態，在 `NexAlarmApp.onCreate` 從 `SettingsManager` 恢復
- `BillingManager`：Application 級單例（`NexAlarmApp.billingManager`），Google Play Billing 一次性購買，商品 ID `nexalarm_premium`
- 免費版資料夾上限：10 個（不含系統資料夾）
- 亦支援優惠碼兌換（透過後端 API 驗證）

## 重要檔案

### 核心

- `NexAlarmApp.kt`：Application 初始化順序：CrashReportingManager → NotificationHelper → AppSettingsProvider → FeatureFlags → schedulePeriodicSync
- `MainActivity.kt`：權限請求（通知、精確鬧鐘、電池優化白名單）+ Deep Link 處理
- `AppNavigation.kt`：完整導航結構（Drawer + Pager + NavController）

### 設定與狀態

- `util/AppSettingsProvider.kt`：全域設定單一真實源（`isDarkTheme`、`isAppEnglish`），橋接 Compose 和非 Compose 環境
- `data/SettingsManager.kt`：SharedPreferences 封裝；敏感資料（JWT token、userId、username）使用 EncryptedSharedPreferences；亦儲存 `timeZoneId`（nullable，null = 跟隨系統）
- `util/FeatureFlags.kt`：付費功能開關（`object`，可運行期修改）
- `ui/theme/Strings.kt`：所有國際化字串

### 鬧鐘系統

- `util/AlarmScheduler.kt`：核心排程，`setAlarmClock()` + 時區/重複日計算
- `receiver/AlarmReceiver.kt` / `receiver/BootReceiver.kt`
- `service/AlarmService.kt`：前台服務（wake lock、播放鈴聲）
- `ui/screens/AlarmRingingActivity.kt`：全螢幕鬧鐘 UI
- `service/MeetingModeTileService.kt`：快捷磚片

### 資料庫

- `data/database/NexAlarmDatabase.kt`：**Room v7**，含 MIGRATION_1_2 ～ MIGRATION_6_7
- `data/model/AlarmEntity.kt`：核心模型，含 `clientId`（UUID）、`updatedAt`（Unix ms）、`snoozeEnabled` 等欄位
- `data/model/FolderEntity.kt`：含 `isSystem`、`emoji` 欄位

### 雲端與帳號

- `data/ApiClient.kt`：HTTP 客戶端（HttpURLConnection）
- `data/AuthRepository.kt`：登入/註冊 API 呼叫
- `data/AlarmSyncRepository.kt`：鬧鐘雙向同步邏輯
- `worker/AlarmSyncWorker.kt`：WorkManager 背景同步任務

### 工具

- `util/CrashReportingManager.kt`：Firebase Crashlytics + 本地 CrashHandler 統一入口
- `util/BillingManager.kt`：Google Play Billing 整合
- `util/NotificationHelper.kt`：通知頻道建立
- `util/AlarmTestHook.kt`：測試專用掛鉤

## 開發注意事項

### DB Schema 變更（必讀）

每次修改 `AlarmEntity` 或 `FolderEntity` **必須**：
1. 遞增 `NexAlarmDatabase` 的 `version`（目前為 7）
2. 新增對應的 `MIGRATION_X_Y` 並加入 `addMigrations(...)` 清單

跳過此步驟會導致 `IllegalStateException: Room cannot verify the data integrity`。

### 鬧鐘功能

- 務必在**實體裝置**上測試（模擬器無法模擬 Doze 模式）
- 新增/更新鬧鐘一律呼叫 `AlarmScheduler.schedule(alarm)`，不可直接操作 `AlarmManager`
- API 31+ 需先確認 `alarmManager.canScheduleExactAlarms()`（`AlarmScheduler` 已處理 fallback）

### 程式碼規範

- 複雜業務邏輯使用中文註解（符合現有程式碼庫風格）
- UI 一律使用 `@Composable` 函數，不使用 XML 佈局
- 所有 DB 操作使用 `suspend`，UI 層透過 `Flow`/`StateFlow` 觀察
- build.gradle.kts 使用 Kotlin DSL 語法（`isMinifyEnabled`、`buildConfigField("boolean", ...)` 等，**不是** Groovy 語法）

### Firebase 設定

Firebase Crashlytics 需要 `app/google-services.json`（從 Firebase Console 下載）。缺少此檔案時 Release build 會失敗。詳見 `FIREBASE_SETUP.md`。

## CI/CD

`.github/workflows/` 含三個 workflow：
- `ci.yml`：每次 push/PR 自動 lint + debug build
- `release.yml`：手動觸發或 `v*` tag push，自動 Release build + 建立 GitHub Release
- `security-check.yml`：每週一自動掃描依賴安全性

## 權限需求

- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`：精確鬧鐘（API 31+ 需使用者手動開啟）
- `POST_NOTIFICATIONS`：鬧鐘通知（API 33+ 需執行期請求）
- `USE_FULL_SCREEN_INTENT`：鎖定螢幕顯示
- `WAKE_LOCK`：鬧鐘響鈴期間保持裝置喚醒
- `RECEIVE_BOOT_COMPLETED`：重新開機後重新排程
- `VIBRATE`、`FOREGROUND_SERVICE`
