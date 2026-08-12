package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.ui.components.AlarmCard
import com.nexalarm.app.ui.components.NexTopBar
import com.nexalarm.app.ui.components.rememberCountdownText
import com.nexalarm.app.ui.theme.*

@Composable
fun AlarmScreen(
    alarms: List<AlarmEntity>,
    folders: List<FolderEntity>,
    onAlarmClick: (AlarmEntity) -> Unit,
    onAlarmToggle: (AlarmEntity) -> Unit
) {
    val openMenu = LocalMenuAction.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(S.single, S.repeat)

    val filteredAlarms = when (selectedTab) {
        0 -> alarms.filter { !it.isRecurring }
        1 -> alarms.filter { it.isRecurring }
        else -> alarms
    }
    val countdown = rememberCountdownText(filteredAlarms)

    Column(modifier = Modifier.fillMaxSize()) {
            NexTopBar(title = S.alarm, onMenuClick = openMenu)

            // Tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .nexGlassSurface(24.dp)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) PrimaryBlue else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(horizontal = 20.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) TextOnPrimary else TextSecondary
                        )
                    }
                }
            }

            // Countdown
            AlarmSummaryPanel(
                countdown = countdown,
                alarmCount = filteredAlarms.size,
                enabledCount = filteredAlarms.count { it.isEnabled }
            )

            // Alarm list
            if (filteredAlarms.isEmpty()) {
                val (emoji, title) = when (selectedTab) {
                    0 -> "🔔" to S.noSingleAlarms
                    else -> "🔁" to S.noRepeatAlarms
                }
                EmptyState(emoji = emoji, title = title, subtitle = S.tapPlusToAdd)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredAlarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            folder = folders.find { it.id == alarm.folderId },
                            onClick = { onAlarmClick(alarm) },
                            onToggle = { onAlarmToggle(alarm) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(90.dp)) }
                }
            }
        }
}

@Composable
private fun AlarmSummaryPanel(
    countdown: String,
    alarmCount: Int,
    enabledCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .nexGlassSurface(24.dp, elevated = true)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = S.nextAlarmShort,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Text(
                    text = countdown.ifEmpty { S.homeNoActiveAlarm },
                    fontSize = if (countdown.isEmpty()) 20.sp else 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (countdown.isEmpty()) TextSecondary else TextPrimary,
                    lineHeight = 30.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetric(value = alarmCount.toString(), label = S.alarm)
                SummaryMetric(value = enabledCount.toString(), label = S.enabledShort)
            }
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}
