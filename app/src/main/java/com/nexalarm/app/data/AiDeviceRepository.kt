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
        val request = OneTimeWorkRequestBuilder<AlarmSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()
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

    suspend fun sync(context: Context, token: String, settings: SettingsManager): Int {
        val prefs = context.getSharedPreferences("ai_device", Context.MODE_PRIVATE)
        val id = deviceId(context, settings)
        val registration = JSONObject().put("device_id", id).put("name", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("fcm_token", prefs.getString("fcm_token", ""))
            .put("timezone", ZoneId.systemDefault().id).put("capabilities", 2)
        val registered = ApiClient.post("$BASE/devices/register", registration, token)
        // A downgraded account may still acknowledge already-queued operations.
        if (registered.code !in 200..299 && registered.code != 403) error("Device registration failed: ${registered.code}")
        val pending = ApiClient.get("$BASE/devices/$id/pending", token)
        if (pending.code == 404 && registered.code == 403) return 0
        check(pending.code in 200..299) { "Pending operation fetch failed: ${pending.code}" }
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
            check(ack.code in 200..299) { "Receipt failed: ${ack.code}" }
            prefs.edit().remove(cacheKey).commit()
        }
        return applied
    }
}
