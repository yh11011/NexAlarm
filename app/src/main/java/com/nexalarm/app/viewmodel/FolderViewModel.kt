package com.nexalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.data.repository.FolderRepository
import com.nexalarm.app.ui.theme.S
import com.nexalarm.app.util.AlarmScheduler
import com.nexalarm.app.util.AlarmSyncHelper
import com.nexalarm.app.util.FeatureFlags
import com.nexalarm.app.util.ScheduleCommand
import com.nexalarm.app.util.ScheduleGroupDuplicator
import com.nexalarm.app.util.ScheduleGroupPlanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NexAlarmDatabase.getDatabase(application)
    private val repository: FolderRepository
    private val scheduler = AlarmScheduler(application)
    private val settings = SettingsManager(application)
    private val syncHelper = AlarmSyncHelper(viewModelScope, settings, database.alarmDao(), scheduler)
    val allFolders: StateFlow<List<FolderEntity>>

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage

    init {
        repository = FolderRepository(database.folderDao())

        allFolders = repository.getAllFolders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addFolder(name: String, color: String = "#1A73E8", emoji: String = "calendar") {
        viewModelScope.launch {
            val count = repository.getUserFolderCount()
            if (!FeatureFlags.canCreateFolder(count)) {
                _errorMessage.emit(S.folderLimitReached)
                return@launch
            }
            repository.insert(FolderEntity(name = name, color = color, emoji = emoji))
        }
    }

    fun updateFolder(folder: FolderEntity) {
        if (folder.isSystem) return
        viewModelScope.launch {
            repository.update(folder)
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        if (folder.isSystem) return
        viewModelScope.launch {
            val alarms = database.alarmDao().getAlarmsByFolderList(folder.id)
            alarms.forEach(scheduler::cancel)
            database.withTransaction {
                if (settings.authToken == null) {
                    database.alarmDao().deleteAll(alarms)
                } else {
                    val deletedAt = System.currentTimeMillis()
                    alarms.forEach { alarm ->
                        database.alarmDao().update(
                            alarm.copy(isEnabled = false, isDeleted = true, updatedAt = deletedAt)
                        )
                    }
                }
                repository.delete(folder)
            }
            if (settings.authToken != null) syncHelper.triggerSync()
        }
    }

    fun duplicateFolder(folder: FolderEntity) {
        if (folder.isSystem) return
        viewModelScope.launch {
            val folderCount = repository.getUserFolderCount()
            if (!FeatureFlags.canCreateFolder(folderCount)) {
                _errorMessage.emit(S.folderLimitReached)
                return@launch
            }

            val sourceAlarms = database.alarmDao().getAlarmsByFolderList(folder.id)
            val totalAlarmCount = database.alarmDao().getTotalAlarmCount()
            if (!FeatureFlags.isPremium && totalAlarmCount + sourceAlarms.size > FeatureFlags.FREE_ALARM_LIMIT) {
                _errorMessage.emit(S.duplicateFolderAlarmLimit)
                return@launch
            }

            var candidateName = S.folderCopyName(folder.name)
            var suffix = 2
            while (repository.findByName(candidateName) != null) {
                candidateName = S.folderCopyName(folder.name, suffix++)
            }

            database.withTransaction {
                val newFolderId = repository.insert(
                    folder.copy(id = 0, name = candidateName, isEnabled = false)
                )
                val copy = ScheduleGroupDuplicator.copy(
                    source = folder,
                    alarms = sourceAlarms,
                    copyName = candidateName,
                    newFolderId = newFolderId,
                )
                copy.alarms.forEach { alarm ->
                    database.alarmDao().insert(alarm)
                }
            }
            if (settings.authToken != null) syncHelper.triggerSync()
        }
    }

    fun toggleFolder(folderId: Long) {
        viewModelScope.launch {
            val folder = repository.getFolderById(folderId) ?: return@launch
            val groupEnabled = !folder.isEnabled
            repository.setEnabled(folderId, groupEnabled)
            database.alarmDao().setFolderAlarmsEnabled(folderId, groupEnabled)
            ScheduleGroupPlanner.plan(
                alarms = database.alarmDao().getAlarmsByFolderList(folderId),
                groupEnabled = groupEnabled
            ).forEach { command ->
                when (command) {
                    is ScheduleCommand.Schedule -> scheduler.schedule(command.alarm)
                    is ScheduleCommand.Cancel -> scheduler.cancel(command.alarm)
                }
            }
        }
    }

    suspend fun findByName(name: String): FolderEntity? = repository.findByName(name)
}
