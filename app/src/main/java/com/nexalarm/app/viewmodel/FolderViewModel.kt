package com.nexalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.data.repository.FolderRepository
import com.nexalarm.app.ui.theme.S
import com.nexalarm.app.util.AlarmScheduler
import com.nexalarm.app.util.FeatureFlags
import com.nexalarm.app.util.ScheduleCommand
import com.nexalarm.app.util.ScheduleGroupPlanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NexAlarmDatabase.getDatabase(application)
    private val repository: FolderRepository
    private val scheduler = AlarmScheduler(application)
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
            repository.delete(folder)
        }
    }

    fun toggleFolder(folderId: Long) {
        viewModelScope.launch {
            val folder = repository.getFolderById(folderId) ?: return@launch
            val groupEnabled = !folder.isEnabled
            repository.setEnabled(folderId, groupEnabled)
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
