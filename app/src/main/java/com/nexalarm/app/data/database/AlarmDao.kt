package com.nexalarm.app.data.database

import androidx.room.*
import com.nexalarm.app.data.model.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms WHERE is_deleted = 0 ORDER BY hour, minute")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND is_deleted = 0 ORDER BY hour, minute")
    fun getEnabledAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND is_deleted = 0")
    suspend fun getEnabledAlarmsList(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE folderId = :folderId AND is_deleted = 0 ORDER BY hour, minute")
    fun getAlarmsByFolder(folderId: Long): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE clientId = :clientId LIMIT 1")
    suspend fun getByClientId(clientId: String): AlarmEntity?

    @Query("SELECT * FROM alarms")
    suspend fun getAllAlarmsList(): List<AlarmEntity>

    // 顯式處理 folderId null：SQLite 中 NULL = NULL 為 FALSE，必須用 IS NULL 比對
    @Query("SELECT * FROM alarms WHERE hour = :hour AND minute = :minute AND title = :title AND ((:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId) AND repeatDays = :repeatDays AND is_deleted = 0 LIMIT 1")
    suspend fun findDuplicate(hour: Int, minute: Int, title: String, folderId: Long?, repeatDays: String): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAll(alarms: List<AlarmEntity>)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE alarms SET isEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE alarms SET vibrateOnly = :vibrateOnly WHERE id = :id")
    suspend fun setVibrateOnly(id: Long, vibrateOnly: Boolean)

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND is_deleted = 0 AND (isRecurring = 0 OR repeatDays LIKE '%' || :dayOfWeek || '%')")
    suspend fun getTodayAlarms(dayOfWeek: Int): List<AlarmEntity>

    @Query("SELECT COUNT(*) FROM alarms WHERE folderId = :folderId AND is_deleted = 0")
    suspend fun getAlarmCountByFolder(folderId: Long): Int

    @Query("SELECT COUNT(*) FROM alarms WHERE is_deleted = 0")
    suspend fun getTotalAlarmCount(): Int

    // 同一資料夾內是否已有相同時間的鬧鐘（排除自身）
    // 顯式處理 folderId null：Room 的 IS + bound parameter 行為依驅動版本不穩定
    @Query("SELECT * FROM alarms WHERE hour = :hour AND minute = :minute AND id != :excludeId AND ((:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId) LIMIT 1")
    suspend fun findTimeConflict(hour: Int, minute: Int, folderId: Long?, excludeId: Long): AlarmEntity?
}
