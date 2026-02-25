package com.nexalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 開機自動啟動接收器
 * 當裝置重新啟動後，重新排程所有啟用的鬧鐘
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted, rescheduling alarms")

            // 使用協程重新排程鬧鐘
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    rescheduleAlarms(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    /**
     * 重新排程所有啟用的鬧鐘
     */
    private suspend fun rescheduleAlarms(context: Context) {
        val database = NexAlarmDatabase.getDatabase(context)
        val alarmDao = database.alarmDao()
        val scheduler = AlarmScheduler(context)

        // 取得所有啟用的鬧鐘
        val enabledAlarms = alarmDao.getEnabledAlarmsList()

        Log.d("BootReceiver", "Found ${enabledAlarms.size} enabled alarms")

        // 重新排程每一個鬧鐘
        enabledAlarms.forEach { alarm ->
            scheduler.schedule(alarm)
            Log.d("BootReceiver", "Rescheduled alarm: ${alarm.id} - ${alarm.title}")
        }
    }
}
