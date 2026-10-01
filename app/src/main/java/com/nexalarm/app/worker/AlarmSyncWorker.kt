package com.nexalarm.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexalarm.app.MainActivity
import com.nexalarm.app.R
import com.nexalarm.app.data.*
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.util.NotificationHelper
import kotlinx.coroutines.sync.withLock

class AlarmSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = AiDeviceRepository.syncMutex.withLock {
        val settings = SettingsManager(applicationContext)
        try {
            settings.flushDeviceUnregistrations()
            val token = settings.authToken ?: return@withLock Result.success()
            SyncDiagnostics.save(applicationContext, "syncing")
            val applied = AiDeviceRepository.sync(applicationContext, token, settings)
            if (applied > 0 && NotificationHelper.hasNotificationPermission(applicationContext)) {
                val intent = PendingIntent.getActivity(applicationContext, 701,
                    Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID_REMINDER)
                    .setSmallIcon(R.drawable.ic_alarm).setContentTitle(com.nexalarm.app.ui.theme.S.aiSyncTitle(settings.isEnglish))
                    .setContentText(com.nexalarm.app.ui.theme.S.aiSyncBody(settings.isEnglish))
                    .setContentIntent(intent).setAutoCancel(true).build()
                (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(701, notification)
            }
            if (settings.authToken != token || applied < 0) return@withLock Result.success()
            val dao = NexAlarmDatabase.getDatabase(applicationContext).alarmDao()
            val result = AlarmSyncRepository.sync(token, dao.getAllAlarmsList())
            if (result.isFailure) {
                val code = Regex("HTTP (\\d+)").find(result.exceptionOrNull()?.message.orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 0
                throw SyncFailure(code)
            }
            val applier = AlarmSyncApplier(applicationContext)
            for (remote in result.getOrThrow()) {
                if (settings.authToken != token) break
                applier.apply(remote)
            }
            AiDeviceRepository.uploadSchedules(applicationContext, token, settings)
            if (settings.authToken == token) SyncDiagnostics.save(applicationContext, "success", success = true)
            Result.success()
        } catch (e: Exception) {
            val phase = when ((e as? SyncFailure)?.statusCode) {
                401 -> "login_required"
                403 -> "premium_required"
                409 -> "registration_conflict"
                else -> "network_error"
            }
            SyncDiagnostics.save(applicationContext, phase)
            if (phase == "login_required" || phase == "premium_required") return@withLock Result.success()
            // Tokens and API bodies are intentionally excluded from background logs.
            Result.retry()
        }
    }
}
