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
import com.nexalarm.app.receiver.AlarmReceiver
import com.nexalarm.app.ui.screens.AlarmRingingActivity
import com.nexalarm.app.util.NotificationHelper

/**
 * 鬧鐘服務
 * 負責播放鈴聲、震動，並顯示前台通知
 */
class AlarmService : Service() {

    companion object {
        const val ACTION_START_ALARM = "com.nexalarm.app.START_ALARM"
        const val ACTION_STOP_ALARM = "com.nexalarm.app.STOP_ALARM"

        private const val NOTIFICATION_ID = 1001
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private var alarmId: Long = -1
    private var alarmTitle: String = "鬧鐘"
    private var vibrateOnly: Boolean = false

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
                alarmTitle = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TITLE) ?: "鬧鐘"
                vibrateOnly = intent.getBooleanExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE_ONLY, false)

                startForeground(NOTIFICATION_ID, createNotification())
                startAlarm()
            }
            ACTION_STOP_ALARM -> {
                stopAlarm()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 開始播放鬧鐘
     */
    private fun startAlarm() {
        // 播放鈴聲（如果不是僅震動模式）
        if (!vibrateOnly) {
            startRingtone()
        }

        // 震動
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
        // 震動模式：[等待, 震動, 等待, 震動, ...]
        val pattern = longArrayOf(0, 1000, 1000, 1000, 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0) // 0 表示循環
            )
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
        // 停止鈴聲
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null

        // 停止震動
        vibrator?.cancel()

        android.util.Log.d("AlarmService", "Alarm stopped")
    }

    /**
     * 建立前台通知
     */
    private fun createNotification(): Notification {
        // 點擊通知開啟全螢幕 Activity
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarmTitle)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 延後按鈕
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this, 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 關閉按鈕
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
            .setContentText("鬧鐘響鈴中")
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // 無法滑動移除
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true) // 全螢幕通知
            .addAction(0, "延後", snoozePendingIntent)
            .addAction(0, "關閉", dismissPendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
