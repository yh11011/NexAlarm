package com.nexalarm.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nexalarm.app.data.AlarmSyncRepository
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.util.AlarmScheduler
import com.nexalarm.app.util.FeatureFlags
import java.util.concurrent.TimeUnit

/**
 * 背景同步 Worker：拉取伺服器最新鬧鐘資料（5 分鐘一次，自我重排程）
 * 使用「強制套用」邏輯：伺服器優先，忽略時間戳比較
 * 只在使用者已登入且 Premium 時執行同步。
 */
class AlarmSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsManager(applicationContext)
        val token = settings.authToken ?: return Result.success() // 未登入，跳過
        if (!FeatureFlags.isPremium) return Result.success() // 非 Premium，跳過

        val db = NexAlarmDatabase.getDatabase(applicationContext)
        val alarmDao = db.alarmDao()
        val scheduler = AlarmScheduler(applicationContext)

        val localAlarms = alarmDao.getAllAlarmsList()

        val syncResult = AlarmSyncRepository.sync(token, localAlarms)

        // 同步失敗時返回 retry，WorkManager 會自動重試（指數退避）
        if (syncResult.isFailure) {
            android.util.Log.w("AlarmSyncWorker", "同步失敗，排程重試：${syncResult.exceptionOrNull()?.message}")
            // 同步失敗仍重新套用本地已啟用的鬧鐘，避免排程因網路問題失效
            try {
                alarmDao.getAllAlarmsList()
                    .filter { it.isEnabled }
                    .forEach { scheduler.schedule(it) }
            } catch (e: Exception) {
                android.util.Log.w("AlarmSyncWorker", "重套用本地鬧鐘失敗：${e.message}")
            }
            return Result.retry()
        }

        // 同步成功：強制套用所有伺服器鬧鐘（伺服器優先，忽略時間戳）
        syncResult.onSuccess { serverAlarms ->
            for (serverAlarm in serverAlarms) {
                val existing = alarmDao.getByClientId(serverAlarm.clientId)

                if (serverAlarm.isDeleted) {
                    if (existing != null) {
                        scheduler.cancel(existing)
                        alarmDao.delete(existing)
                    }
                } else {
                    // pull-only：直接強制套用，不比較時間戳
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

        // 自我重排程：5 分鐘後再執行一次
        val nextWork = OneTimeWorkRequestBuilder<AlarmSyncWorker>()
            .setInitialDelay(5, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork("alarm_sync_5min", ExistingWorkPolicy.REPLACE, nextWork)

        return Result.success()
    }
}
