package com.nexalarm.app.util

import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity

/** Keeps a group switch from overwriting an alarm's personal enabled choice. */
sealed interface ScheduleCommand {
    data class Schedule(val alarm: AlarmEntity) : ScheduleCommand
    data class Cancel(val alarm: AlarmEntity) : ScheduleCommand
}

object ScheduleGroupPlanner {
    fun plan(alarms: List<AlarmEntity>, groupEnabled: Boolean): List<ScheduleCommand> =
        if (groupEnabled) {
            alarms.filter { it.isEnabled }.map(ScheduleCommand::Schedule)
        } else {
            alarms.map(ScheduleCommand::Cancel)
        }

    fun activeAlarms(
        alarms: List<AlarmEntity>,
        groups: List<FolderEntity>
    ): List<AlarmEntity> {
        val groupState = groups.associate { it.id to it.isEnabled }
        return alarms.filter { alarm ->
            alarm.isEnabled && (alarm.folderId == null || groupState[alarm.folderId] ?: true)
        }
    }
}
