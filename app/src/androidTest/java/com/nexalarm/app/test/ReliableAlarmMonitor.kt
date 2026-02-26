package com.nexalarm.app.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.nexalarm.app.util.AlarmTestHook
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 可靠的鬧鐘觸發監聽器
 *
 * 使用雙重驗證：
 *   1. Broadcast 即時通知（速度快但可能遺失）
 *   2. SharedPreferences 持久化讀取（慢但可靠）
 *
 * 測試端呼叫 startMonitoring() → waitForResult() → stopMonitoring()
 */
class ReliableAlarmMonitor(private val context: Context) {

    private val TAG = "ReliableAlarmMonitor"
    private var triggerLatch: CountDownLatch? = null
    private var broadcastReceivedTime: Long = 0L
    private var targetAlarmId: Long = -1L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val event = intent?.getStringExtra("event") ?: return
            val id = intent.getLongExtra("alarm_id", -1L)
            Log.d(TAG, "收到事件: event=$event, alarm_id=$id, target=$targetAlarmId")

            if (id == targetAlarmId && event == "receiver_triggered") {
                broadcastReceivedTime = intent.getLongExtra("time", System.currentTimeMillis())
                triggerLatch?.countDown()
            }
        }
    }

    /**
     * 開始監聽指定鬧鐘
     */
    fun startMonitoring(alarmId: Long) {
        targetAlarmId = alarmId
        broadcastReceivedTime = 0L
        triggerLatch = CountDownLatch(1)

        // 清除該鬧鐘的舊數據
        AlarmTestHook.clearForAlarm(context, alarmId)

        val filter = IntentFilter(AlarmTestHook.ACTION_TEST_EVENT)
        // minSdk 26, RECEIVER_EXPORTED 常數可用
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        Log.d(TAG, "開始監聽 alarm_id=$alarmId")
    }

    /**
     * 等待鬧鐘觸發並收集完整的 Level 2 數據
     *
     * @param scheduledTime 排定時間
     * @param timeoutSeconds 最長等待秒數
     * @param postTriggerWaitMs 觸發後額外等待時間（等 Service 存活 + 音量驗證）
     */
    fun waitForResult(
        scheduledTime: Long,
        timeoutSeconds: Long = 90L,
        postTriggerWaitMs: Long = 8000L
    ): CollectedData {
        // 1. 等待 Level 0 觸發
        val latchResult = triggerLatch?.await(timeoutSeconds, TimeUnit.SECONDS) ?: false

        if (latchResult) {
            // 2. 觸發後，等待額外時間讓 Service 存活 ≥5 秒
            Log.d(TAG, "觸發成功，等待 ${postTriggerWaitMs}ms 收集 Level 2 數據...")
            Thread.sleep(postTriggerWaitMs)
        } else {
            // Broadcast 沒收到，嘗試 SharedPreferences 檢查
            Log.w(TAG, "Broadcast 未收到，檢查 SharedPreferences...")
            Thread.sleep(3000)
        }

        // 3. 從 SharedPreferences 讀取所有數據
        return collectData(scheduledTime)
    }

    /**
     * 從 SharedPreferences 收集完整數據
     */
    private fun collectData(scheduledTime: Long): CollectedData {
        val id = targetAlarmId
        return CollectedData(
            alarmId = id,
            scheduledTime = scheduledTime,
            receiverTime = AlarmTestHook.getReceiverTime(context, id),
            serviceStartTime = AlarmTestHook.getServiceStartTime(context, id),
            mediaPlayTime = AlarmTestHook.getMediaPlayTime(context, id),
            vibrationStartTime = AlarmTestHook.getVibrationStartTime(context, id),
            notificationTime = AlarmTestHook.getNotificationTime(context, id),
            fullScreenTime = AlarmTestHook.getFullScreenTime(context, id),
            streamVolume = AlarmTestHook.getStreamVolume(context, id),
            serviceLastAliveTime = AlarmTestHook.getServiceAliveTime(context, id),
            crashInfo = AlarmTestHook.getCrashInfo(context, id)
        )
    }

    fun stopMonitoring() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
    }

    /**
     * 收集到的原始數據
     */
    data class CollectedData(
        val alarmId: Long,
        val scheduledTime: Long,
        val receiverTime: Long,
        val serviceStartTime: Long,
        val mediaPlayTime: Long,
        val vibrationStartTime: Long,
        val notificationTime: Long,
        val fullScreenTime: Long,
        val streamVolume: Int,
        val serviceLastAliveTime: Long,
        val crashInfo: String?
    ) {
        /** Level 0: Receiver 是否被呼叫 */
        val level0: Boolean get() = receiverTime > 0

        /** 延遲毫秒 (Receiver 觸發時間 - 排定時間) */
        val delayMs: Long
            get() = if (receiverTime > 0) receiverTime - scheduledTime else -1L

        /** Level 1: Service 啟動 + 鈴聲或震動啟動 */
        val level1: Boolean
            get() = level0 && serviceStartTime > 0 && (mediaPlayTime > 0 || vibrationStartTime > 0)

        /** Service 存活秒數 */
        val serviceAliveSec: Double
            get() = if (serviceStartTime > 0 && serviceLastAliveTime > 0)
                (serviceLastAliveTime - serviceStartTime) / 1000.0 else 0.0

        /**
         * Level 2: 使用者層成功
         *   1. 延遲 ≤ 3 秒
         *   2. Service 存活 ≥ 5 秒
         *   3. 無 crash
         *   4. 音量 > 0（或震動模式下有震動）
         *   5. 有通知或全螢幕
         */
        val level2: Boolean
            get() {
                if (!level1) return false
                if (delayMs > 3000) return false
                if (serviceAliveSec < 5.0) return false
                if (crashInfo != null) return false
                // 音量檢查：如果有鈴聲播放，音量必須 > 0；如果是震動模式，只需有震動
                if (mediaPlayTime > 0 && streamVolume <= 0) return false
                if (notificationTime <= 0 && fullScreenTime <= 0) return false
                return true
            }

        /** 失敗類型分類 */
        val failureType: FailureType
            get() = when {
                level2 -> FailureType.NONE
                !level0 -> FailureType.F1_NO_TRIGGER
                crashInfo != null || serviceAliveSec < 5.0 -> FailureType.F4_CRASH
                delayMs > 10000 -> FailureType.F2_EXCESSIVE_DELAY
                delayMs > 3000 -> FailureType.F5_SYSTEM_DEFERRED
                mediaPlayTime <= 0 && vibrationStartTime <= 0 -> FailureType.F3_NO_SOUND
                mediaPlayTime > 0 && streamVolume <= 0 -> FailureType.F3_NO_SOUND
                else -> FailureType.F3_NO_SOUND
            }
    }
}

