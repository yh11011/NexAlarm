package com.nexalarm.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    alarms: List<AlarmEntity>,
    folders: List<FolderEntity>,
    selectedFolderId: Long?,
    onFolderSelected: (Long?) -> Unit,
    onAddAlarmClick: () -> Unit,
    onAlarmClick: (AlarmEntity) -> Unit,
    onAlarmToggle: (AlarmEntity) -> Unit,
    onAlarmDelete: (AlarmEntity) -> Unit,
    onFolderManageClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nex Alarm",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onFolderManageClick) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Manage Folders")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlarmClick,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Folder filter row
            if (folders.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFolderId == null,
                            onClick = { onFolderSelected(null) },
                            label = { Text("All") },
                            leadingIcon = if (selectedFolderId == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    items(folders) { folder ->
                        val folderColor = try {
                            Color(android.graphics.Color.parseColor(folder.color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        FilterChip(
                            selected = selectedFolderId == folder.id,
                            onClick = { onFolderSelected(folder.id) },
                            label = { Text(folder.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(folderColor)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = folderColor.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }

            // Warning card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4A2800)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFAB40),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Alarms will ring even in silent/DND mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFAB40)
                    )
                }
            }

            // Alarms list
            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AlarmAdd,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No alarms yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            "Tap + to create your first alarm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            folder = folders.find { it.id == alarm.folderId },
                            onClick = { onAlarmClick(alarm) },
                            onToggle = { onAlarmToggle(alarm) },
                            onDelete = { onAlarmDelete(alarm) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmCard(
    alarm: AlarmEntity,
    folder: FolderEntity?,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (alarm.isEnabled)
            MaterialTheme.colorScheme.surfaceVariant
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "cardBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )

                if (alarm.title.isNotBlank()) {
                    Text(
                        text = alarm.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (alarm.isRecurring && alarm.repeatDays.isNotEmpty()) {
                        Text(
                            text = formatRepeatDays(alarm.repeatDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = "One-time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    if (alarm.vibrateOnly) {
                        Icon(
                            Icons.Default.Vibration,
                            contentDescription = "Vibrate only",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    if (folder != null) {
                        val fColor = try {
                            Color(android.graphics.Color.parseColor(folder.color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(fColor)
                        )
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

fun formatRepeatDays(days: List<Int>): String {
    if (days.sorted() == listOf(1, 2, 3, 4, 5)) return "Weekdays"
    if (days.sorted() == listOf(6, 7)) return "Weekends"
    if (days.sorted() == listOf(1, 2, 3, 4, 5, 6, 7)) return "Every day"
    return days.sorted().joinToString(" ") { DAY_LABELS.getOrElse(it - 1) { "?" } }
}

