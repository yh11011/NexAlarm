package com.nexalarm.app.util

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

object AlarmReliabilityChecker {
    fun evaluate(context: Context): ReliabilityState {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exactAlarmReady = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryUnrestricted = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        return AlarmReliabilityAdvisor.evaluate(
            notificationEnabled = NotificationHelper.hasNotificationPermission(context),
            exactAlarmReady = exactAlarmReady,
            fullScreenReady = NotificationHelper.hasFullScreenIntentPermission(context),
            batteryUnrestricted = batteryUnrestricted,
            bootRescheduleReady = true
        )
    }
}
