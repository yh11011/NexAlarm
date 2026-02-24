package com.nexalarm.app.data.repository

import com.nexalarm.app.data.database.AlarmDao
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.RepeatDaysConverter
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao) {

    fun getAllAlarms(): Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    fun getEnabledAlarms(): Flow<List<AlarmEntity>> = alarmDao.getEnabledAlarms()

    suspend fun getEnabledAlarmsList(): List<AlarmEntity> = alarmDao.getEnabledAlarmsList()

    fun getAlarmsByFolder(folderId: Long): Flow<List<AlarmEntity>> =
        alarmDao.getAlarmsByFolder(folderId)

    suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

    suspend fun insertOrUpdate(alarm: AlarmEntity): Long {
        val converter = RepeatDaysConverter()
        val existing = alarmDao.findDuplicate(
            alarm.hour, alarm.minute, alarm.title,
            alarm.folderId, converter.fromList(alarm.repeatDays)
        )
        return if (existing != null) {
            val updated = alarm.copy(id = existing.id)
            alarmDao.update(updated)
            existing.id
        } else {
            alarmDao.insert(alarm)
        }
    }

    suspend fun insert(alarm: AlarmEntity): Long = alarmDao.insert(alarm)

    suspend fun update(alarm: AlarmEntity) = alarmDao.update(alarm)

    suspend fun delete(alarm: AlarmEntity) = alarmDao.delete(alarm)

    suspend fun deleteById(id: Long) = alarmDao.deleteById(id)

    suspend fun setEnabled(id: Long, enabled: Boolean) = alarmDao.setEnabled(id, enabled)

    suspend fun setVibrateOnly(id: Long, vibrateOnly: Boolean) =
        alarmDao.setVibrateOnly(id, vibrateOnly)

    suspend fun getTodayAlarms(dayOfWeek: Int): List<AlarmEntity> =
        alarmDao.getTodayAlarms(dayOfWeek)
}
