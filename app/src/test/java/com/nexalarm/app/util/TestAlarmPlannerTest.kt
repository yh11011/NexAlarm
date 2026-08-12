package com.nexalarm.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TestAlarmPlannerTest {

    @Test
    fun `test alarm is scheduled thirty seconds from now with safe defaults`() {
        val nowMs = dateTime(2026, 6, 8, 21, 30, 0)

        val plan = TestAlarmPlanner.createPlan(nowMs)

        assertEquals(nowMs + 30_000L, plan.triggerAtMs)
        assertEquals("NexAlarm test", plan.title)
        assertEquals(60, plan.volume)
        assertEquals(10, plan.snoozeDelayMinutes)
    }

    @Test
    fun `test alarm keeps exact thirty second trigger across minute boundary`() {
        val nowMs = dateTime(2026, 6, 8, 21, 30, 45)

        val plan = TestAlarmPlanner.createPlan(nowMs)

        assertEquals(dateTime(2026, 6, 8, 21, 31, 15), plan.triggerAtMs)
    }

    private fun dateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Taipei")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
