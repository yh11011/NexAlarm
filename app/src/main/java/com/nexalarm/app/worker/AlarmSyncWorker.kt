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
            if (settings.authToken != token || !settings.isPremium) return@withLock Result.success()
            val dao = NexAlarmDatabase.getDatabase(applicationContext).alarmDao()
            val result = AlarmSyncRepository.sync(token, dao.getAllAlarmsList())
            if (result.isFailure) return@withLock Result.retry()
            val applier = AlarmSyncApplier(applicationContext)
            for (remote in result.getOrThrow()) {
                if (settings.authToken != token) break
                applier.apply(remote)
            }
            Result.success()
        } catch (_: Exception) {
            // Tokens and API bodies are intentionally excluded from background logs.
            Result.retry()
        }
    }
}
