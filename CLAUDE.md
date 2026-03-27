# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案說明

NexAlarm 是一個功能完整的現代 Android 鬧鐘應用程式，採用 Kotlin 和 Jetpack Compose 開發。**重要：使用者完全不懂 Android 開發，所有開發工作都由 AI 完成。AI 必須使用中文回答所有問題，包括技術解釋和程式碼說明。**

**核心特色**:
- 單次/重複鬧鐘管理，支援週期性排程
- 資料夾分類系統，含免費/付費版本限制（`FeatureFlags.FREE_FOLDER_LIMIT = 10`）
- 會議模式快捷磚片（震動模式切換）
- Deep Link 整合 (`nexalarm://` URI scheme)
- Material Design 3 設計語言
- 雙語支援（中文/英文）+ 深色/淺色主題

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
| 符號處理 | KSP (Kotlin Symbol Processing) | 2.1.0-1.0.29 |
| 最低 SDK | API 26 (Android 8.0) | - |
| 目標 SDK | API 35 | - |
| 包名 | `com.nexalarm.app` | - |

## 開發命令

### 構建與部署
```bash
# Debug 構建
./gradlew assembleDebug

# 安裝到已連接的設備/模擬器
./gradlew installDebug

# 清理後重新構建
./gradlew clean assembleDebug
```

### 測試執行
```bash
# 本地單元測試（不需裝置）
./gradlew test

# 儀器測試（需連接實體裝置）
./gradlew connectedAndroidTest

# 執行特定測試類
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nexalarm.app.test.AlarmReliabilityTest
```

### ADB 測試命令
```bash
# 通過深度連結添加鬧鐘
adb shell am start -a android.intent.action.VIEW -d "nexalarm://add?time=0730&title=WakeUp&folder=Study&repeat=1,2,3,4,5&silent=true"

# 添加簡單鬧鐘
adb shell am start -a android.intent.action.VIEW -d "nexalarm://add?time=0700&title=Morning"

# 通過 ID 刪除鬧鐘
adb shell am start -a android.intent.action.VIEW -d "nexalarm://delete?id=1"

# 切換資料夾狀態
adb shell am start -a android.intent.action.VIEW -d "nexalarm://toggle_folder?name=Study"
```

## 架構設計

### 分層架構 (MVVM)

**資料層 (Data Layer)**
- `NexAlarmDatabase`: Room 資料庫，**目前版本 4**，含 `AlarmEntity`、`FolderEntity` 表格及 4 個遷移腳本（MIGRATION_1_2、MIGRATION_2_3、MIGRATION_3_4）
- `PrepopulateCallback`：首次建立 DB 時自動插入系統資料夾（"單次鬧鐘"🔔、"重複鬧鐘"🔁）
- `RepeatDaysConverter`：TypeConverter，重複日期 `List<Int>` ↔ 逗號分隔字串
- DAO：`AlarmDao`, `FolderDao` 提供 suspend 函數和 Flow

**倉庫層 (Repository)**
- `AlarmRepository.insertOrUpdate()`：含去重邏輯（依 hour + minute + title + folderId + repeatDays 比對），防止 Deep Link 重複新增
- `FolderRepository`：含 `FeatureFlags` 付費檢查
- 使用 Coroutines + Flow 實現響應式資料流

**展示層 (Presentation)**
- `AlarmViewModel`, `FolderViewModel`: 使用 `StateFlow`，繼承 `AndroidViewModel`
- `TimerViewModel` / `StopwatchViewModel`: 計時器和碼錶功能

### 導航架構（重要，三層結構）

所有導航邏輯集中在 `AppNavigation.kt` 的 `NexAlarmMainContent()`，`MainActivity.kt` 只負責權限請求和 Deep Link 入口。

三層導航機制共同運作：
1. **`ModalNavigationDrawer`**（側邊抽屜）：Home、Settings、Account 及快速切換 Tab
2. **`HorizontalPager`**（底部 4 個主 Tab，路由 `"tabs"`）：
   - 頁 0：`AlarmScreen`（鬧鐘列表）
   - 頁 1：`FolderManageScreen`（資料夾管理）
   - 頁 2：`StopwatchScreen`（碼錶）
   - 頁 3：`TimerScreen`（計時器）
3. **`NavController`**（全頁 push）：
   - `"alarm_edit/{alarmId}?folderId={folderId}"`
   - `"folder_detail/{folderId}"`
   - `"home"`, `"settings"`, `"account"`（抽屜專用頁面）

**新增頁面時判斷原則**：主功能頁 → 加入 Pager；子頁面/設定頁 → 加入 NavController。

**FAB 行為**：在 Tab 0（鬧鐘）導航到 `alarm_edit/-1`；在 Tab 1（資料夾）開啟新增資料夾對話框；Tab 2/3 無 FAB。

### 鬧鐘系統架構

**核心排程流程**
1. ViewModel → `AlarmRepository` → `AlarmScheduler.schedule(alarm)`
2. `AlarmScheduler` 優先使用 `AlarmManager.setAlarmClock()`（繞過 Doze，狀態列顯示鬧鐘圖示）
3. 無精確排程權限時（Android 12 API 31-32）fallback 到 `setAndAllowWhileIdle()`，並於 `MainActivity.checkFirstLaunchPermissions()` 引導使用者開啟設定

**觸發流程**：`AlarmReceiver` → 啟動 `AlarmService`（前台服務）→ 呼叫 `AlarmRingingActivity`（全螢幕 UI）

