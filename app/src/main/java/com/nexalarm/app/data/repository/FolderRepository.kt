package com.nexalarm.app.data.repository

import com.nexalarm.app.data.database.FolderDao
import com.nexalarm.app.data.model.FolderEntity
import kotlinx.coroutines.flow.Flow

class FolderRepository(private val folderDao: FolderDao) {

    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()

    suspend fun getFolderById(id: Long): FolderEntity? = folderDao.getFolderById(id)

    suspend fun findByName(name: String): FolderEntity? = folderDao.findByName(name)

    suspend fun insert(folder: FolderEntity): Long = folderDao.insert(folder)

    suspend fun update(folder: FolderEntity) = folderDao.update(folder)

    suspend fun delete(folder: FolderEntity) = folderDao.delete(folder)

    suspend fun setEnabled(id: Long, enabled: Boolean) = folderDao.setEnabled(id, enabled)

    suspend fun getUserFolderCount(): Int = folderDao.getUserFolderCount()
}
