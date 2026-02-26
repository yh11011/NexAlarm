package com.nexalarm.app.test

/**
 * 鬧鐘測試結果數據模型
 *
 * 每一筆代表一次鬧鐘排程→觸發→驗證的完整結果。
 * 依照三層成功等級 (Level 0 / 1 / 2) 記錄。
 */
data class AlarmTestResult(
    // ===== 基本資訊 =====
    val testCase: String,
    val scenario: String,
    val iteration: Int,

    // ===== 時間 =====
    val scheduledTime: Long,          // 排定觸發時間
    val receiverTime: Long,           // Level 0: onReceive 時間
    val serviceStartTime: Long,       // Level 1: ForegroundService 啟動時間
    val mediaPlayTime: Long,          // Level 1: MediaPlayer.start() 時間
    val fullScreenTime: Long,         // Level 2: 全螢幕 Activity 時間
    val serviceLastAliveTime: Long,   // Level 2: Service 最後心跳時間

    // ===== 驗證指標 =====
    val delayMs: Long,                // |receiverTime - scheduledTime|
    val serviceAliveSeconds: Double,  // Service 存活秒數
    val streamVolume: Int,            // STREAM_ALARM 音量 (-1=未取得)
    val crashInfo: String?,           // crash 訊息 (null=無)

    // ===== 成功等級 =====
    val level0Success: Boolean,       // Receiver 被呼叫
    val level1Success: Boolean,       // Service + MediaPlayer 啟動
    val level2Success: Boolean,       // 使用者層成功（完整定義）

    // ===== 失敗分類 =====
    val failureType: FailureType,

    // ===== 環境 =====
    val deviceState: String,
    val isRealDevice: Boolean
) {
    /** 延遲等級評價 */
    val delayRating: String
        get() = when {
            delayMs < 0 -> "N/A"
            delayMs <= 1000 -> "完美"
            delayMs <= 3000 -> "可接受"
            delayMs <= 10000 -> "邊緣"
            else -> "不可接受"
        }

    fun toCsvRow(): String = listOf(
        testCase, scenario, iteration,
        scheduledTime, receiverTime, serviceStartTime, mediaPlayTime,
        fullScreenTime, serviceLastAliveTime,
        delayMs, "%.1f".format(serviceAliveSeconds), streamVolume,
        crashInfo ?: "",
        level0Success, level1Success, level2Success,
        failureType.name, delayRating,
        deviceState, isRealDevice
    ).joinToString(",")

    companion object {
        fun csvHeader(): String = listOf(
            "TestCase", "Scenario", "Iteration",
            "ScheduledTime", "ReceiverTime", "ServiceStartTime", "MediaPlayTime",
            "FullScreenTime", "ServiceLastAliveTime",
            "DelayMs", "ServiceAliveSec", "StreamVolume",
            "CrashInfo",
            "Level0", "Level1", "Level2",
            "FailureType", "DelayRating",
            "DeviceState", "IsRealDevice"
        ).joinToString(",")
    }
}

/**
 * 失敗類型分類
 */
enum class FailureType {
    /** 成功 */
    NONE,
    /** F1: 完全沒觸發（Receiver 未被呼叫） */
    F1_NO_TRIGGER,
    /** F2: 延遲 > 10 秒 */
    F2_EXCESSIVE_DELAY,
    /** F3: 有觸發但無聲音（MediaPlayer 未啟動或音量為 0） */
    F3_NO_SOUND,
    /** F4: 觸發後立刻 crash（Service 存活 < 5 秒） */
    F4_CRASH,
    /** F5: 被系統延後（3-10 秒，Doze batching） */
    F5_SYSTEM_DEFERRED
}

