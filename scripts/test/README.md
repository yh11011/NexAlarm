# NexAlarm 測試矩陣 v2

## 概述

基於 8+2 維度的完整測試矩陣，涵蓋 52 個測試案例，分為三個層級。

## 維度定義

| 維度 | 名稱 | 值 |
|------|------|-----|
| D1 | Android API | 26-30, 31-32, 33+ |
| D2 | App 狀態 | 前景, 背景, 被滑掉, Process death |
| D3 | 裝置狀態 | 解鎖, 鎖屏, 充電, 待機, 重開機後 |
| D4 | 系統限制 | 無, Doze, 電池優化未白名單, DND |
| D5 | 精確鬧鐘權限 | USE_EXACT, SCHEDULE_EXACT (granted/denied) |
| D6 | 通知權限 | N/A(<33), POST_NOTIFICATIONS (granted/denied) |
| D7 | 帳號狀態 | 未登入, 已登入, Token 過期 |
| D8 | 網路狀態 | 正常, 離線, 弱網, 伺服器 5xx |
| D9 | 安裝來源 | fresh install, upgrade, backup-restore |
| D10 | 時間條件 | 今日未到, 今日已過, 跨日, 跨週, 跨月, 跨年, DST |

## 測試層級

| 層級 | 案例數 | 預估時間 | 自動化程度 |
|------|--------|----------|-----------|
| Layer 1 (Smoke) | 12 + 6 補充 = 18 | ~30 min | 90% ADB script |
| Layer 2 (Regression) | 20 | ~45 min | 70% ADB + 30% Instrumentation |
| Layer 3 (Reliability) | 14 | ~3-4 hrs | 50% Instrumentation + 50% Manual |
| **總計** | **52** | **~4-5 hrs** | **~70%** |

## 快速開始

### 1. 執行 Layer 1 Smoke 測試 (ADB 腳本)

```bash
# 基本用法
bash scripts/test/run_layer1_smoke.sh

# 指定裝置
bash scripts/test/run_layer1_smoke.sh <device_serial>

# 查看報告
cat reports/smoke_<timestamp>/results.csv
```

### 2. 執行 Instrumentation 測試

```bash
# Layer 2 Regression
./gradlew connectedAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.nexalarm.app.test.Layer2RegressionTest

# Layer 3 Reliability (原有)
./gradlew connectedAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.nexalarm.app.test.AlarmReliabilityTest
```

### 3. 使用批次管理器

```bash
# 執行全部層級
bash scripts/test/run_test_batch.sh --layer all

# 僅執行 Layer 1
bash scripts/test/run_test_batch.sh --layer 1

# 僅執行 Layer 2
bash scripts/test/run_test_batch.sh --layer 2

# 僅執行 Layer 3
bash scripts/test/run_test_batch.sh --layer 3

# 預覽執行計畫 (不實際執行)
bash scripts/test/run_test_batch.sh --layer all --dry-run
```

## 檔案結構

```
scripts/test/
├── run_layer1_smoke.sh      # Layer 1 ADB 自動化腳本
├── run_test_batch.sh        # 批次執行管理器
└── README.md                # 本文件

app/src/androidTest/java/com/nexalarm/app/test/
├── TestMatrix.kt            # 測試矩陣映射表 (52 案例定義)
├── AlarmReliabilityTest.kt  # Layer 3 可靠性測試 (原有)
├── Layer2RegressionTest.kt  # Layer 2 Regression 測試 (新增)
├── AlarmTestResult.kt       # 測試結果數據模型
├── TestReportGenerator.kt   # 報告生成器
└── ReliableAlarmMonitor.kt  # 鬧鐘監控器 (AlarmReliabilityTest 內部類)

app/src/main/java/com/nexalarm/app/util/
└── AlarmTestHook.kt         # 測試鉤子 (嵌入 App 本體)
```

## 驗證訊號

每個案例透過以下 4 種訊號驗證:

| 訊號 | 驗證方式 | 工具 |
|------|----------|------|
| DB 狀態 | Room DB 查詢 | `alarmDao.getAllAlarmsList()` |
| AlarmManager 狀態 | dumpsys alarm | `adb shell dumpsys alarm \| grep nexalarm` |
| Notification 狀態 | dumpsys notification | `adb shell dumpsys notification \| grep nexalarm` |
| UI/Activity 狀態 | logcat + UI Automator | `adb logcat`, `uiautomator dump` |

## 裝置配置建議

| 裝置 | API | 用途 |
|------|-----|------|
| HTC Desire 20 Pro | 29 | 舊版覆蓋、Doze 真實測試、L1/L2 主力 |
| 模擬器 Pixel 6 | 31 | SCHEDULE_EXACT_ALARM 權限流程、L1/L2 |
| 模擬器 Pixel 7 | 33+ | POST_NOTIFICATIONS、USE_EXACT_ALARM、N1 案例 |

## 測試矩陣映射

完整 52 案例定義見 `TestMatrix.kt`:

- `LAYER_1_SMOKE`: 12 組核心 Smoke 測試
- `SUPPLEMENTARY`: 6 組補充案例 (S11, S12, S13, N1, B1, T5)
- `LAYER_2_REGRESSION`: 20 組 Regression 測試
- `LAYER_3_RELIABILITY`: 14 組 Reliability 測試

每個案例包含:
- Case ID, 層級, 功能, 描述, 優先級
- 8+2 維度映射
- 方案狀態 (free/premium_play/premium_promo)
- 預期驗證訊號
- 自動化類型 (ADB_SCRIPT/INSTRUMENTATION/MANUAL)

## 成功標準

### Level 0 — 系統層
- Receiver onReceive() 被呼叫

### Level 1 — 應用層
- ForegroundService 啟動
- MediaPlayer.start() 或震動啟動

### Level 2 — 使用者層 (商業標準)
1. 延遲 ≤ 3 秒
2. 播放/震動持續 ≥ 5 秒
3. 無 ANR / crash
4. AudioManager STREAM_ALARM 音量 > 0
5. 有通知或全螢幕顯示

## 注意事項

- Doze 測試僅在真機上可靠，模擬器行為可能不準確
- Force-stop 屬於系統設計限制，不列入成功率統計
- `setAlarmClock()` 會 bypass Doze
- Android 12+ 缺少 exact-alarm access 時，應用會降級使用非精確的 `setAndAllowWhileIdle()`；此路徑不可承諾準時觸發
- Firebase Crashlytics 需要有效的 `google-services.json` 才能正常初始化
