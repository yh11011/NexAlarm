package com.nexalarm.app.data

import android.content.Context
import android.os.Build
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessaging
import com.nexalarm.app.worker.AlarmSyncWorker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.time.ZoneId
import java.util.UUID

object AiDeviceRepository {
    const val BASE = "https://login.nex11.me/api/v1"
    val syncMutex = Mutex()

    fun enqueue(context: Context) {
        val builder = OneTimeWorkRequestBuilder<AlarmSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        // Older Android requires a foreground notification for expedited work.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }
        val request = builder.build()
        WorkManager.getInstance(context).enqueueUniqueWork("ai_alarm_sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun refreshPushToken(context: Context) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            context.getSharedPreferences("ai_device", Context.MODE_PRIVATE).edit().putString("fcm_token", it).apply()
            enqueue(context)
        }
    }

    fun deviceId(context: Context, settings: SettingsManager): String {
        val prefs = context.getSharedPreferences("ai_device", Context.MODE_PRIVATE)
        val owner = settings.authUserId.toString()
        val current = prefs.getString("device_id", null)
        if (current != null && prefs.getString("owner", null) == owner) return current
        val id = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", id).putString("owner", owner).commit()
        return id
    }

    suspend fun uploadSchedules(context: Context, token: String, settings: SettingsManager) {
        val dao = com.nexalarm.app.data.database.NexAlarmDatabase.getDatabase(context).alarmDao()
        val ledger = ScheduleLedger(context)
        val scheduler = com.nexalarm.app.util.AlarmScheduler(context)
        val alarms = dao.getAllAlarmsList()
        for (alarm in alarms) {
            if (settings.authToken != token) return
            // Initialize evidence for installations predating the ledger without resetting snooze.
            if (ledger.current(alarm) == null) {
                runCatching { if (alarm.isDeleted || !alarm.isEnabled) scheduler.cancel(alarm) else scheduler.schedule(alarm) }
            }
        }
        for (chunk in alarms.chunked(20)) {
            if (settings.authToken != token) return
            val body = JSONObject().put("alarms", org.json.JSONArray(chunk.map { ledger.report(it) }))
            val result = ApiClient.post("$BASE/devices/${deviceId(context, settings)}/schedule-status", body, token)
            if (result.code !in 200..299) throw SyncFailure(result.code)
        }
    }

    suspend fun sync(context: Context, token: String, settings: SettingsManager): Int {
        val prefs = context.getSharedPreferences("ai_device", Context.MODE_PRIVATE)
        val id = deviceId(context, settings)
        val registration = JSONObject().put("device_id", id).put("name", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("fcm_token", prefs.getString("fcm_token", ""))
            .put("timezone", ZoneId.systemDefault().id).put("capabilities", 2)
            .put("app_version", com.nexalarm.app.BuildConfig.VERSION_NAME).put("app_version_code", com.nexalarm.app.BuildConfig.VERSION_CODE)
        val registered = ApiClient.post("$BASE/devices/register", registration, token)
        // A downgraded account may still acknowledge already-queued operations.
        if (registered.code !in 200..299 && registered.code != 403) throw SyncFailure(registered.code)
        SyncDiagnostics.save(context, if (registered.code == 403) "premium_required" else "registered")
        val pending = ApiClient.get("$BASE/devices/$id/pending", token)
        if (pending.code == 404 && registered.code == 403) return -1
        if (pending.code !in 200..299) throw SyncFailure(pending.code)
        val operations = JSONObject(pending.body).getJSONArray("operations")
        val applier = AlarmSyncApplier(context)
        var applied = 0
        for (i in 0 until operations.length()) {
            if (settings.authToken != token) break
            val operation = operations.getJSONObject(i)
            val opId = operation.getString("operation_id")
            val cacheKey = "receipt_$id:$opId"
            val cached = prefs.getString(cacheKey, null)
            val receipt = if (cached != null) JSONObject(cached) else {
                val payload = operation.getJSONObject("alarm")
                val remote = ServerAlarm(payload.getString("client_id"), payload.getJSONObject("data"),
                    payload.getLong("updated_at"), payload.optBoolean("is_deleted"))
                val result = try {
                    applier.apply(remote, forceSchedule = true)!!
                } catch (e: Exception) {
                    com.nexalarm.app.util.AlarmScheduler.ScheduleResult("failed", reason = "Unable to schedule this alarm")
                }
                applied++
                JSONObject().put("operation_id", opId).put("version", operation.getLong("version"))
                    .put("status", result.status).put("trigger_at", result.triggerAt ?: JSONObject.NULL)
                    .put("timezone", ZoneId.systemDefault().id).put("reason", result.reason ?: JSONObject.NULL)
                    .also { check(prefs.edit().putString(cacheKey, it.toString()).commit()) }
            }
            val ack = ApiClient.post("$BASE/devices/$id/receipt", receipt, token)
            if (ack.code !in 200..299) throw SyncFailure(ack.code)
            prefs.edit().remove(cacheKey).commit()
        }
        return if (registered.code == 403) -1 else applied
    }
}
