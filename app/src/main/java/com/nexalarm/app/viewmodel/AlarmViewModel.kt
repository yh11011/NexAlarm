package com.nexalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.repository.AlarmRepository
import com.nexalarm.app.util.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository
    val allAlarms: StateFlow<List<AlarmEntity>>

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId

    val filteredAlarms: StateFlow<List<AlarmEntity>>

    init {
        val db = NexAlarmDatabase.getDatabase(application)
        repository = AlarmRepository(db.alarmDao())

        allAlarms = repository.getAllAlarms()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        filteredAlarms = combine(allAlarms, _selectedFolderId) { alarms, folderId ->
            if (folderId == null) alarms else alarms.filter { it.folderId == folderId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setFolderFilter(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun saveAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val id = repository.insert(alarm.copy(
                folderId = alarm.folderId,
                isEnabled = true
            ))
            val saved = alarm.copy(id = id, isEnabled = true)
            AlarmScheduler.schedule(getApplication(), saved)
        }
    }

    fun updateAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.update(alarm)
            if (alarm.isEnabled) {
                AlarmScheduler.schedule(getApplication(), alarm)
            } else {
                AlarmScheduler.cancel(getApplication(), alarm)
            }
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        val toggled = alarm.copy(isEnabled = !alarm.isEnabled)
        viewModelScope.launch {
            repository.update(toggled)
            if (toggled.isEnabled) {
                AlarmScheduler.schedule(getApplication(), toggled)
            } else {
                AlarmScheduler.cancel(getApplication(), toggled)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), alarm)
            repository.delete(alarm)
        }
    }

    suspend fun getAlarmById(id: Long): AlarmEntity? = repository.getAlarmById(id)

    /**
     * Used by URI deep link handler. Deduplicates based on time+title+folder+repeat.
     */
    fun insertOrUpdateFromUri(alarm: AlarmEntity) {
        viewModelScope.launch {
            val id = repository.insertOrUpdate(alarm)
            val saved = alarm.copy(id = id, isEnabled = true)
            AlarmScheduler.schedule(getApplication(), saved)
        }
    }

    fun deleteAlarmById(id: Long) {
        viewModelScope.launch {
            val alarm = repository.getAlarmById(id)
            if (alarm != null) {
                AlarmScheduler.cancel(getApplication(), alarm)
                repository.deleteById(id)
            }
        }
    }
}
