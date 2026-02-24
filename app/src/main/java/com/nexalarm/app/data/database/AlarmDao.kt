package com.nexalarm.app.data.database

import androidx.room.*
import com.nexalarm.app.data.model.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY hour, minute")
    fun getEnabledAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getEnabledAlarmsList(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE folderId = :folderId ORDER BY hour, minute")
    fun getAlarmsByFolder(folderId: Long): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE hour = :hour AND minute = :minute AND title = :title AND folderId = :folderId AND repeatDays = :repeatDays LIMIT 1")
    suspend fun findDuplicate(hour: Int, minute: Int, title: String, folderId: Long?, repeatDays: String): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE alarms SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE alarms SET vibrateOnly = :vibrateOnly WHERE id = :id")
    suspend fun setVibrateOnly(id: Long, vibrateOnly: Boolean)

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND (isRecurring = 0 OR repeatDays LIKE '%' || :dayOfWeek || '%')")
    suspend fun getTodayAlarms(dayOfWeek: Int): List<AlarmEntity>
}
