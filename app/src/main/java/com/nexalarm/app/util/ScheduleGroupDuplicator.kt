package com.nexalarm.app.util

import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity

data class ScheduleGroupCopy(
    val folder: FolderEntity,
    val alarms: List<AlarmEntity>,
)

object ScheduleGroupDuplicator {
    fun copy(
        source: FolderEntity,
        alarms: List<AlarmEntity>,
        copyName: String,
        newFolderId: Long,
        now: Long = System.currentTimeMillis(),
    ): ScheduleGroupCopy = ScheduleGroupCopy(
        folder = source.copy(id = newFolderId, name = copyName, isEnabled = false),
        alarms = alarms.map { alarm ->
            alarm.copy(
                id = 0,
                folderId = newFolderId,
                isEnabled = false,
                clientId = java.util.UUID.randomUUID().toString(),
                createdAt = now,
                updatedAt = now,
                isDeleted = false,
            )
        },
    )
}
