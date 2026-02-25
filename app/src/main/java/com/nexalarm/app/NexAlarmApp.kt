package com.nexalarm.app

import android.app.Application
import com.nexalarm.app.util.NotificationHelper

/**
 * Application 類別
 * 負責 App 啟動時的初始化工作
 */
class NexAlarmApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 建立通知頻道
        NotificationHelper.createNotificationChannels(this)

        android.util.Log.d("NexAlarmApp", "Application initialized")
    }
}
