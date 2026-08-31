package com.nexalarm.app.util

import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScheduleGroupDuplicatorTest {
    @Test
    fun `copy duplicates every member into a disabled independent group`() {
        val source = FolderEntity(id = 7, name = "Work", isEnabled = true)
        val alarms = listOf(
            AlarmEntity(id = 11, hour = 8, minute = 0, folderId = 7, clientId = "first"),
            AlarmEntity(id = 12, hour = 9, minute = 30, folderId = 7, clientId = "second"),
        )

        val result = ScheduleGroupDuplicator.copy(source, alarms, "Work copy", 8, now = 1234)

        assertEquals(8L, result.folder.id)
        assertEquals("Work copy", result.folder.name)
        assertFalse(result.folder.isEnabled)
        assertEquals(2, result.alarms.size)
        result.alarms.forEachIndexed { index, alarm ->
            assertEquals(0L, alarm.id)
            assertEquals(8L, alarm.folderId)
            assertFalse(alarm.isEnabled)
            assertEquals(1234, alarm.createdAt)
            assertEquals(1234, alarm.updatedAt)
            assertNotEquals(alarms[index].clientId, alarm.clientId)
        }
    }
}
