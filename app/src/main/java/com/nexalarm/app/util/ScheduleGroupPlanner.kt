package com.nexalarm.app.util

import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity

/** A schedule group is the single source of truth for all member alarms. */
sealed interface ScheduleCommand {
    data class Schedule(val alarm: AlarmEntity) : ScheduleCommand
    data class Cancel(val alarm: AlarmEntity) : ScheduleCommand
}

object ScheduleGroupPlanner {
    fun plan(alarms: List<AlarmEntity>, groupEnabled: Boolean): List<ScheduleCommand> =
        if (groupEnabled) {
            alarms.map { alarm -> ScheduleCommand.Schedule(alarm.copy(isEnabled = true)) }
        } else {
            alarms.map(ScheduleCommand::Cancel)
        }

    fun activeAlarms(
        alarms: List<AlarmEntity>,
        groups: List<FolderEntity>
    ): List<AlarmEntity> {
        val groupState = groups.associate { it.id to it.isEnabled }
        return alarms.filter { alarm ->
            if (alarm.folderId == null) {
                alarm.isEnabled
            } else {
                groupState[alarm.folderId] ?: false
            }
        }
    }
}
