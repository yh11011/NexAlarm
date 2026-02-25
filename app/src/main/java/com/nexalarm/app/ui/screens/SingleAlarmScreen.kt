package com.nexalarm.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.ui.components.AlarmCard
import com.nexalarm.app.ui.components.rememberCountdownText
import com.nexalarm.app.ui.theme.*

@Composable
fun SingleAlarmScreen(
    alarms: List<AlarmEntity>,
    folders: List<FolderEntity>,
    onAddClick: () -> Unit,
    onAlarmClick: (AlarmEntity) -> Unit,
    onAlarmToggle: (AlarmEntity) -> Unit
) {
    val singleAlarms = alarms.filter { !it.isRecurring }
    val countdown = rememberCountdownText(singleAlarms)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "單次",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextPrimary,
                    letterSpacing = (-0.3).sp
                )
            }

            // Countdown
            if (countdown.isNotEmpty()) {
                Text(
                    text = countdown,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextPrimary,
                    letterSpacing = (-0.2).sp,
                    modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 26.dp)
                )
            }

            // Alarm list
            if (singleAlarms.isEmpty()) {
                EmptyState(emoji = "🔔", title = "尚無單次鬧鐘", subtitle = "點擊 + 新增")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(singleAlarms, key = { it.id }) { alarm ->
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

        // FAB
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = PrimaryBlue,
            shape = androidx.compose.foundation.shape.CircleShape
        ) {
            Text("+", fontSize = 24.sp, color = TextPrimary)
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = emoji,
                fontSize = 52.sp,
                modifier = Modifier.padding(bottom = 14.dp),
                color = TextPrimary.copy(alpha = 0.1f)
            )
            Text(
                text = title,
                fontSize = 15.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextTertiary
            )
        }
    }
}

