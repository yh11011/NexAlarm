package com.nexalarm.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexalarm.app.data.AlarmSyncRepository
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.util.AlarmScheduler

/**
 * 背景同步 Worker：由 WorkManager 每 15 分鐘執行一次。
 * 只在使用者已登入時執行同步。
 */
class AlarmSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsManager(applicationContext)
        val token = settings.authToken ?: return Result.success() // 未登入，跳過

        val db = NexAlarmDatabase.getDatabase(applicationContext)
        val alarmDao = db.alarmDao()
        val scheduler = AlarmScheduler(applicationContext)

        val localAlarms = alarmDao.getAllAlarmsList()

        val syncResult = AlarmSyncRepository.sync(token, localAlarms)
        syncResult.onSuccess { serverAlarms ->
            for (serverAlarm in serverAlarms) {
                val existing = alarmDao.getByClientId(serverAlarm.clientId)

                if (serverAlarm.isDeleted) {
                    // 伺服器標記刪除 → 本地刪除
                    if (existing != null) {
                        scheduler.cancel(existing)
                        alarmDao.delete(existing)
                    }
                } else {
                    val serverUpdatedAt = serverAlarm.updatedAt
                    val localUpdatedAt = existing?.updatedAt ?: 0L

                    if (serverUpdatedAt > localUpdatedAt) {
                        // 伺服器版本較新 → 覆蓋本地
                        val newAlarm = AlarmSyncRepository.jsonToAlarm(
                            serverAlarm.data,
                            serverAlarm.clientId,
                            serverAlarm.updatedAt,
                            localId = existing?.id ?: 0L
                        )
                        if (existing == null) {
                            val newId = alarmDao.insert(newAlarm)
                            if (newAlarm.isEnabled) scheduler.schedule(newAlarm.copy(id = newId))
                        } else {
                            alarmDao.update(newAlarm)
                            if (newAlarm.isEnabled) scheduler.schedule(newAlarm)
                            else scheduler.cancel(newAlarm)
                        }
                    }
                }
            }
        }

        return Result.success()
    }
}
