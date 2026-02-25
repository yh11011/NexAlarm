package com.nexalarm.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.receiver.AlarmReceiver
import java.util.*

/**
 * 鬧鐘排程管理器
 * 負責使用 AlarmManager 設定精確的鬧鐘觸發時間
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 排程鬧鐘
     * @param alarm 鬧鐘實體
     */
    fun schedule(alarm: AlarmEntity) {
        // 如果鬧鐘未啟用，取消排程
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        // 計算下次觸發時間
        val triggerTime = calculateNextTriggerTime(alarm)

        // 建立 PendingIntent
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
            putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE_ONLY, alarm.vibrateOnly)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 檢查權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // 引導使用者開啟權限
                val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settingsIntent)
                return
            }
        }

        // 設定精確鬧鐘（即使在 Doze 模式也能觸發）
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )

        android.util.Log.d("AlarmScheduler",
            "Scheduled alarm ${alarm.id} at ${Date(triggerTime)}")
    }

    /**
     * 取消鬧鐘排程
     */
    fun cancel(alarm: AlarmEntity) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        android.util.Log.d("AlarmScheduler", "Cancelled alarm ${alarm.id}")
    }

    /**
     * 排程貪睡鬧鐘
     */
    fun scheduleSnooze(alarm: AlarmEntity, snoozeMinutes: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val triggerTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
            putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE_ONLY, alarm.vibrateOnly)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )

        android.util.Log.d("AlarmScheduler",
            "Snoozed alarm ${alarm.id} for $snoozeMinutes min, fires at ${Date(triggerTime)}")
    }

    /**
     * 計算下次觸發時間
     */
    private fun calculateNextTriggerTime(alarm: AlarmEntity): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()

        // 如果是重複鬧鐘
        if (alarm.isRecurring && alarm.repeatDays.isNotEmpty()) {
            // 找出下一個應該觸發的日期
            val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val sortedDays = alarm.repeatDays.sorted()

            // 轉換：我們的格式 1=週一，Calendar 格式 1=週日
            val targetDays = sortedDays.map {
                when (it) {
                    7 -> Calendar.SUNDAY
                    else -> it + 1
                }
            }

            // 尋找下一個觸發日
            var found = false

            for (i in 0..7) {
                val checkDay = (currentDayOfWeek + i - 1) % 7 + 1
                if (targetDays.contains(checkDay)) {
                    val tempCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, alarm.hour)
                        set(Calendar.MINUTE, alarm.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_MONTH, i)
                    }

                    // 如果是今天，檢查時間是否已過
                    if (i == 0 && tempCal.timeInMillis <= now) {
                        continue
                    }

                    calendar.timeInMillis = tempCal.timeInMillis
                    found = true
                    break
                }
            }

            if (!found) {
                // 找第一個重複日
                val firstDay = targetDays.first()
                val daysToAdd = (firstDay - currentDayOfWeek + 7) % 7
                val adjustedDays = if (daysToAdd == 0) 7 else daysToAdd
                calendar.add(Calendar.DAY_OF_MONTH, adjustedDays)
            }
        } else {
            // 單次鬧鐘
            // 如果時間已過，設定為明天
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return calendar.timeInMillis
    }

    /**
     * 計算距離下次觸發的時間文字
     * @return 例如 "6小時30分鐘後"
     */
    fun getTimeUntilText(alarm: AlarmEntity): String {
        if (!alarm.isEnabled) return ""

        val triggerTime = calculateNextTriggerTime(alarm)
        val now = System.currentTimeMillis()
        val diff = triggerTime - now

        if (diff <= 0) return "即將響鈴"

        val hours = (diff / (1000 * 60 * 60)).toInt()
        val minutes = ((diff % (1000 * 60 * 60)) / (1000 * 60)).toInt()

        return when {
            hours > 0 && minutes > 0 -> "${hours}小時${minutes}分鐘後"
            hours > 0 -> "${hours}小時後"
            minutes > 0 -> "${minutes}分鐘後"
            else -> "不到1分鐘"
        }
    }

    /**
     * 取得下次觸發的時間戳記
     */
    fun getNextTriggerTime(alarm: AlarmEntity): Long {
        return if (alarm.isEnabled) {
            calculateNextTriggerTime(alarm)
        } else {
            0L
        }
    }

    companion object {
        /**
         * 快捷方法：建立實例並排程
         */
        fun schedule(context: Context, alarm: AlarmEntity) {
            AlarmScheduler(context).schedule(alarm)
        }

        /**
         * 快捷方法：建立實例並取消
         */
        fun cancel(context: Context, alarm: AlarmEntity) {
            AlarmScheduler(context).cancel(alarm)
        }

        /**
         * 快捷方法：建立實例並排程貪睡
         */
        fun scheduleSnooze(context: Context, alarm: AlarmEntity, snoozeMinutes: Int) {
            AlarmScheduler(context).scheduleSnooze(alarm, snoozeMinutes)
        }
    }
}
