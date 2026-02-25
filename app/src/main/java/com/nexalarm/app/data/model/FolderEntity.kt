package com.nexalarm.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val color: String = "#1A73E8",
    val isSystem: Boolean = false,
    val emoji: String = "📁"
)
