package com.nexalarm.app.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * 鬧鐘測試鉤子（嵌入 App 本體）
 *
 * 在 AlarmReceiver / AlarmService / AlarmRingingActivity 中呼叫，
 * 用 SharedPreferences 記錄每個關鍵事件的時間戳。
 *
 * 不依賴 NotificationManager.activeNotifications（Android 13+ 不可靠），
 * 也不依賴 Broadcast（可能遺失）。
 *
 * 驗證等級：
 *   Level 0 - 系統層：onReceive 被呼叫
 *   Level 1 - 應用層：ForegroundService 啟動 + MediaPlayer.start()
 *   Level 2 - 使用者層：播放 ≥5s + 無 crash + 螢幕喚醒 + 音量 >0
 */
object AlarmTestHook {

    private const val TAG = "AlarmTestHook"
    private const val PREFS_NAME = "alarm_test_hook"

    // ===== Key 前綴 =====
    private const val KEY_RECEIVER_TIME = "receiver_time_"      // Level 0
    private const val KEY_SERVICE_START_TIME = "service_start_"  // Level 1
    private const val KEY_MEDIA_PLAY_TIME = "media_play_"        // Level 1
    private const val KEY_FULLSCREEN_TIME = "fullscreen_"        // Level 2
    private const val KEY_NOTIFICATION_TIME = "notification_"    // Level 1
    private const val KEY_STREAM_VOLUME = "stream_volume_"       // Level 2
    private const val KEY_CRASH_DETECTED = "crash_"              // Level 2
    private const val KEY_SERVICE_ALIVE_TIME = "service_alive_"  // Level 2
    private const val KEY_VIBRATION_START = "vibration_start_"   // Level 1

    // ===== 全域紀錄 =====
    private const val KEY_TRIGGER_LOG = "trigger_log"

    // ===== Broadcast action (備用通知測試端) =====
    const val ACTION_TEST_EVENT = "com.nexalarm.app.TEST_HOOK_EVENT"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ==================== 寫入方法（App 本體呼叫）====================

    /** Level 0: AlarmReceiver.onReceive() 被呼叫 */
    fun onReceiverTriggered(context: Context, alarmId: Long) {
        val now = System.currentTimeMillis()
        prefs(context).edit().apply {
            putLong("${KEY_RECEIVER_TIME}$alarmId", now)
            // 追加日誌
            val log = prefs(context).getString(KEY_TRIGGER_LOG, "") ?: ""
            putString(KEY_TRIGGER_LOG, log + "RCV|$alarmId|$now|${fmtTime(now)}\n")
            apply()
        }
        broadcastEvent(context, "receiver_triggered", alarmId, now)
        Log.d(TAG, "📡 onReceiverTriggered: alarm=$alarmId at ${fmtTime(now)}")
    }

    /** Level 1: ForegroundService 已啟動（startForeground 呼叫後） */
    fun onServiceStarted(context: Context, alarmId: Long) {
        val now = System.currentTimeMillis()
        prefs(context).edit().putLong("${KEY_SERVICE_START_TIME}$alarmId", now).apply()
        broadcastEvent(context, "service_started", alarmId, now)
        Log.d(TAG, "🔧 onServiceStarted: alarm=$alarmId")
    }

    /** Level 1: MediaPlayer.start() 成功呼叫 */
    fun onMediaPlayStarted(context: Context, alarmId: Long) {
        val now = System.currentTimeMillis()
        prefs(context).edit().putLong("${KEY_MEDIA_PLAY_TIME}$alarmId", now).apply()
        broadcastEvent(context, "media_started", alarmId, now)
        Log.d(TAG, "🔊 onMediaPlayStarted: alarm=$alarmId")
    }

    /** Level 1: 震動已啟動 */
    fun onVibrationStarted(context: Context, alarmId: Long) {
        val now = System.currentTimeMillis()
        prefs(context).edit().putLong("${KEY_VIBRATION_START}$alarmId", now).apply()
        Log.d(TAG, "📳 onVibrationStarted: alarm=$alarmId")
    }

    /** Level 1: 通知已顯示 */
    fun onNotificationShown(context: Context, alarmId: Long) {
        val now = System.currentTimeMillis()
        prefs(context).edit().putLong("${KEY_NOTIFICATION_TIME}$alarmId", now).apply()
        Log.d(TAG, "🔔 onNotificationShown: alarm=$alarmId")
    }

