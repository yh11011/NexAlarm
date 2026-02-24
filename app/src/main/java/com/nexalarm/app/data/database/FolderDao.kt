package com.nexalarm.app.data.database

import androidx.room.*
import com.nexalarm.app.data.model.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY isSystem DESC, name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): FolderEntity?

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Delete
    suspend fun delete(folder: FolderEntity)

    @Query("UPDATE folders SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM folders WHERE isSystem = 0")
    suspend fun getUserFolderCount(): Int
}
