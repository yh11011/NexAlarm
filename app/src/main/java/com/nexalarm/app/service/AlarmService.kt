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
import com.nexalarm.app.ui.theme.S
import com.nexalarm.app.util.AppSettingsProvider
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
    private var alarmTitle: String = ""
    private var vibrateOnly: Boolean = false
    private var snoozeEnabled: Boolean = true

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        // 同步設定，確保背景服務中的狀態與 SharedPreferences 一致
        AppSettingsProvider.syncFromSharedPreferences()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
                alarmTitle = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TITLE) ?: S.alarmDefaultTitle
                vibrateOnly = intent.getBooleanExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE_ONLY, false)
                snoozeEnabled = intent.getBooleanExtra(AlarmReceiver.EXTRA_ALARM_SNOOZE_ENABLED, true)

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
            if (isPlaying) stop()
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
        stopAlarm()
        super.onDestroy()
    }
}
