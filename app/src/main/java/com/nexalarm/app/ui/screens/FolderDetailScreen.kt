package com.nexalarm.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.nexalarm.app.ui.components.NexBackTopBar
import com.nexalarm.app.ui.components.NexToggle
import com.nexalarm.app.ui.theme.*

@Composable
fun FolderDetailScreen(
    folder: FolderEntity?,
    alarms: List<AlarmEntity>,
    onBack: () -> Unit,
    onToggleFolder: () -> Unit,
    onAddAlarm: () -> Unit,
    onAlarmClick: (AlarmEntity) -> Unit,
) {
    if (folder == null) return

    val folderAlarms = alarms.filter { it.folderId == folder.id }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NexBackTopBar(
                title = folder.name,
                subtitle = S.alarmCount(folderAlarms.size) + if (folder.isEnabled) " · ON" else " · OFF",
                onBack = onBack,
                trailing = {
                    NexToggle(
                        checked = folder.isEnabled,
                        onCheckedChange = { onToggleFolder() }
                    )
                }
            )

            // 鬧鐘列表
            if (folderAlarms.isEmpty()) {
                EmptyState(emoji = "🕐", title = S.emptyFolder, subtitle = S.tapPlusToAdd)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(folderAlarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            folder = null,
                            onClick = { onAlarmClick(alarm) },
                            onToggle = {},
                            showToggle = false,
                        )
                    }
                    item { Spacer(modifier = Modifier.height(90.dp)) }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onAddAlarm,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = PrimaryBlue,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = S.newAlarm, tint = TextOnPrimary)
        }
    }

}
