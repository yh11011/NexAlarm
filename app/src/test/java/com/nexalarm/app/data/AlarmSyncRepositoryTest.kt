package com.nexalarm.app.data

import com.nexalarm.app.data.model.AlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSyncRepositoryTest {

    @Test
    fun `alarmToJson includes ringtoneUri`() {
        val alarm = AlarmEntity(
            id = 1,
            hour = 7,
            minute = 30,
            title = "Morning",
            ringtoneUri = "content://media/internal/audio/media/42"
        )

        val json = AlarmSyncRepository.alarmToJson(alarm)

        assertEquals("content://media/internal/audio/media/42", json.getString("ringtoneUri"))
    }

    @Test
    fun `jsonToAlarm restores ringtoneUri and nullable folderId`() {
        val json = AlarmSyncRepository.alarmToJson(
            AlarmEntity(
                id = 2,
                hour = 22,
                minute = 15,
                title = "Sleep",
                folderId = null,
                ringtoneUri = "__silent__"
            )
        )

        val alarm = AlarmSyncRepository.jsonToAlarm(
            json = json,
            clientId = "client-1",
            updatedAt = 1234L
        )

        assertEquals("__silent__", alarm.ringtoneUri)
        assertEquals(null, alarm.folderId)
    }

    @Test
    fun `jsonToAlarm falls back when ringtoneUri is absent`() {
        val json = AlarmSyncRepository.alarmToJson(
            AlarmEntity(
                id = 3,
                hour = 6,
                minute = 0,
                title = "Run"
            )
        ).apply {
            remove("ringtoneUri")
        }

        val alarm = AlarmSyncRepository.jsonToAlarm(
            json = json,
            clientId = "client-2",
            updatedAt = 5678L
        )

        assertTrue(alarm.ringtoneUri.isEmpty())
    }
}
