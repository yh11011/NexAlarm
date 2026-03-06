package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.ui.components.AlarmCard
import com.nexalarm.app.ui.components.rememberCountdownText
import com.nexalarm.app.ui.theme.*

@Composable
fun AlarmScreen(
    alarms: List<AlarmEntity>,
    folders: List<FolderEntity>,
    onAddClick: () -> Unit,
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = openMenu,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = S.menu,
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = S.alarm,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) PrimaryBlue else DarkSurface)
                            .clickable { selectedTab = index }
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) TextPrimary else TextSecondary
                        )
                    }
                    if (index < tabs.lastIndex) {
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
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
            if (filteredAlarms.isEmpty()) {
                val (emoji, title) = when (selectedTab) {
                    0 -> "🔔" to S.noSingleAlarms
                    else -> "🔁" to S.noRepeatAlarms
                }
                EmptyState(emoji = emoji, title = title, subtitle = S.tapPlusToAdd)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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

        // FAB
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = PrimaryBlue,
            shape = CircleShape
        ) {
            Text("+", fontSize = 24.sp, color = TextPrimary)
        }
    }
}
