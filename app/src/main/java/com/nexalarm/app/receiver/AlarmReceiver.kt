package com.nexalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nexalarm.app.data.AlarmSyncRepository
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.repository.AlarmRepository
import com.nexalarm.app.service.AlarmService
import com.nexalarm.app.ui.screens.AlarmRingingActivity
import com.nexalarm.app.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 鬧鐘觸發接收器
 * 當 AlarmManager 觸發時，此接收器會被呼叫
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.nexalarm.app.ALARM_TRIGGER"
        const val ACTION_SNOOZE = "com.nexalarm.app.ALARM_SNOOZE"
        const val ACTION_DISMISS = "com.nexalarm.app.ALARM_DISMISS"

        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_TITLE = "alarm_title"
        const val EXTRA_ALARM_VIBRATE_ONLY = "alarm_vibrate_only"
        const val EXTRA_ALARM_SNOOZE_ENABLED = "alarm_snooze_enabled"
        const val EXTRA_ALARM_VOLUME = "alarm_volume"

        private const val SNOOZE_PREFS = "nexalarm_snooze_counts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Received intent: ${intent.action}")

        when (intent.action) {
            ACTION_ALARM_TRIGGER -> handleAlarmTrigger(context, intent)
            ACTION_SNOOZE -> handleSnooze(context, intent)
            ACTION_DISMISS -> handleDismiss(context, intent)
        }
    }

    /**
     * 處理鬧鐘觸發
     */
    private fun handleAlarmTrigger(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        val title = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: ""
        val vibrateOnly = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE_ONLY, false)
        val snoozeEnabled = intent.getBooleanExtra(EXTRA_ALARM_SNOOZE_ENABLED, true)

        Log.d("AlarmReceiver", "Alarm triggered: ID=$alarmId, Title=$title")

        val volume = intent.getIntExtra(EXTRA_ALARM_VOLUME, 80)
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, title)
            putExtra(EXTRA_ALARM_VIBRATE_ONLY, vibrateOnly)
            putExtra(EXTRA_ALARM_SNOOZE_ENABLED, snoozeEnabled)
            putExtra(EXTRA_ALARM_VOLUME, volume)
        }

        context.startForegroundService(serviceIntent)

        if (isScreenLocked(context) || isLauncherApp(context)) {
            startFullScreenActivity(context, alarmId, title)
        }
    }

    /**
     * 啟動全螢幕鬧鐘 Activity
     */
    private fun startFullScreenActivity(context: Context, alarmId: Long, title: String) {
        val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, title)
        }
        context.startActivity(activityIntent)
    }

    /**
     * 處理貪睡：檢查貪睡次數，超過上限則自動關閉
     */
    private fun handleSnooze(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        Log.d("AlarmReceiver", "Snooze alarm: $alarmId")

        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        context.startService(stopIntent)

        val db = NexAlarmDatabase.getDatabase(context)
        val repo = AlarmRepository(db.alarmDao())
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = repo.getAlarmById(alarmId)
                if (alarm != null) {
                    val newCount = incrementSnoozeCount(context, alarmId)
                    if (alarm.maxSnoozeCount > 0 && newCount > alarm.maxSnoozeCount) {
                        // 超過貪睡次數上限：自動關閉鬧鐘
                        Log.d("AlarmReceiver", "Snooze limit reached for alarm $alarmId, auto-dismissing")
                        clearSnoozeCount(context, alarmId)
                        handlePostDismiss(context, alarm, repo)
                    } else {
                        AlarmScheduler(context).scheduleSnooze(alarm, alarm.snoozeDelay)
                        Log.d("AlarmReceiver", "Snoozed alarm $alarmId ($newCount/${alarm.maxSnoozeCount}) for ${alarm.snoozeDelay} min")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * 處理關閉鬧鐘
     */
    private fun handleDismiss(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        Log.d("AlarmReceiver", "Dismiss alarm: $alarmId")

        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        context.startService(stopIntent)

        clearSnoozeCount(context, alarmId)

        val db = NexAlarmDatabase.getDatabase(context)
        val repo = AlarmRepository(db.alarmDao())
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = repo.getAlarmById(alarmId)
                if (alarm != null) {
                    handlePostDismiss(context, alarm, repo)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * 關閉後處理：刪除單次鬧鐘 / 停用 / 排程下一次，並同步雲端
     */
    private suspend fun handlePostDismiss(context: Context, alarm: AlarmEntity, repo: AlarmRepository) {
        val db = NexAlarmDatabase.getDatabase(context)
        val token = SettingsManager(context).authToken

        if (!alarm.isRecurring && !alarm.keepAfterRinging) {
            repo.deleteById(alarm.id)
            Log.d("AlarmReceiver", "Deleted one-time alarm ${alarm.id}")
            // 同步軟刪除到雲端
            if (token != null) {
                val deletedAt = System.currentTimeMillis()
                AlarmSyncRepository.sync(
                    token = token,
                    localAlarms = db.alarmDao().getAllAlarmsList(),
                    deletedClientIds = listOf(alarm.clientId to deletedAt)
                )
            }
        } else if (!alarm.isRecurring) {
            repo.setEnabled(alarm.id, false)
            Log.d("AlarmReceiver", "Disabled one-time alarm ${alarm.id} (keepAfterRinging)")
            // 同步停用狀態到雲端
            if (token != null) {
                AlarmSyncRepository.sync(token, db.alarmDao().getAllAlarmsList())
            }
        } else {
            AlarmScheduler(context).schedule(alarm)
            Log.d("AlarmReceiver", "Rescheduled recurring alarm ${alarm.id}")
        }
    }

    // ── Snooze count helpers ──────────────────────────────────────────────────

    private fun incrementSnoozeCount(context: Context, alarmId: Long): Int {
        val prefs = context.getSharedPreferences(SNOOZE_PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt("alarm_$alarmId", 0) + 1
        prefs.edit().putInt("alarm_$alarmId", count).apply()
        return count
    }

    private fun clearSnoozeCount(context: Context, alarmId: Long) {
        context.getSharedPreferences(SNOOZE_PREFS, Context.MODE_PRIVATE)
            .edit().remove("alarm_$alarmId").apply()
    }

    // ── Screen / launcher detection ──────────────────────────────────────────

    private fun isScreenLocked(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE)
            as android.app.KeyguardManager
        return keyguardManager.isKeyguardLocked
    }

    private fun isLauncherApp(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager

            val runningTasks = activityManager.appTasks
            if (runningTasks.isEmpty()) return true

            val topActivity = runningTasks[0].taskInfo.topActivity ?: return true
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

            val resolveInfo = context.packageManager.resolveActivity(
                launcherIntent,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )

            topActivity.packageName == resolveInfo?.activityInfo?.packageName
        } catch (e: Exception) {
            Log.w("AlarmReceiver", "Failed to check launcher: ${e.message}")
            true
        }
    }
}
