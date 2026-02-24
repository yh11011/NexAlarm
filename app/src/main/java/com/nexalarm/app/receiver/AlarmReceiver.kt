package com.nexalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nexalarm.app.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        context.startForegroundService(serviceIntent)
    }
}
