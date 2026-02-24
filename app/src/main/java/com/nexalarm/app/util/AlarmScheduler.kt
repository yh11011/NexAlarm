package com.nexalarm.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    private const val EXTRA_ALARM_ID = "extra_alarm_id"

    fun schedule(context: Context, alarm: AlarmEntity) {
        if (!alarm.isEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val triggerTime = calculateNextTrigger(alarm)
        val pendingIntent = createPendingIntent(context, alarm)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun cancel(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, alarm)
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleSnooze(context: Context, alarm: AlarmEntity, snoozeMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val triggerTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
        val pendingIntent = createPendingIntent(context, alarm)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun calculateNextTrigger(alarm: AlarmEntity): Long {
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.isRecurring && alarm.repeatDays.isNotEmpty()) {
            val todayDow = mapCalendarDow(now.get(Calendar.DAY_OF_WEEK))
            val sorted = alarm.repeatDays.sorted()

            // Find next valid day
            var nextDay = sorted.firstOrNull { it > todayDow }
                ?: sorted.firstOrNull { it >= todayDow && trigger.after(now) }

            if (nextDay == null || (nextDay == todayDow && !trigger.after(now))) {
                // Go to next week's first day
                nextDay = sorted.first()
                val daysUntil = if (nextDay > todayDow) {
                    nextDay - todayDow
                } else {
                    7 - todayDow + nextDay
                }
                trigger.add(Calendar.DAY_OF_YEAR, daysUntil)
            } else if (nextDay == todayDow && trigger.after(now)) {
                // Today, but time not passed
            } else {
                val daysUntil = if (nextDay > todayDow) {
                    nextDay - todayDow
                } else {
                    7 - todayDow + nextDay
                }
                trigger.add(Calendar.DAY_OF_YEAR, daysUntil)
            }
        } else {
            // One-time alarm: if time already passed today, schedule tomorrow
            if (!trigger.after(now)) {
                trigger.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return trigger.timeInMillis
    }

    private fun createPendingIntent(context: Context, alarm: AlarmEntity): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            action = "com.nexalarm.ALARM_TRIGGER_${alarm.id}"
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Maps Calendar's DAY_OF_WEEK (Sun=1..Sat=7) to our format (Mon=1..Sun=7).
     */
    private fun mapCalendarDow(calDow: Int): Int {
        return when (calDow) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }
}
