# NexAlarm 鬧鐘可靠性自動化測試指南

## 概述

本專案包含 **14 個自動化測試場景**，驗證鬧鐘在各種極端條件下是否能正確觸發並響鈴。

### 測試場景列表

| # | 場景 | 測試目的 | 預估時間 |
|---|------|----------|----------|
| 1 | 螢幕開啟_基本鬧鐘 | 最基礎的正確性測試 | ~3 分鐘 |
| 2 | 螢幕關閉 | 螢幕休眠時是否喚醒 | ~3 分鐘 |
| 3 | 裝置鎖定 | 鎖定畫面時是否顯示全螢幕 | ~3 分鐘 |
| 4 | Synthetic Doze | 省電休眠模式 (僅真機) | ~6 分鐘 |
| 5 | 短間隔連續鬧鐘 | PendingIntent 覆蓋問題 | ~5 分鐘 |
| 6 | Process 被回收 | am kill 後 AlarmManager 是否存活 | ~8 分鐘 |
| 7 | kill-9 非正常終止 | 暴力殺程序後是否恢復 | ~8 分鐘 |
| 8 | AlarmQueue 驗證 | dumpsys 確認排程正確 | ~5 分鐘 |
| 9 | 勿擾模式 (DND) | 完全勿擾下鬧鐘是否穿透 | ~3 分鐘 |
| 10 | 省電模式 | Battery Saver 開啟時是否延遲 | ~3 分鐘 |
| 11 | 僅震動模式 | vibrateOnly 是否正確只震動 | ~3 分鐘 |
| 12 | 同時多鬧鐘 | 同秒觸發多個鬧鐘 | ~2 分鐘 |
| 13 | 長時間待機 | 3 分鐘睡眠後喚醒 | ~20 分鐘 |
| 14 | 靜音模式 | Ringer=SILENT 但 STREAM_ALARM 獨立 | ~3 分鐘 |

### 三層驗證標準

- **Level 0**: BroadcastReceiver 被呼叫
- **Level 1**: ForegroundService 啟動 + 鈴聲/震動啟動
- **Level 2**: 延遲 ≤ 3s + Service 存活 ≥ 5s + 無 crash + 音量 > 0 + 有通知/全螢幕

### 失敗分類

- **F1** — 完全沒觸發 (Receiver 未被呼叫)
- **F2** — 延遲 > 10 秒
- **F3** — 有觸發但無聲音
- **F4** — 觸發後立刻 crash
- **F5** — 被系統延後 (3-10 秒)

---

## 執行方式

### 前置條件

1. 連接 Android 裝置 (USB 偵錯已開啟) 或啟動模擬器
2. 確認 `adb devices` 可見裝置
3. APP 已安裝或可由 Gradle 自動安裝

### 執行全部測試

```bash
cd C:\Users\user\Desktop\NexAlarm
.\gradlew connectedAndroidTest --info
```

### 執行單一場景

```bash
# 只跑勿擾模式測試
.\gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nexalarm.app.test.AlarmReliabilityTest#test09_DoNotDisturbMode

# 只跑螢幕開啟基本測試
.\gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nexalarm.app.test.AlarmReliabilityTest#test01_ScreenOn_BasicAlarm
```

### 用 adb 直接執行 (更快)

```bash
# 先安裝 APK
.\gradlew installDebug installDebugAndroidTest

# 執行全部
adb shell am instrument -w -r \
  -e class com.nexalarm.app.test.AlarmReliabilityTest \
  com.nexalarm.app.test/androidx.test.runner.AndroidJUnitRunner

# 執行單一場景
adb shell am instrument -w -r \
  -e class com.nexalarm.app.test.AlarmReliabilityTest#test09_DoNotDisturbMode \
  com.nexalarm.app.test/androidx.test.runner.AndroidJUnitRunner
```

---

## 查看測試報告

測試完成後，報告會輸出到裝置的外部儲存：

```bash
# 列出報告檔案
adb shell ls /sdcard/Android/data/com.nexalarm.app/files/

# 拉取 CSV 原始數據
adb pull /sdcard/Android/data/com.nexalarm.app/files/alarm_test_results.csv .

# 拉取摘要報告
adb pull /sdcard/Android/data/com.nexalarm.app/files/alarm_test_report.txt .
```

報告內容包含：
- 每個場景的 Level 0/1/2 成功率
- 延遲統計: P50 / P90 / P95 / P99
- 失敗類型分佈
- 裝置資訊

---

## 注意事項

- 場景 4 (Doze) 在**模擬器上會自動跳過**，需要真機
- 場景 13 (長時間待機) 需要約 20 分鐘，可單獨執行
- 測試過程中裝置會自動開關螢幕、切換靜音等，請勿手動操作
- 所有場景的 cleanup 會自動恢復裝置狀態