    /** Level 2: 全螢幕 Activity 已顯示 */
    fun onFullScreenShown(context: Context, alarmId: Long) {
        val now = System.currentTimeMillis()
        prefs(context).edit().putLong("${KEY_FULLSCREEN_TIME}$alarmId", now).apply()
        broadcastEvent(context, "fullscreen_shown", alarmId, now)
        Log.d(TAG, "📱 onFullScreenShown: alarm=$alarmId")
    }

    /** Level 2: 記錄當前 STREAM_ALARM 音量 */
    fun recordStreamVolume(context: Context, alarmId: Long) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        prefs(context).edit().putInt("${KEY_STREAM_VOLUME}$alarmId", volume).apply()
        Log.d(TAG, "🔊 recordStreamVolume: alarm=$alarmId volume=$volume/$maxVolume")
    }

    /** Level 2: Service 仍存活（定期呼叫，更新最後存活時間） */
    fun onServiceStillAlive(context: Context, alarmId: Long) {
        val now = System.currentTimeMillis()
        prefs(context).edit().putLong("${KEY_SERVICE_ALIVE_TIME}$alarmId", now).apply()
    }

    /** Level 2: 偵測到 crash */
    fun onCrashDetected(context: Context, alarmId: Long, error: String) {
        prefs(context).edit().putString("${KEY_CRASH_DETECTED}$alarmId", error).apply()
        Log.e(TAG, "💥 onCrashDetected: alarm=$alarmId error=$error")
    }

    // ==================== 讀取方法（測試端呼叫）====================

    fun getReceiverTime(context: Context, alarmId: Long): Long =
        prefs(context).getLong("${KEY_RECEIVER_TIME}$alarmId", 0L)

    fun getServiceStartTime(context: Context, alarmId: Long): Long =
        prefs(context).getLong("${KEY_SERVICE_START_TIME}$alarmId", 0L)

    fun getMediaPlayTime(context: Context, alarmId: Long): Long =
        prefs(context).getLong("${KEY_MEDIA_PLAY_TIME}$alarmId", 0L)

    fun getVibrationStartTime(context: Context, alarmId: Long): Long =
        prefs(context).getLong("${KEY_VIBRATION_START}$alarmId", 0L)

    fun getNotificationTime(context: Context, alarmId: Long): Long =
        prefs(context).getLong("${KEY_NOTIFICATION_TIME}$alarmId", 0L)

    fun getFullScreenTime(context: Context, alarmId: Long): Long =
        prefs(context).getLong("${KEY_FULLSCREEN_TIME}$alarmId", 0L)

    fun getStreamVolume(context: Context, alarmId: Long): Int =
        prefs(context).getInt("${KEY_STREAM_VOLUME}$alarmId", -1)

    fun getServiceAliveTime(context: Context, alarmId: Long): Long =
        prefs(context).getLong("${KEY_SERVICE_ALIVE_TIME}$alarmId", 0L)

    fun getCrashInfo(context: Context, alarmId: Long): String? =
        prefs(context).getString("${KEY_CRASH_DETECTED}$alarmId", null)

    fun getTriggerLog(context: Context): String =
        prefs(context).getString(KEY_TRIGGER_LOG, "") ?: ""

    /** 清除指定鬧鐘的所有測試數據 */
    fun clearForAlarm(context: Context, alarmId: Long) {
        prefs(context).edit().apply {
            remove("${KEY_RECEIVER_TIME}$alarmId")
            remove("${KEY_SERVICE_START_TIME}$alarmId")
            remove("${KEY_MEDIA_PLAY_TIME}$alarmId")
            remove("${KEY_VIBRATION_START}$alarmId")
            remove("${KEY_NOTIFICATION_TIME}$alarmId")
            remove("${KEY_FULLSCREEN_TIME}$alarmId")
            remove("${KEY_STREAM_VOLUME}$alarmId")
            remove("${KEY_SERVICE_ALIVE_TIME}$alarmId")
            remove("${KEY_CRASH_DETECTED}$alarmId")
            apply()
        }
    }

    /** 清除所有測試數據 */
    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // ==================== 內部工具 ====================

    private fun broadcastEvent(context: Context, event: String, alarmId: Long, time: Long) {
        try {
            context.sendBroadcast(Intent(ACTION_TEST_EVENT).apply {
                setPackage(context.packageName)
                putExtra("event", event)
                putExtra("alarm_id", alarmId)
                putExtra("time", time)
            })
        } catch (e: Exception) {
            Log.w(TAG, "broadcastEvent failed: ${e.message}")
        }
    }

    private fun fmtTime(millis: Long): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(millis))
}

