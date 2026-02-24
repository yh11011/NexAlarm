package com.nexalarm.app

import android.app.Application
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.service.AlarmService

class NexAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize database (triggers prepopulate callback on first run)
        NexAlarmDatabase.getDatabase(this)
        // Create notification channels
        AlarmService.createNotificationChannel(this)
    }
}
