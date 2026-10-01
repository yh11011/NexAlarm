package com.nexalarm.app.data

import android.content.Context
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.util.AlarmScheduler
import org.json.JSONObject
import java.time.ZoneId

/** Durable evidence of actual scheduler calls; never infer success from an enabled flag. */
class ScheduleLedger(context: Context) {
    private val prefs = context.getSharedPreferences("alarm_schedule_ledger", Context.MODE_PRIVATE)

    fun record(alarm: AlarmEntity, result: AlarmScheduler.ScheduleResult) {
        val entry = JSONObject().put("client_id", alarm.clientId).put("version", alarm.updatedAt)
            .put("status", result.status).put("trigger_at", result.triggerAt ?: JSONObject.NULL)
            .put("timezone", ZoneId.systemDefault().id).put("reason", result.reason ?: JSONObject.NULL)
            .put("reported_at", System.currentTimeMillis())
        check(prefs.edit().putString(alarm.clientId, entry.toString()).commit())
    }

    fun current(alarm: AlarmEntity): JSONObject? = prefs.getString(alarm.clientId, null)?.let {
        JSONObject(it).takeIf { entry -> entry.optLong("version") == alarm.updatedAt }
    }

    fun report(alarm: AlarmEntity): JSONObject = current(alarm) ?: JSONObject()
        .put("client_id", alarm.clientId).put("version", alarm.updatedAt).put("status", "unknown")
        .put("trigger_at", JSONObject.NULL).put("timezone", ZoneId.systemDefault().id)
        .put("reported_at", System.currentTimeMillis())
}
