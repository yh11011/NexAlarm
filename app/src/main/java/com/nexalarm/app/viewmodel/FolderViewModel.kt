package com.nexalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.data.repository.FolderRepository
import com.nexalarm.app.util.FeatureFlags
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FolderRepository
    val allFolders: StateFlow<List<FolderEntity>>

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage

    init {
        val db = NexAlarmDatabase.getDatabase(application)
        repository = FolderRepository(db.folderDao())

        allFolders = repository.getAllFolders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addFolder(name: String, color: String = "#2196F3") {
        viewModelScope.launch {
            val count = repository.getUserFolderCount()
            if (!FeatureFlags.canCreateFolder(count)) {
                _errorMessage.emit("Free tier limited to ${FeatureFlags.FREE_FOLDER_LIMIT} folders. Upgrade to premium for unlimited.")
                return@launch
            }
            repository.insert(FolderEntity(name = name, color = color))
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
            repository.setEnabled(folderId, !folder.isEnabled)
        }
    }

    suspend fun findByName(name: String): FolderEntity? = repository.findByName(name)
}
