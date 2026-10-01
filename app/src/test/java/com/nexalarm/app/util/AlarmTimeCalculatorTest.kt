package com.nexalarm.app.util

import com.nexalarm.app.data.model.AlarmEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class AlarmTimeCalculatorTest {
    @Test fun datedAlarmUsesEachPhonesLocalTime() {
        val alarm = AlarmEntity(hour = 7, minute = 0, scheduledDate = "2030-01-02")
        val now = Instant.parse("2030-01-01T00:00:00Z")
        assertEquals(Instant.parse("2030-01-01T23:00:00Z").toEpochMilli(), AlarmTimeCalculator.nextTrigger(alarm, now, ZoneId.of("Asia/Taipei")))
        assertEquals(Instant.parse("2030-01-02T12:00:00Z").toEpochMilli(), AlarmTimeCalculator.nextTrigger(alarm, now, ZoneId.of("America/New_York")))
    }
    @Test fun pastDateDoesNotRollIntoTomorrow() {
        val alarm = AlarmEntity(hour = 7, minute = 0, scheduledDate = "2020-01-01")
        val now = Instant.parse("2030-01-01T00:00:00Z")
        assertTrue(AlarmTimeCalculator.nextTrigger(alarm, now, ZoneId.of("UTC")) < now.toEpochMilli())
    }
    @Test fun weeklyScheduleSkipsUnselectedDays() {
        val alarm = AlarmEntity(hour = 7, minute = 0, isRecurring = true, repeatDays = listOf(1))
        assertEquals(Instant.parse("2026-10-05T07:00:00Z").toEpochMilli(), AlarmTimeCalculator.nextTrigger(alarm, Instant.parse("2026-09-29T08:00:00Z"), ZoneId.of("UTC")))
    }
    @Test fun nextOccurrenceRollsForwardWithoutDate() {
        assertEquals(Instant.parse("2030-01-02T07:00:00Z").toEpochMilli(), AlarmTimeCalculator.nextTrigger(AlarmEntity(hour=7,minute=0), Instant.parse("2030-01-01T08:00:00Z"), ZoneId.of("UTC")))
    }
    @Test fun dstGapShiftsForwardAndOverlapChoosesFirst() {
        val zone = ZoneId.of("America/New_York")
        assertEquals(Instant.parse("2026-03-08T07:30:00Z").toEpochMilli(), AlarmTimeCalculator.nextTrigger(AlarmEntity(hour=2,minute=30,scheduledDate="2026-03-08"), Instant.EPOCH, zone))
        assertEquals(Instant.parse("2026-11-01T05:30:00Z").toEpochMilli(), AlarmTimeCalculator.nextTrigger(AlarmEntity(hour=1,minute=30,scheduledDate="2026-11-01"), Instant.EPOCH, zone))
    }
}
