package com.nexalarm.app.util

enum class ReliabilityLevel {
    READY,
    NEEDS_ATTENTION
}

data class ReliabilityState(
    val level: ReliabilityLevel,
    val readyCount: Int,
    val totalCount: Int,
    val blockingKeys: List<String>
) {
    val scoreText: String get() = "$readyCount/$totalCount"
}

object AlarmReliabilityAdvisor {
    fun evaluate(
        notificationEnabled: Boolean,
        exactAlarmReady: Boolean,
        fullScreenReady: Boolean,
        batteryUnrestricted: Boolean,
        bootRescheduleReady: Boolean
    ): ReliabilityState {
        val checks = listOf(
            "notifications" to notificationEnabled,
            "exact_alarm" to exactAlarmReady,
            "full_screen" to fullScreenReady,
            "battery" to batteryUnrestricted,
            "boot_reschedule" to bootRescheduleReady
        )
        val blockingKeys = checks.filterNot { it.second }.map { it.first }
        return ReliabilityState(
            level = if (blockingKeys.isEmpty()) ReliabilityLevel.READY else ReliabilityLevel.NEEDS_ATTENTION,
            readyCount = checks.size - blockingKeys.size,
            totalCount = checks.size,
            blockingKeys = blockingKeys
        )
    }
}
