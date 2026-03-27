package com.nexalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexalarm.app.data.AlarmSyncRepository
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.util.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 鬧鐘 ViewModel
 * 加入 AlarmScheduler 整合
 */
class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NexAlarmDatabase.getDatabase(application)
    private val alarmDao = database.alarmDao()
    private val scheduler = AlarmScheduler(application)
    private val settings = SettingsManager(application)

    /** 有操作時立即觸發與伺服器同步（僅在已登入時執行） */
    private fun triggerSync() {
        val token = settings.authToken ?: return
        viewModelScope.launch {
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

    private suspend fun applyServerAlarms(serverAlarms: List<com.nexalarm.app.data.ServerAlarm>) {
        for (serverAlarm in serverAlarms) {
            val existing = alarmDao.getByClientId(serverAlarm.clientId)
            if (serverAlarm.isDeleted) {
                if (existing != null) { scheduler.cancel(existing); alarmDao.delete(existing) }
            } else if (serverAlarm.updatedAt > (existing?.updatedAt ?: 0L)) {
                val newAlarm = AlarmSyncRepository.jsonToAlarm(
                    serverAlarm.data, serverAlarm.clientId, serverAlarm.updatedAt, existing?.id ?: 0L
                )
                if (existing == null) {
                    val newId = alarmDao.insert(newAlarm)
                    if (newAlarm.isEnabled) scheduler.schedule(newAlarm.copy(id = newId))
                } else {
                    alarmDao.update(newAlarm)
                    if (newAlarm.isEnabled) scheduler.schedule(newAlarm) else scheduler.cancel(newAlarm)
                }
            }
        }
    }

    // 所有鬧鐘
    private val _allAlarms = MutableStateFlow<List<AlarmEntity>>(emptyList())
    val allAlarms: StateFlow<List<AlarmEntity>> = _allAlarms

    // 單次鬧鐘
    private val _singleAlarms = MutableStateFlow<List<AlarmEntity>>(emptyList())
    val singleAlarms: StateFlow<List<AlarmEntity>> = _singleAlarms

    // 重複鬧鐘
    private val _repeatAlarms = MutableStateFlow<List<AlarmEntity>>(emptyList())
    val repeatAlarms: StateFlow<List<AlarmEntity>> = _repeatAlarms

    // 下一個要響的鬧鐘（StateFlow，自動隨 allAlarms 更新，UI 可直接 collectAsState）
    val nextAlarm: StateFlow<AlarmEntity?> = _allAlarms
        .map { alarms ->
            alarms.filter { it.isEnabled }
                  .minByOrNull { scheduler.getNextTriggerTime(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        loadAlarms()
    }

    /**
     * 載入所有鬧鐘
     */
    private fun loadAlarms() {
        viewModelScope.launch {
            alarmDao.getAllAlarms().collect { alarms ->
                _allAlarms.value = alarms
                _singleAlarms.value = alarms.filter { !it.isRecurring }
                _repeatAlarms.value = alarms.filter { it.isRecurring }
            }
        }
    }

    /**
     * 新增或更新鬧鐘
     * ⚠️ 重點：會自動排程鬧鐘
     */
    fun saveAlarm(alarm: AlarmEntity) {
        // 確保每次儲存都更新 updatedAt
        val now = System.currentTimeMillis()
        val alarmWithTime = alarm.copy(updatedAt = now)
        viewModelScope.launch {
            if (alarmWithTime.id == 0L) {
                val newId = alarmDao.insert(alarmWithTime)
                scheduler.schedule(alarmWithTime.copy(id = newId))
            } else {
                alarmDao.update(alarmWithTime)
                scheduler.schedule(alarmWithTime)
            }
            triggerSync()
        }
    }

    /**
     * 切換鬧鐘啟用狀態
     * ⚠️ 重點：會自動排程或取消排程
     */
    fun toggleAlarm(alarm: AlarmEntity) {
        val updated = alarm.copy(isEnabled = !alarm.isEnabled, updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            alarmDao.update(updated)
            if (updated.isEnabled) scheduler.schedule(updated) else scheduler.cancel(updated)
            triggerSync()
        }
    }

    /**
     * 刪除鬧鐘
     * ⚠️ 重點：會自動取消排程
     */
    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            scheduler.cancel(alarm)
            alarmDao.delete(alarm)
            // 通知伺服器刪除（以軟刪除形式同步）
            val token = settings.authToken ?: return@launch
            val deletedAt = System.currentTimeMillis()
            AlarmSyncRepository.sync(
                token = token,
                localAlarms = alarmDao.getAllAlarmsList(),
                deletedClientIds = listOf(alarm.clientId to deletedAt)
            )
        }
    }

    /**
     * 批次刪除鬧鐘
     */
    fun deleteAlarms(alarms: List<AlarmEntity>) {
        viewModelScope.launch {
            alarms.forEach { alarm ->
                scheduler.cancel(alarm)
            }
            alarmDao.deleteAll(alarms)
            // 同步刪除到雲端
            val token = settings.authToken ?: return@launch
            val deletedAt = System.currentTimeMillis()
            AlarmSyncRepository.sync(
                token = token,
                localAlarms = alarmDao.getAllAlarmsList(),
                deletedClientIds = alarms.map { it.clientId to deletedAt }
            )
        }
    }

    /**
     * 取得特定資料夾的鬧鐘
     */
    fun getAlarmsByFolder(folderId: Long): StateFlow<List<AlarmEntity>> {
        val flow = MutableStateFlow<List<AlarmEntity>>(emptyList())
        viewModelScope.launch {
            alarmDao.getAlarmsByFolder(folderId).collect { alarms ->
                flow.value = alarms
            }
        }
        return flow
    }

    /**
     * 取得下一個要響的鬧鐘（同步讀值，建議 UI 改用 nextAlarm StateFlow）
     */
    fun getNextAlarm(): AlarmEntity? = nextAlarm.value

    /**
     * 取得距離下次響鈴的時間文字
     */
    fun getTimeUntilNextAlarm(): String {
        val nextAlarm = getNextAlarm() ?: return ""
        val isEnglish = SettingsManager(getApplication()).isEnglish
        return scheduler.getTimeUntilText(nextAlarm, isEnglish)
    }

    /**
     * 根據 ID 取得鬧鐘
     */
    suspend fun getAlarmById(id: Long): AlarmEntity? {
        return alarmDao.getAlarmById(id)
    }

    /**
     * 更新鬧鐘（含排程）
     */
    fun updateAlarm(alarm: AlarmEntity) {
        val updated = alarm.copy(updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            alarmDao.update(updated)
            if (updated.isEnabled) scheduler.schedule(updated) else scheduler.cancel(updated)
            triggerSync()
        }
    }
}
