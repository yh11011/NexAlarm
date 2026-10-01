package com.nexalarm.app.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.util.AlarmScheduler
import com.nexalarm.app.ui.theme.isAppEnglish
import kotlinx.coroutines.delay

@Composable
fun rememberCountdownText(alarms: List<AlarmEntity>): String {
    val context = LocalContext.current
    val scheduler = remember(context) { AlarmScheduler(context) }
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000L); tick++ }
    }
    val english = isAppEnglish
    return remember(alarms, tick, english) {
        val now = System.currentTimeMillis()
        val next = alarms.filter { it.isEnabled }.filter { scheduler.getNextTriggerTime(it) > now }
            .minByOrNull { scheduler.getNextTriggerTime(it) }
        if (next == null) "" else scheduler.getTimeUntilText(next, english)
    }
}
