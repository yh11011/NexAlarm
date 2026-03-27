package com.nexalarm.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "alarms")
@TypeConverters(RepeatDaysConverter::class)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val title: String = "",
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = false,
    val repeatDays: List<Int> = emptyList(),
    val folderId: Long? = null,
    val vibrateOnly: Boolean = false,
    val volume: Int = 80,
    val ringtoneUri: String = "",
    val snoozeDelay: Int = 10,
    val maxSnoozeCount: Int = 3,
    val keepAfterRinging: Boolean = false,
    val snoozeEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    // 雲端同步欄位（v6 新增）
    val clientId: String = java.util.UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis()
)

class RepeatDaysConverter {
    @TypeConverter
    fun fromList(days: List<Int>): String = days.joinToString(",")

    @TypeConverter
    fun toList(data: String): List<Int> =
        if (data.isBlank()) emptyList() else data.split(",").mapNotNull { it.trim().toIntOrNull() }
}
