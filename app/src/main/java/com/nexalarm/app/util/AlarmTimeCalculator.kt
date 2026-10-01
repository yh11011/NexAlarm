package com.nexalarm.app.util

import com.nexalarm.app.data.model.AlarmEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Wall-clock schedules use each phone's zone. atZone shifts DST gaps forward and chooses the first overlap. */
object AlarmTimeCalculator {
    fun nextTrigger(alarm: AlarmEntity, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Long {
        val time = LocalTime.of(alarm.hour, alarm.minute)
        alarm.scheduledDate?.let {
            return LocalDate.parse(it).atTime(time).atZone(zone).toInstant().toEpochMilli()
        }
        val today = now.atZone(zone).toLocalDate()
        for (offset in 0L..7L) {
            val date = today.plusDays(offset)
            if (alarm.isRecurring && alarm.repeatDays.isNotEmpty() && date.dayOfWeek.value !in alarm.repeatDays) continue
            val candidate = date.atTime(time).atZone(zone).toInstant()
            if (candidate > now) return candidate.toEpochMilli()
        }
        error("No valid next occurrence")
    }
}