**重複鬧鐘 day-of-week 轉換**：內部格式 1=週一...7=週日；`Calendar` 格式 1=週日。轉換邏輯在 `AlarmScheduler.calculateNextTriggerTime()`。

**`BootReceiver`**：裝置重開機後重新排程所有 `isEnabled=true` 的鬧鐘。

### 付費功能與 Billing

- `FeatureFlags.isPremium`：全域付費狀態，初始化時從 `SettingsManager` 恢復
- `BillingManager`：Google Play Billing 一次性購買，商品 ID `nexalarm_premium`，購買確認後透過 `SettingsManager.isPremium` 持久化
- 免費版資料夾上限：`FeatureFlags.FREE_FOLDER_LIMIT = 10`（不含系統資料夾）
- 在 Google Play Console 建立 `nexalarm_premium` 商品後，`BillingManager.launchPurchaseFlow(activity)` 即可啟動購買

### 國際化與主題

- **所有使用者可見文字**放在 `ui/theme/Strings.kt` 的 `object S`，動態字串用函數：`fun alarmCount(count: Int): String`
- **全域狀態**：`isDarkTheme`、`isAppEnglish`（定義在 `Strings.kt`，為 `var`，由 `SettingsManager` 在 `MainActivity.onCreate` 初始化）
- 語言/主題變更後需重組整個 `NexAlarmTheme` 才生效

## 重要檔案

### 核心應用程式
- `MainActivity.kt`: 權限請求（通知、精確鬧鐘、電池優化白名單）+ Deep Link 處理
- `AppNavigation.kt`: 完整導航結構（Drawer + Pager + NavController）+ `NexAlarmMainContent()`
- `NexAlarmApp.kt`: Application 初始化、通知頻道設定、`CrashHandler`

### 鬧鐘系統
- `util/AlarmScheduler.kt`: 核心排程（`setAlarmClock()` + 時區/重複日計算）
- `receiver/AlarmReceiver.kt`: 鬧鐘觸發，轉交給 `AlarmService`
- `receiver/BootReceiver.kt`: 開機後重新排程
- `service/AlarmService.kt`: 前台服務（wake lock、播放鈴聲）
- `ui/screens/AlarmRingingActivity.kt`: 全螢幕鬧鐘 UI（取消/稍後提醒）
- `service/MeetingModeTileService.kt`: 快捷磚片，切換今日鬧鐘為震動模式

### 資料庫
- `data/database/NexAlarmDatabase.kt`: Room 資料庫 v4，含所有遷移腳本
- `data/model/AlarmEntity.kt`: 核心鬧鐘資料模型（含 `snoozeEnabled` 欄位，v4 新增）
- `data/model/FolderEntity.kt`: 資料夾模型（含 `isSystem`、`emoji` 欄位）
- `data/database/AlarmDao.kt` / `FolderDao.kt`: DAO

### 設定與功能
- `data/SettingsManager.kt`: SharedPreferences 封裝（深色模式、語言、isPremium、權限引導標記）
- `util/FeatureFlags.kt`: 付費功能開關（`object`，運行期可改動）
- `util/BillingManager.kt`: Google Play Billing 整合
- `ui/theme/Strings.kt`: 國際化字串 + `isDarkTheme`/`isAppEnglish` 全域變數
- `util/NotificationHelper.kt`: 通知建立工具
- `util/AlarmTestHook.kt`: 測試專用掛鉤

### 測試
- `app/src/androidTest/java/com/nexalarm/app/test/AlarmReliabilityTest.kt`: 儀器測試，含 Doze/Process 回收等場景
- `app/src/test/java/com/nexalarm/app/FeatureFlagsTest.kt`: 本地單元測試
- `app/src/test/java/com/nexalarm/app/data/model/RepeatDaysConverterTest.kt`: TypeConverter 單元測試

## 開發注意事項

### 鬧鐘功能
- 務必在**實體裝置**上測試（模擬器無法完整模擬 Doze 模式）
- 新增/更新鬧鐘一律呼叫 `AlarmScheduler.schedule(alarm)`，不可直接操作 `AlarmManager`
- API 31+ 需先確認 `alarmManager.canScheduleExactAlarms()`，不可跳過（`AlarmScheduler` 已處理 fallback）

### DB Schema 變更
- 每次修改 `AlarmEntity` 或 `FolderEntity` **必須**遞增 `NexAlarmDatabase` 的 `version` 並新增對應的 `MIGRATION_X_Y`
- 跳過此步驟會導致 `IllegalStateException: Room cannot verify the data integrity`

### 程式碼規範
- 複雜業務邏輯使用中文註解（符合現有程式碼庫風格）
- UI 一律使用 `@Composable` 函數，不使用 XML 佈局
- 所有 DB 操作使用 `suspend`，UI 層透過 `Flow`/`StateFlow` 觀察

## 權限需求

- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: 精確鬧鐘（API 31+ 需使用者手動開啟）
- `POST_NOTIFICATIONS`: 鬧鐘通知（API 33+ 需執行期請求）
- `USE_FULL_SCREEN_INTENT`: 鎖定螢幕顯示
- `WAKE_LOCK`: 鬧鐘響鈴期間保持裝置喚醒
- `RECEIVE_BOOT_COMPLETED`: 重新開機後重新排程
- `VIBRATE`: 震動
- `FOREGROUND_SERVICE`: 前台鬧鐘服務
