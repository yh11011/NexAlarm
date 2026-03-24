# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案說明

NexAlarm 是一個功能完整的現代 Android 鬧鐘應用程式，採用 Kotlin 和 Jetpack Compose 開發。**重要：使用者完全不懂 Android 開發，所有開發工作都由 AI 完成。**

**AI 開發模式**：
- 使用者僅提供需求描述，不具備 Android 技術知識
- AI 需要主動解釋技術決策和實作步驟
- 所有程式碼修改都需要詳細說明影響和目的
- 提供完整的測試和驗證步驟
- **重要：AI 必須使用中文回答所有問題，包括技術解釋和程式碼說明**

**核心特色**:
- 單次/重複鬧鐘管理，支援週期性排程
- 資料夾分類系統，含免費/付費版本限制
- 會議模式快捷磚片（震動模式切換）
- Deep Link 整合 (`nexalarm://` URI scheme)
- Material Design 3 設計語言
- 雙語支援（中文/英文）
- 深色/淺色主題切換

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

# 清理構建
./gradlew clean assembleDebug
```

### 測試執行
```bash
# 執行儀器測試（需連接實體裝置）
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
- `NexAlarmDatabase`: Room 資料庫，版本 3，包含 `AlarmEntity` 和 `FolderEntity` 表格
- 資料庫遷移支援（MIGRATION_1_2、MIGRATION_2_3），含系統資料夾自動初始化回呼
- 使用 TypeConverters 處理複雜資料型別 (`RepeatDaysConverter`)
- DAO 模式：`AlarmDao`, `FolderDao` 提供 suspend 函數和 Flow
- 支援資料夾表情符號（emoji 欄位）

**倉庫層 (Repository)**
- `AlarmRepository`: 管理鬧鐘 CRUD 操作，整合 `AlarmScheduler`
- `FolderRepository`: 處理資料夾管理，含付費功能限制 (`FeatureFlags`)
- 使用 Coroutines 和 Flow 實現響應式資料流

**展示層 (Presentation)**
- `AlarmViewModel`: 鬧鐘畫面狀態管理，處理鬧鐘生命週期
- `FolderViewModel`: 資料夾管理和付費功能閘門
- `TimerViewModel` / `StopwatchViewModel`: 計時器和碼錶功能
- 所有 ViewModel 繼承 `AndroidViewModel`，使用 `StateFlow` 管理狀態

### 導航架構（重要）

`MainActivity` 同時使用兩套導航機制：
1. **`HorizontalPager`**（底部 Tab）：鬧鐘、計時器、碼錶、帳號四個主頁面
2. **`NavController`**（頁面內導航）：`AlarmEditScreen`、`FolderManageScreen`、`FolderDetailScreen`、`SingleAlarmScreen`、`RepeatAlarmScreen` 等子頁面

新增頁面時需判斷應加入 Pager（主頁）還是 NavController（子頁面）。

### 鬧鐘系統架構

**核心排程系統**
- `AlarmScheduler`: 使用 **`AlarmManager.setAlarmClock()`** 排程鬧鐘。`setAlarmClock()` 可繞過 Doze 模式和電池優化，是商業鬧鐘 App 的標準做法
- `AlarmReceiver`: BroadcastReceiver 處理鬧鐘觸發
- `AlarmService`: 前台服務處理鬧鐘響鈴，含 wake lock
- `AlarmRingingActivity`: 全螢幕 UI，提供取消/稍後提醒操作（位於 `ui/screens/`）
- `BootReceiver`: 裝置重新開機後重新排程所有已啟用的鬧鐘

**UI 架構 (Compose)**
- 自訂元件：`AlarmCard`, `TimePickerSheet`, `NexToggle`, `CountdownText`
- 國際化支援透過 `Strings.kt` object `S`（中英文切換）
- 全域狀態管理：`isDarkTheme`, `isAppEnglish`（定義在 `Strings.kt`）

## 關鍵功能與模式

### 鬧鐘管理功能
- 24小時制時間選擇器，支援重複鬧鐘（週間選擇）
- 可配置稍後提醒（5-10分鐘延遲，最大次數）、音量控制、震動模式
- 單次鬧鐘響鈴後自動刪除（可配置）

### 資料夾系統
- 顏色編碼的鬧鐘分類，支援表情符號
- 系統資料夾（"單次鬧鐘"、"重複鬧鐘"）受保護，禁止刪除
- 付費功能：無限資料夾（免費版限制 10 個），透過 `FeatureFlags.isPremium` 控制

