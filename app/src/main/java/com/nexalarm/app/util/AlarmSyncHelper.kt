package com.nexalarm.app.util

import androidx.lifecycle.viewModelScope
import com.nexalarm.app.data.AlarmSyncRepository
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 鬧鐘同步輔助類
 *
 * 負責處理與伺服器的雲端同步邏輯：
 * - 觸發同步
 * - 應用伺服器端的鬧鐘變更
 * - 處理軟刪除和硬刪除邏輯
 */
class AlarmSyncHelper(
    private val scope: CoroutineScope,
    private val settings: SettingsManager,
    private val alarmDao: com.nexalarm.app.data.database.AlarmDao,
    private val scheduler: AlarmScheduler
) {

    /**
     * 觸發與伺服器同步（僅在已登入時執行）
     */
    fun triggerSync() {
        val token = settings.authToken ?: return
        if (!FeatureFlags.isPremium) return
        scope.launch {
            // getAllAlarmsList() 包含軟刪除（is_deleted=true）的鬧鐘，
            // 同步時會帶 is_deleted:true 送至伺服器；伺服器確認後 applyServerAlarms 會硬刪
            val localAlarms = alarmDao.getAllAlarmsList()
            AlarmSyncRepository.sync(token, localAlarms)
                .onSuccess { serverAlarms ->
                    applyServerAlarms(serverAlarms)
                }
                .onFailure { e ->
                    // Token 過期（401）：自動清除登入狀態，下次操作或開啟帳號頁時提示重新登入
                    if (e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true) {
                        settings.clearAuth()
                    }
                }
        }
    }

    /**
     * 應用伺服器端的鬧鐘變更
     */
    private suspend fun applyServerAlarms(serverAlarms: List<com.nexalarm.app.data.ServerAlarm>) {
        for (serverAlarm in serverAlarms) {
            val existing = alarmDao.getByClientId(serverAlarm.clientId)
            if (serverAlarm.isDeleted) {
                if (existing != null) {
                    scheduler.cancel(existing)
                    alarmDao.delete(existing)
                }
            } else if (serverAlarm.updatedAt > (existing?.updatedAt ?: 0L)) {
                val newAlarm = AlarmSyncRepository.jsonToAlarm(
                    serverAlarm.data, serverAlarm.clientId, serverAlarm.updatedAt, existing?.id ?: 0L
                )
                if (existing == null) {
                    val newId = alarmDao.insert(newAlarm)
                    if (newAlarm.isEnabled) {
                        scheduler.schedule(newAlarm.copy(id = newId))
                    }
                } else {
                    alarmDao.update(newAlarm)
                    if (newAlarm.isEnabled) {
                        scheduler.schedule(newAlarm)
                    } else {
                        scheduler.cancel(newAlarm)
                    }
                }
            }
        }
    }
}
