package com.nexalarm.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nexalarm.app.ui.theme.PrimaryBlue

data class ScheduleGroupIconOption(
    val id: String,
    val label: String,
    val icon: ImageVector
)

val scheduleGroupIconOptions = listOf(
    ScheduleGroupIconOption("briefcase", "工作", Icons.Default.BusinessCenter),
    ScheduleGroupIconOption("book", "課業", Icons.AutoMirrored.Filled.MenuBook),
    ScheduleGroupIconOption("calendar", "行程", Icons.Default.CalendarMonth),
    ScheduleGroupIconOption("bedtime", "睡眠", Icons.Default.Bedtime),
    ScheduleGroupIconOption("fitness", "運動", Icons.Default.FitnessCenter),
    ScheduleGroupIconOption("flight", "旅行", Icons.Default.FlightTakeoff),
    ScheduleGroupIconOption("home", "家庭", Icons.Default.Home),
    ScheduleGroupIconOption("cafe", "休息", Icons.Default.LocalCafe),
    ScheduleGroupIconOption("music", "音樂", Icons.Default.MusicNote),
    ScheduleGroupIconOption("bolt", "重要", Icons.Default.Bolt)
)

private val legacyIconIds = mapOf(
    "💼" to "briefcase",
    "📘" to "book",
    "📚" to "book",
    "🎉" to "calendar",
    "🏋️" to "fitness",
    "🌙" to "bedtime",
    "☕" to "cafe",
    "🎵" to "music",
    "🏠" to "home",
    "✈️" to "flight",
    "🌿" to "calendar",
    "⚡" to "bolt"
)

private val knownIconIds = scheduleGroupIconOptions.map { it.id }.toSet()

fun scheduleGroupIconId(savedValue: String): String = when {
    savedValue in knownIconIds -> savedValue
    else -> legacyIconIds[savedValue] ?: "calendar"
}

fun scheduleGroupIconOption(savedValue: String): ScheduleGroupIconOption =
    scheduleGroupIconOptions.first { it.id == scheduleGroupIconId(savedValue) }

@Composable
fun ScheduleGroupIcon(
    savedValue: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = PrimaryBlue
) {
    Icon(
        imageVector = scheduleGroupIconOption(savedValue).icon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
