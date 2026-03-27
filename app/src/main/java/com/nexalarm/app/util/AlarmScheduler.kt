package com.nexalarm.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
            putExtra(AlarmReceiver.EXTRA_ALARM_SNOOZE_ENABLED, alarm.snoozeEnabled)
            putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, alarm.volume)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 設定鬧鐘
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // 無精確鬧鐘權限：使用 setAndAllowWhileIdle 作為 fallback（精度較低但鬧鐘不會消失）
            // 權限引導由 MainActivity 在首次啟動時處理，排程器不重複彈出
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            android.util.Log.w("AlarmScheduler",
                "No SCHEDULE_EXACT_ALARM permission, using inexact fallback for alarm ${alarm.id}")
            return
        }

        // 正常路徑：使用 setAlarmClock 可 bypass Doze，是商業鬧鐘的標準做法
        val showIntent = PendingIntent.getActivity(
            context, alarm.id.toInt(),
            Intent(context, com.nexalarm.app.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerTime, showIntent),
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
            // 無精確權限：fallback 到非精確鬧鐘
            val triggerFallback = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
            val intentFallback = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
                putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
                putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE_ONLY, alarm.vibrateOnly)
                putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, alarm.volume)
            }
            val piFallback = PendingIntent.getBroadcast(
                context, alarm.id.toInt(), intentFallback,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerFallback, piFallback)
            return
        }

        val triggerTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
            putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE_ONLY, alarm.vibrateOnly)
            putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, alarm.volume)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 貪睡也用 setAlarmClock，確保 bypass Doze
        val showIntent = PendingIntent.getActivity(
            context, alarm.id.toInt(),
            Intent(context, com.nexalarm.app.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerTime, showIntent),
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
    fun getTimeUntilText(alarm: AlarmEntity, isEnglish: Boolean = false): String {
        if (!alarm.isEnabled) return ""

        val triggerTime = calculateNextTriggerTime(alarm)
        val now = System.currentTimeMillis()
        val diff = triggerTime - now

        if (diff <= 0) return if (isEnglish) "Ringing now" else "即將響鈴"

        val hours = (diff / (1000 * 60 * 60)).toInt()
        val minutes = ((diff % (1000 * 60 * 60)) / (1000 * 60)).toInt()

        return if (isEnglish) {
            when {
                hours > 0 && minutes > 0 -> "in ${hours}h ${minutes}m"
                hours > 0 -> "in ${hours}h"
                minutes > 0 -> "in ${minutes}m"
                else -> "in < 1 min"
            }
        } else {
            when {
                hours > 0 && minutes > 0 -> "${hours}小時${minutes}分鐘後"
                hours > 0 -> "${hours}小時後"
                minutes > 0 -> "${minutes}分鐘後"
                else -> "不到1分鐘"
            }
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
