package com.nexalarm.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nexalarm.app.AlarmRingingActivity
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.repository.AlarmRepository
import com.nexalarm.app.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "nexalarm_ringing"
        const val CHANNEL_NAME = "Alarm Ringing"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DISMISS = "com.nexalarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.nexalarm.ACTION_SNOOZE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        private const val PREFS_SNOOZE = "snooze_counts"

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm ringing notifications"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentAlarmId: Long = -1L
    private var currentAlarm: AlarmEntity? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                handleDismiss()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                handleSnooze()
                return START_NOT_STICKY
            }
        }

        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        currentAlarmId = alarmId
        createNotificationChannel(this)

        val db = NexAlarmDatabase.getDatabase(this)
        val repo = AlarmRepository(db.alarmDao())

        CoroutineScope(Dispatchers.IO).launch {
            val alarm = repo.getAlarmById(alarmId)
            if (alarm != null) {
                currentAlarm = alarm
                // Reschedule recurring alarms for next occurrence
                if (alarm.isRecurring) {
                    AlarmScheduler.schedule(this@AlarmService, alarm)
                }
                launch(Dispatchers.Main) {
                    startRinging(alarm)
                }
            } else {
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startRinging(alarm: AlarmEntity) {
        // Build full-screen intent
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this, alarm.id.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val dismissPi = PendingIntent.getService(
            this, (alarm.id.toInt() + 1000), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val snoozePi = PendingIntent.getService(
            this, (alarm.id.toInt() + 2000), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (alarm.title.isNotBlank()) alarm.title else "Alarm"
        val timeStr = String.format("%02d:%02d", alarm.hour, alarm.minute)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Alarm at $timeStr")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPi)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze", snoozePi)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Start vibration
        startVibration()

        // Play sound if not vibrate-only
        if (!alarm.vibrateOnly) {
            startSound(alarm)
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 800, 400, 800, 400, 800)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun startSound(alarm: AlarmEntity) {
        try {
            val ringtoneUri = if (alarm.ringtoneUri.isNotBlank()) {
                android.net.Uri.parse(alarm.ringtoneUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, ringtoneUri)
                isLooping = true
                val vol = alarm.volume / 100f
                setVolume(vol, vol)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleDismiss() {
        stopRinging()
        // Handle one-time alarm cleanup on dismiss
        val alarm = currentAlarm
        if (alarm != null) {
            val db = NexAlarmDatabase.getDatabase(this)
            val repo = AlarmRepository(db.alarmDao())
            CoroutineScope(Dispatchers.IO).launch {
                if (!alarm.isRecurring && !alarm.keepAfterRinging) {
                    repo.deleteById(alarm.id)
                } else if (!alarm.isRecurring) {
                    // Keep but disable
                    repo.setEnabled(alarm.id, false)
                }
            }
        }
        // Clear snooze count
        clearSnoozeCount(currentAlarmId)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleSnooze() {
        stopRinging()
        stopForeground(STOP_FOREGROUND_REMOVE)

        val alarm = currentAlarm
        if (alarm != null) {
            val count = getSnoozeCount(alarm.id)
            if (count >= alarm.maxSnoozeCount) {
                // Max snooze reached, auto-dismiss
                handleDismiss()
                return
            }
            incrementSnoozeCount(alarm.id)
            AlarmScheduler.scheduleSnooze(this, alarm, alarm.snoozeDelay)
        }

        stopSelf()
    }

    private fun getSnoozeCount(alarmId: Long): Int {
        val prefs = getSharedPreferences(PREFS_SNOOZE, MODE_PRIVATE)
        return prefs.getInt("snooze_$alarmId", 0)
    }

    private fun incrementSnoozeCount(alarmId: Long) {
        val prefs = getSharedPreferences(PREFS_SNOOZE, MODE_PRIVATE)
        prefs.edit().putInt("snooze_$alarmId", getSnoozeCount(alarmId) + 1).apply()
    }

    private fun clearSnoozeCount(alarmId: Long) {
        val prefs = getSharedPreferences(PREFS_SNOOZE, MODE_PRIVATE)
        prefs.edit().remove("snooze_$alarmId").apply()
    }

    private fun stopRinging() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }
}
