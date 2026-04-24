package com.nexalarm.app.util

import com.nexalarm.app.data.model.AlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AlarmTriggerCalculatorTest {

    private val taipeiTimeZone = TimeZone.getTimeZone("Asia/Taipei")

    @Test
    fun `single alarm stays on same day when time is still ahead`() {
        val now = dateTime(2026, 4, 9, 8, 15)
        val alarm = alarm(hour = 9, minute = 30)

        val trigger = AlarmTriggerCalculator.calculateNextTriggerTime(alarm, now, taipeiTimeZone)

        assertEquals(dateTime(2026, 4, 9, 9, 30), trigger)
    }

    @Test
    fun `single alarm rolls to next day when time has passed`() {
        val now = dateTime(2026, 4, 9, 22, 5)
        val alarm = alarm(hour = 21, minute = 45)

        val trigger = AlarmTriggerCalculator.calculateNextTriggerTime(alarm, now, taipeiTimeZone)

        assertEquals(dateTime(2026, 4, 10, 21, 45), trigger)
    }

    @Test
    fun `recurring alarm uses today when weekday matches and time is ahead`() {
        val now = dateTime(2026, 4, 9, 8, 0)
        val alarm = alarm(
            hour = 8,
            minute = 30,
            isRecurring = true,
            repeatDays = listOf(4)
        )

        val trigger = AlarmTriggerCalculator.calculateNextTriggerTime(alarm, now, taipeiTimeZone)

        assertEquals(dateTime(2026, 4, 9, 8, 30), trigger)
    }

    @Test
    fun `recurring alarm skips to next matching week when todays slot has passed`() {
        val now = dateTime(2026, 4, 9, 8, 31)
        val alarm = alarm(
            hour = 8,
            minute = 30,
            isRecurring = true,
            repeatDays = listOf(4)
        )

        val trigger = AlarmTriggerCalculator.calculateNextTriggerTime(alarm, now, taipeiTimeZone)

        assertEquals(dateTime(2026, 4, 16, 8, 30), trigger)
    }

    @Test
    fun `recurring alarm correctly wraps from sunday to monday`() {
        val now = dateTime(2026, 4, 12, 23, 0)
        val alarm = alarm(
            hour = 7,
            minute = 45,
            isRecurring = true,
            repeatDays = listOf(1)
        )

        val trigger = AlarmTriggerCalculator.calculateNextTriggerTime(alarm, now, taipeiTimeZone)

        assertEquals(dateTime(2026, 4, 13, 7, 45), trigger)
    }

    private fun alarm(
        hour: Int,
        minute: Int,
        isRecurring: Boolean = false,
        repeatDays: List<Int> = emptyList()
    ): AlarmEntity {
        return AlarmEntity(
            id = 1,
            hour = hour,
            minute = minute,
            title = "Test",
            isEnabled = true,
            isRecurring = isRecurring,
            repeatDays = repeatDays
        )
    }

    private fun dateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(taipeiTimeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
