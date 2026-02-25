package com.nexalarm.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.nexalarm.app.data.model.AlarmEntity

@Composable
fun rememberCountdownText(alarms: List<AlarmEntity>): String {
    return remember(alarms) {
        val now = java.util.Calendar.getInstance()
        val nowMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val enabled = alarms.filter { it.isEnabled }
        if (enabled.isEmpty()) return@remember ""

        val nextMinutes = enabled.minOf { a ->
            val alarmMin = a.hour * 60 + a.minute
            val diff = alarmMin - nowMinutes
            if (diff <= 0) diff + 1440 else diff
        }
        val h = nextMinutes / 60
        val m = nextMinutes % 60
        buildString {
            if (h > 0) append("${h} 小時 ")
            append("${m} 分鐘後響鈴")
        }
    }
}

