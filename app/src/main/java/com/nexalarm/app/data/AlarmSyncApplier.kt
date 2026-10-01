package com.nexalarm.app.data

import android.content.Context
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.util.AlarmScheduler

/** Shared Room and system-scheduler application for foreground and background synchronization. */
class AlarmSyncApplier(context: Context) {
    private val dao = NexAlarmDatabase.getDatabase(context).alarmDao()
    private val scheduler = AlarmScheduler(context)

    suspend fun apply(remote: ServerAlarm, forceSchedule: Boolean = false): AlarmScheduler.ScheduleResult? {
        val local = dao.getByClientId(remote.clientId)
        if (local != null && local.updatedAt > remote.updatedAt) {
            return AlarmScheduler.ScheduleResult("superseded", reason = "Newer local version exists")
        }
        if (remote.isDeleted) {
            if (local != null) { scheduler.cancel(local); dao.delete(local) }
            return AlarmScheduler.ScheduleResult("cancelled")
        }
        if (local != null && local.updatedAt == remote.updatedAt && !forceSchedule) return null
        val alarm = AlarmSyncRepository.jsonToAlarm(remote.data, remote.clientId, remote.updatedAt, local?.id ?: 0L)
        val saved = if (local == null) alarm.copy(id = dao.insert(alarm)) else alarm.also { dao.update(it) }
        return scheduler.schedule(saved)
    }
}
