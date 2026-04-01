package com.nexalarm.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.nexalarm.app.R
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.receiver.AlarmReceiver
import com.nexalarm.app.ui.screens.AlarmRingingActivity
import com.nexalarm.app.ui.theme.S
import com.nexalarm.app.util.AlarmScheduler
import com.nexalarm.app.util.AppSettingsProvider
import com.nexalarm.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 鬧鐘服務
 * 負責播放鈴聲、震動，並顯示前台通知
 */
class AlarmService : Service() {

    companion object {
        const val ACTION_START_ALARM = "com.nexalarm.app.START_ALARM"
        const val ACTION_STOP_ALARM = "com.nexalarm.app.STOP_ALARM"

        private const val NOTIFICATION_ID = 1001

        /**
         * 目前是否正在響鈴。
         * 用來防止同分鐘多個鬧鐘重複啟動音效——只有第一個鬧鐘觸發音效，
         * 後續在服務存活期間觸發的鬧鐘僅更新通知。
         */
        @Volatile
        private var isRinging = false
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var serviceScope: CoroutineScope? = null

    private var alarmId: Long = -1
    private var alarmTitle: String = ""
    private var vibrateOnly: Boolean = false
    private var snoozeEnabled: Boolean = true
    private var alarmVolume: Int = 80

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        serviceScope = CoroutineScope(Dispatchers.Main + Job())
        // 同步設定，確保背景服務中的狀態與 SharedPreferences 一致
        AppSettingsProvider.syncFromSharedPreferences()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val newAlarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
                val prevAlarmId = alarmId  // 記錄被取代的鬧鐘 id

                alarmTitle = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TITLE) ?: S.alarmDefaultTitle
                vibrateOnly = intent.getBooleanExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE_ONLY, false)
                snoozeEnabled = intent.getBooleanExtra(AlarmReceiver.EXTRA_ALARM_SNOOZE_ENABLED, true)
                alarmVolume = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, 80)

                // [NexAlarmTest] 事件 4/4：ForegroundService 已啟動，鈴聲即將播放
                android.util.Log.i("NexAlarmTest",
                    "SERVICE_START|id=$newAlarmId|title=$alarmTitle|ts=${System.currentTimeMillis()}")

                if (isRinging && prevAlarmId != -1L && prevAlarmId != newAlarmId) {
                    // 已有鬧鐘正在響鈴：不重啟音效（避免音效中斷或重疊），
                    // 僅更新通知以顯示最新鬧鐘名稱，並排程被取代的鬧鐘。
                    alarmId = newAlarmId
                    startForeground(NOTIFICATION_ID, createNotification())
                    android.util.Log.d("AlarmService",
                        "Alarm $newAlarmId arrived while ringing; coalescing. Rescheduling displaced alarm $prevAlarmId.")
                    rescheduleDisplacedAlarm(prevAlarmId)
                } else {
                    // 正常啟動：先停止舊音效（若有），再開始新音效
                    stopAlarm()
                    alarmId = newAlarmId
                    isRinging = true
                    startForeground(NOTIFICATION_ID, createNotification())
                    startAlarm()
                }
            }
            ACTION_STOP_ALARM -> {
                isRinging = false
                stopAlarm()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 排程被取代的鬧鐘：確保重複鬧鐘下次仍能觸發，單次鬧鐘則刪除或停用。
     * 邏輯與 AlarmReceiver.handlePostDismiss 一致。
     */
    private fun rescheduleDisplacedAlarm(displacedAlarmId: Long) {
        serviceScope?.launch {
            try {
                val db = NexAlarmDatabase.getDatabase(this@AlarmService)
                val alarm = withContext(Dispatchers.IO) { db.alarmDao().getAlarmById(displacedAlarmId) }
                    ?: return@launch
                if (alarm.isRecurring) {
                    // 重複鬧鐘：排程下次觸發
                    AlarmScheduler(this@AlarmService).schedule(alarm)
                    android.util.Log.d("AlarmService", "Rescheduled displaced recurring alarm $displacedAlarmId")
                } else if (!alarm.keepAfterRinging) {
                    // 單次鬧鐘（不保留）：從 DB 刪除
                    withContext(Dispatchers.IO) { db.alarmDao().deleteById(displacedAlarmId) }
                    android.util.Log.d("AlarmService", "Deleted displaced one-time alarm $displacedAlarmId")
                } else {
                    // 單次鬧鐘（保留）：停用
                    withContext(Dispatchers.IO) { db.alarmDao().setEnabled(displacedAlarmId, false) }
                    android.util.Log.d("AlarmService", "Disabled displaced one-time alarm $displacedAlarmId")
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmService", "Failed to reschedule displaced alarm $displacedAlarmId", e)
            }
        }
    }

    /**
     * 開始播放鬧鐘
     */
    private fun startAlarm() {
        if (!vibrateOnly) {
            startRingtone()
        }
        startVibration()
    }

    /**
     * 播放鈴聲
     */
    private fun startRingtone() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                // 套用使用者設定的音量 (0-100 → 0.0-1.0)
                val vol = (alarmVolume / 100f).coerceIn(0f, 1f)
                setVolume(vol, vol)
                start()
            }

            android.util.Log.d("AlarmService", "Ringtone started")
        } catch (e: Exception) {
            android.util.Log.e("AlarmService", "Failed to start ringtone", e)
        }
    }

    /**
     * 開始震動
     */
    private fun startVibration() {
        val pattern = longArrayOf(0, 1000, 1000, 1000, 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }

        android.util.Log.d("AlarmService", "Vibration started")
    }

    /**
     * 停止鬧鐘
     */
    private fun stopAlarm() {
        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
                reset()  // 確保進入 Idle 狀態，讓音訊資源完全釋放再 release
            } catch (e: Exception) {
                android.util.Log.w("AlarmService", "Error stopping media player", e)
            }
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        android.util.Log.d("AlarmService", "Alarm stopped")
    }

    /**
     * 建立前台通知
     */
    private fun createNotification(): Notification {
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarmTitle)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this, 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_ALARM)
            .setContentTitle(alarmTitle)
            .setContentText(S.alarmRinging)
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .apply { if (snoozeEnabled) addAction(0, S.snoozeAction, snoozePendingIntent) }
            .addAction(0, S.dismissAction, dismissPendingIntent)
            .build()
    }

    override fun onDestroy() {
        isRinging = false
        stopAlarm()
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }
}
