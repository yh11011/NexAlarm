package com.nexalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.repository.AlarmRepository
import com.nexalarm.app.service.AlarmService
import com.nexalarm.app.ui.screens.AlarmRingingActivity
import com.nexalarm.app.util.AlarmScheduler
import com.nexalarm.app.util.AlarmTestHook
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
        val title = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: "鬧鐘"
        val vibrateOnly = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE_ONLY, false)

        // ===== 測試 Hook: Level 0 =====
        AlarmTestHook.onReceiverTriggered(context, alarmId)

        Log.d("AlarmReceiver", "Alarm triggered: ID=$alarmId, Title=$title")

        // 啟動 AlarmService 播放鈴聲/震動
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, title)
            putExtra(EXTRA_ALARM_VIBRATE_ONLY, vibrateOnly)
        }

        context.startForegroundService(serviceIntent)

        // 判斷是否顯示全螢幕 Activity
        // 螢幕鎖定或在桌面時 → 全螢幕
        // 在使用其他 App 時 → 僅通知（AlarmService 已含 fullScreenIntent）
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
     * 處理貪睡
     */
    private fun handleSnooze(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        Log.d("AlarmReceiver", "Snooze alarm: $alarmId")

        // 停止目前的鈴聲/震動
        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        context.startService(stopIntent)

        // 從 DB 查出鬧鐘，排程貪睡
        val db = NexAlarmDatabase.getDatabase(context)
        val repo = AlarmRepository(db.alarmDao())
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = repo.getAlarmById(alarmId)
                if (alarm != null) {
                    val scheduler = AlarmScheduler(context)
                    scheduler.scheduleSnooze(alarm, alarm.snoozeDelay)
                    Log.d("AlarmReceiver", "Snoozed alarm $alarmId for ${alarm.snoozeDelay} min")
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

        // 停止目前的鈴聲/震動
        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        context.startService(stopIntent)

        // 處理單次 / 重複鬧鐘的後續
        val db = NexAlarmDatabase.getDatabase(context)
        val repo = AlarmRepository(db.alarmDao())
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = repo.getAlarmById(alarmId)
                if (alarm != null) {
                    if (!alarm.isRecurring && !alarm.keepAfterRinging) {
                        // 單次鬧鐘：自動刪除
                        repo.deleteById(alarm.id)
                        Log.d("AlarmReceiver", "Deleted one-time alarm $alarmId")
                    } else if (!alarm.isRecurring) {
                        // 單次但 keepAfterRinging=true：停用
                        repo.setEnabled(alarm.id, false)
                        Log.d("AlarmReceiver", "Disabled one-time alarm $alarmId (keepAfterRinging)")
                    } else {
                        // 重複鬧鐘：排程下一次
                        val scheduler = AlarmScheduler(context)
                        scheduler.schedule(alarm)
                        Log.d("AlarmReceiver", "Rescheduled recurring alarm $alarmId")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * 檢查螢幕是否鎖定
     */
    private fun isScreenLocked(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE)
            as android.app.KeyguardManager
        return keyguardManager.isKeyguardLocked
    }

    /**
     * 檢查當前是否在桌面（Launcher）
     */
    private fun isLauncherApp(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager

            val runningTasks = activityManager.appTasks
            if (runningTasks.isEmpty()) return true // 無前台任務 → 當作桌面

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
            true // 出錯時預設顯示全螢幕
        }
    }
}
