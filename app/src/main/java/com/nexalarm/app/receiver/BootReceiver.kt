package com.nexalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.repository.AlarmRepository
import com.nexalarm.app.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val db = NexAlarmDatabase.getDatabase(context)
        val repository = AlarmRepository(db.alarmDao())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarms = repository.getEnabledAlarmsList()
                for (alarm in alarms) {
                    AlarmScheduler.schedule(context, alarm)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
