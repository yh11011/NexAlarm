package com.nexalarm.app.util

import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleGroupPlannerTest {

    @Test
    fun `disabling a schedule group cancels every member without changing its personal enabled setting`() {
        val alarms = listOf(
            alarm(id = 1, isEnabled = true),
            alarm(id = 2, isEnabled = false),
            alarm(id = 3, isEnabled = true)
        )

        val commands = ScheduleGroupPlanner.plan(alarms, groupEnabled = false)

        assertEquals(
            listOf(
                ScheduleCommand.Cancel(alarms[0]),
                ScheduleCommand.Cancel(alarms[1]),
                ScheduleCommand.Cancel(alarms[2])
            ),
            commands
        )
        assertEquals(listOf(true, false, true), alarms.map { it.isEnabled })
    }

    @Test
    fun `enabling a schedule group schedules only members that are personally enabled`() {
        val active = alarm(id = 1, isEnabled = true)
        val paused = alarm(id = 2, isEnabled = false)

        val commands = ScheduleGroupPlanner.plan(listOf(active, paused), groupEnabled = true)

        assertEquals(listOf(ScheduleCommand.Schedule(active)), commands)
    }

    @Test
    fun `next alarm candidates exclude personally enabled alarms in disabled schedule groups`() {
        val standalone = AlarmEntity(id = 1, hour = 6, minute = 0, title = "Gym", isEnabled = true)
        val disabledGroupAlarm = alarm(id = 2, isEnabled = true)
        val activeGroupAlarm = alarm(id = 3, isEnabled = true).copy(folderId = 100)
        val groups = listOf(
            FolderEntity(id = 99, name = "Night shift", isEnabled = false),
            FolderEntity(id = 100, name = "Day shift", isEnabled = true)
        )

        val candidates = ScheduleGroupPlanner.activeAlarms(
            alarms = listOf(standalone, disabledGroupAlarm, activeGroupAlarm),
            groups = groups
        )

        assertEquals(listOf(standalone, activeGroupAlarm), candidates)
    }

    private fun alarm(id: Long, isEnabled: Boolean) = AlarmEntity(
        id = id,
        hour = 7,
        minute = 30,
        title = "Shift $id",
        isEnabled = isEnabled,
        folderId = 99
    )
}
