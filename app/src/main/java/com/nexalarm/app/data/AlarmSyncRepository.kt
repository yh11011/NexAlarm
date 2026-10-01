package com.nexalarm.app.data

import com.nexalarm.app.data.model.AlarmEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 負責與伺服器同步鬧鐘資料。
 * 同步策略：last-write-wins（以 updatedAt 時間戳記決定，較新的覆蓋較舊的）
 *
 * HTTP 請求統一透過 ApiClient 發送（自動帶 Bearer token、失敗重試）。
 */
object AlarmSyncRepository {

    private const val SYNC_URL = "https://alarm.nex11.me/auth/alarms/sync"

    /**
     * 雙向同步：上傳本地鬧鐘 + 下載伺服器變更
     * @param token            使用者 JWT token
     * @param localAlarms      本地所有鬧鐘
     * @param deletedClientIds 已刪除但尚未同步的 clientId 列表（clientId to deletedAt）
     * @return 伺服器回傳的鬧鐘（含 is_deleted，供客戶端應用）
     */
    suspend fun sync(
        token: String,
        localAlarms: List<AlarmEntity>,
        deletedClientIds: List<Pair<String, Long>> = emptyList()
    ): Result<List<ServerAlarm>> = withContext(Dispatchers.IO) {
        runCatching {
            val alarmsArray = JSONArray()

            for (alarm in localAlarms) {
                alarmsArray.put(JSONObject().apply {
                    put("client_id",  alarm.clientId)
                    put("data",       if (alarm.isDeleted) JSONObject() else alarmToJson(alarm))
                    put("updated_at", alarm.updatedAt)
                    put("is_deleted", alarm.isDeleted)
                })
            }

            // 相容舊版呼叫方式：明確傳入的待刪除 clientId
            for ((clientId, updatedAt) in deletedClientIds) {
                alarmsArray.put(JSONObject().apply {
                    put("client_id",  clientId)
                    put("data",       JSONObject())
                    put("updated_at", updatedAt)
                    put("is_deleted", true)
                })
            }

            val batches = SyncBatchPolicy.group(List(alarmsArray.length()) { alarmsArray.getJSONObject(it).toString() })
            var response: ApiClient.Response? = null
            for (batch in batches) {
                val body = JSONObject().put("alarms", JSONArray(batch.map { JSONObject(it) })).put("capabilities", 2)
                response = ApiClient.post(SYNC_URL, body, token)
                if (response.code !in 200..299) throw Exception("HTTP ${response.code}")
            }
            val resp = response!!

            val arr = JSONObject(resp.body).getJSONArray("alarms")
            (0 until arr.length()).map { i ->
                val item = arr.getJSONObject(i)
                ServerAlarm(
                    clientId  = item.getString("client_id"),
                    data      = item.getJSONObject("data"),
                    updatedAt = item.getLong("updated_at"),
                    isDeleted = item.optBoolean("is_deleted", false)
                )
            }
        }
    }

    /** 將 AlarmEntity 轉為 JSON（同步到伺服器的格式） */
    fun alarmToJson(alarm: AlarmEntity): JSONObject = JSONObject().apply {
        put("scheduledDate", alarm.scheduledDate ?: JSONObject.NULL)
        put("timePolicy", alarm.timePolicy)
        put("ringtoneUri", alarm.ringtoneUri)
        put("title",            alarm.title)
        put("hour",             alarm.hour)
        put("minute",           alarm.minute)
        put("isEnabled",        alarm.isEnabled)
        put("isRecurring",      alarm.isRecurring)
        put("repeatDays",       JSONArray(alarm.repeatDays))
        put("folderId",         alarm.folderId ?: JSONObject.NULL)
        put("vibrateOnly",      alarm.vibrateOnly)
        put("volume",           alarm.volume)
        put("snoozeDelay",      alarm.snoozeDelay)
        put("maxSnoozeCount",   alarm.maxSnoozeCount)
        put("keepAfterRinging", alarm.keepAfterRinging)
        put("snoozeEnabled",    alarm.snoozeEnabled)
        put("createdAt",        alarm.createdAt)
    }

    /** 從伺服器 JSON 還原 AlarmEntity（保留本地 id，只更新內容） */
    fun jsonToAlarm(json: JSONObject, clientId: String, updatedAt: Long, localId: Long = 0): AlarmEntity {
        val repeatDaysArr = json.optJSONArray("repeatDays")
        val repeatDays = if (repeatDaysArr != null) {
            (0 until repeatDaysArr.length()).map { repeatDaysArr.getInt(it) }
        } else emptyList()

        return AlarmEntity(
            id              = localId,
            scheduledDate = if (json.isNull("scheduledDate")) null else json.optString("scheduledDate").takeIf { it.isNotBlank() },
            timePolicy = json.optString("timePolicy", "device_local"),
            ringtoneUri = json.optString("ringtoneUri", ""),
            clientId        = clientId,
            updatedAt       = updatedAt,
            title           = json.optString("title", ""),
            hour            = json.optInt("hour", 0),
            minute          = json.optInt("minute", 0),
            isEnabled       = json.optBoolean("isEnabled", true),
            isRecurring     = json.optBoolean("isRecurring", false),
            repeatDays      = repeatDays,
            folderId        = if (json.isNull("folderId")) null else json.optLong("folderId"),
            vibrateOnly     = json.optBoolean("vibrateOnly", false),
            volume          = json.optInt("volume", 80),
            snoozeDelay     = json.optInt("snoozeDelay", 10),
            maxSnoozeCount  = json.optInt("maxSnoozeCount", 3),
            keepAfterRinging = json.optBoolean("keepAfterRinging", false),
            snoozeEnabled   = json.optBoolean("snoozeEnabled", true),
            createdAt       = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

data class ServerAlarm(
    val clientId:  String,
    val data:      JSONObject,
    val updatedAt: Long,
    val isDeleted: Boolean
)
