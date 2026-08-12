package com.nexalarm.app.util

data class TestAlarmPlan(
    val triggerAtMs: Long,
    val title: String,
    val volume: Int,
    val snoozeDelayMinutes: Int
)

object TestAlarmPlanner {
    private const val TEST_DELAY_MS = 30_000L

    fun createPlan(nowMs: Long = System.currentTimeMillis()): TestAlarmPlan {
        return TestAlarmPlan(
            triggerAtMs = nowMs + TEST_DELAY_MS,
            title = "NexAlarm test",
            volume = 60,
            snoozeDelayMinutes = 10
        )
    }
}
