package com.nexalarm.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmReliabilityAdvisorTest {

    @Test
    fun `all checks passing reports ready state`() {
        val state = AlarmReliabilityAdvisor.evaluate(
            notificationEnabled = true,
            exactAlarmReady = true,
            fullScreenReady = true,
            batteryUnrestricted = true,
            bootRescheduleReady = true
        )

        assertEquals(ReliabilityLevel.READY, state.level)
        assertEquals(5, state.readyCount)
        assertEquals(5, state.totalCount)
        assertEquals("5/5", state.scoreText)
    }

    @Test
    fun `missing exact alarm and notification permission reports attention state`() {
        val state = AlarmReliabilityAdvisor.evaluate(
            notificationEnabled = false,
            exactAlarmReady = false,
            fullScreenReady = true,
            batteryUnrestricted = true,
            bootRescheduleReady = true
        )

        assertEquals(ReliabilityLevel.NEEDS_ATTENTION, state.level)
        assertEquals(3, state.readyCount)
        assertEquals(listOf("notifications", "exact_alarm"), state.blockingKeys)
    }
}
