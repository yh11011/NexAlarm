package com.nexalarm.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nexalarm.app.MainActivity
import com.nexalarm.app.receiver.AlarmReceiver
import java.util.Date

object TestAlarmScheduler {
    private const val TEST_REQUEST_CODE = -30_000
    private const val TEST_ALARM_ID = -30_000L

    fun schedule(context: Context): TestAlarmPlan {
        val plan = TestAlarmPlanner.createPlan()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, TEST_ALARM_ID)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, plan.title)
            putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE_ONLY, false)
            putExtra(AlarmReceiver.EXTRA_ALARM_SNOOZE_ENABLED, false)
            putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, plan.volume)
            putExtra(AlarmReceiver.EXTRA_ALARM_RINGTONE_URI, "")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_REQUEST_CODE,
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.triggerAtMs, pendingIntent)
        } else {
            val showIntent = PendingIntent.getActivity(
                context,
                TEST_REQUEST_CODE,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(plan.triggerAtMs, showIntent),
                pendingIntent
            )
        }

        android.util.Log.i(
            "NexAlarmTest",
            "TEST_SCHEDULED|triggerMs=${plan.triggerAtMs}|at=${Date(plan.triggerAtMs)}"
        )
        return plan
    }
}
