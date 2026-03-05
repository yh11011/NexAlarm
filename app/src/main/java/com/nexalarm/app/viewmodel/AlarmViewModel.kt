package com.nexalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.util.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 鬧鐘 ViewModel
 * 加入 AlarmScheduler 整合
 */
class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NexAlarmDatabase.getDatabase(application)
    private val alarmDao = database.alarmDao()
    private val scheduler = AlarmScheduler(application)

    // 所有鬧鐘
    private val _allAlarms = MutableStateFlow<List<AlarmEntity>>(emptyList())
    val allAlarms: StateFlow<List<AlarmEntity>> = _allAlarms

    // 單次鬧鐘
    private val _singleAlarms = MutableStateFlow<List<AlarmEntity>>(emptyList())
    val singleAlarms: StateFlow<List<AlarmEntity>> = _singleAlarms

    // 重複鬧鐘
    private val _repeatAlarms = MutableStateFlow<List<AlarmEntity>>(emptyList())
    val repeatAlarms: StateFlow<List<AlarmEntity>> = _repeatAlarms

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
        viewModelScope.launch {
            if (alarm.id == 0L) {
                // 新增
                val newId = alarmDao.insert(alarm)
                val newAlarm = alarm.copy(id = newId)
                scheduler.schedule(newAlarm)
            } else {
                // 更新
                alarmDao.update(alarm)
                scheduler.schedule(alarm)
            }
        }
    }

    /**
     * 切換鬧鐘啟用狀態
     * ⚠️ 重點：會自動排程或取消排程
     */
    fun toggleAlarm(alarm: AlarmEntity) {
        val updated = alarm.copy(isEnabled = !alarm.isEnabled)
        viewModelScope.launch {
            alarmDao.update(updated)

            if (updated.isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
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
     * 取得下一個要響的鬧鐘
     */
    fun getNextAlarm(): AlarmEntity? {
        val enabledAlarms = allAlarms.value.filter { it.isEnabled }
        if (enabledAlarms.isEmpty()) return null

        // 找出最早要響的鬧鐘
        return enabledAlarms.minByOrNull { alarm ->
            scheduler.getNextTriggerTime(alarm)
        }
    }

    /**
     * 取得距離下次響鈴的時間文字
     */
    fun getTimeUntilNextAlarm(): String {
        val nextAlarm = getNextAlarm() ?: return ""
        return scheduler.getTimeUntilText(nextAlarm)
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
        viewModelScope.launch {
            alarmDao.update(alarm)
            if (alarm.isEnabled) {
                scheduler.schedule(alarm)
            } else {
                scheduler.cancel(alarm)
            }
        }
    }
}
