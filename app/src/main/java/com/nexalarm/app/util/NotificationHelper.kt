package com.nexalarm.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * 通知管理工具
 * 負責建立通知頻道和發送通知
 */
object NotificationHelper {

    // 通知頻道 ID
    const val CHANNEL_ID_ALARM = "alarm_channel"
    const val CHANNEL_ID_REMINDER = "reminder_channel"
    const val CHANNEL_ID_STOPWATCH = "stopwatch_channel"
    const val CHANNEL_ID_TIMER = "timer_channel"

    /**
     * 建立所有通知頻道
     */
    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        // 鬧鐘頻道
        val alarmChannel = NotificationChannel(
            CHANNEL_ID_ALARM,
            "鬧鐘",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "鬧鐘響鈴通知"
            setSound(null, null) // 使用自訂音效
            enableVibration(false) // 使用自訂震動
            setBypassDnd(true) // 可繞過勿擾模式
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // 提醒頻道
        val reminderChannel = NotificationChannel(
            CHANNEL_ID_REMINDER,
            "提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "一般提醒通知"
        }

        // 碼錶前台服務頻道
        val stopwatchChannel = NotificationChannel(
            CHANNEL_ID_STOPWATCH,
            "碼錶",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "碼錶運行中"
            setShowBadge(false)
        }

        // 計時器前台服務頻道
        val timerChannel = NotificationChannel(
            CHANNEL_ID_TIMER,
            "計時器",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "計時器運行中"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(
            listOf(alarmChannel, reminderChannel, stopwatchChannel, timerChannel)
        )
    }

    /**
     * 檢查通知權限
     * Android 13+ 需要請求通知權限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            return notificationManager.areNotificationsEnabled()
        }
        return true
    }

    /**
     * 檢查全螢幕通知權限
     * Android 14+ 需要額外權限
     */
    fun hasFullScreenIntentPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            return notificationManager.canUseFullScreenIntent()
        }
        return true
    }
}