### Deep Link 整合
- URI scheme: `nexalarm://`，支援操作: `add`, `delete`, `toggle_folder`
- 基於時間 + 標題 + 資料夾 + 重複模式的去重機制，防止重複新增

### 國際化
- 所有使用者可見文字放在 `ui/theme/Strings.kt` 的 object `S`
- 使用 `isAppEnglish` 全域變數切換語言
- 動態字串使用函數: `fun alarmCount(count: Int): String`

## 重要檔案

### 核心應用程式
- `MainActivity.kt`: 主導航（HorizontalPager + NavController）和 Deep Link 處理
- `NexAlarmApp.kt`: 應用程式初始化和通知頻道設定

### 鬧鐘系統
- `util/AlarmScheduler.kt`: 核心排程邏輯，使用 `setAlarmClock()` + 時區處理
- `receiver/AlarmReceiver.kt`: 鬧鐘觸發處理
- `receiver/BootReceiver.kt`: 開機後重新排程已啟用鬧鐘
- `ui/screens/AlarmRingingActivity.kt`: 全螢幕鬧鐘介面
- `service/AlarmService.kt`: 前台服務，處理鬧鐘響鈴
- `service/MeetingModeTileService.kt`: 快捷磚片，將今日鬧鐘切換為震動模式

### 畫面 (Screens)
- `HomeScreen.kt`: 首頁概覽
- `AlarmScreen.kt`: 鬧鐘列表主畫面
- `AlarmEditScreen.kt`: 新增/編輯鬧鐘
- `SingleAlarmScreen.kt`: 單次鬧鐘管理
- `RepeatAlarmScreen.kt`: 重複鬧鐘管理
- `FolderManageScreen.kt`: 資料夾管理列表
- `FolderDetailScreen.kt`: 資料夾詳細內容
- `SettingsScreen.kt`: 設定（主題/語言切換）
- `AccountScreen.kt`: 帳號/付費功能頁面

### 資料庫
- `data/database/NexAlarmDatabase.kt`: Room 資料庫，含遷移（MIGRATION_1_2、MIGRATION_2_3）
- `data/model/AlarmEntity.kt` / `FolderEntity.kt`: 核心資料模型
- `data/database/AlarmDao.kt` / `FolderDao.kt`: 資料存取物件
- `data/model/RepeatDaysConverter.kt`: TypeConverter 處理重複日期

### 設定與功能
- `data/SettingsManager.kt`: 使用者偏好設定持久化（SharedPreferences）
- `util/FeatureFlags.kt`: 付費功能開關
- `ui/theme/Strings.kt`: 國際化字串資源 + 全域 `isDarkTheme`/`isAppEnglish`
- `util/NotificationHelper.kt`: 通知建立工具
- `util/AlarmTestHook.kt`: 測試專用掛鉤工具

## 開發注意事項

### 鬧鐘功能開發
- 務必在實體裝置上測試鬧鐘功能（模擬器無法完整模擬 Doze 模式）
- 創建/更新鬧鐘時務必呼叫 `AlarmScheduler.schedule()`，不可直接操作 `AlarmManager`
- `setAlarmClock()` 會在狀態列顯示鬧鐘圖示，是使用者可見鬧鐘的正確 API
- API 31+ (Android 12) 需要先確認 `alarmManager.canScheduleExactAlarms()` 才能排程

### 程式碼規範
- 複雜業務邏輯使用中文註解（符合現有程式碼庫）
- 使用 `@Composable` 函數建構 UI，避免傳統 XML 佈局
- 所有資料庫操作使用 `suspend` 函數，UI 更新使用 `Flow` + `StateFlow`

### 測試
- 儀器測試位於 `app/src/androidTest/java/com/nexalarm/app/test/`
- `AlarmReliabilityTest.kt` 包含 14 個綜合測試場景（基本鬧鐘、Doze 模式、Process 回收等）
- 使用 JUnit 4 + UIAutomator 進行跨應用程式整合測試

## 權限需求

- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: 精確鬧鐘時間
- `POST_NOTIFICATIONS`: 鬧鐘通知
- `USE_FULL_SCREEN_INTENT`: 鎖定螢幕鬧鐘顯示
- `WAKE_LOCK`: 鬧鐘期間保持裝置喚醒
- `RECEIVE_BOOT_COMPLETED`: 重新開機後重新排程鬧鐘
- `VIBRATE`: 鬧鐘震動
- `FOREGROUND_SERVICE`: 鬧鐘響鈴服務
