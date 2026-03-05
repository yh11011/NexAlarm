package com.nexalarm.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    onAlarmToggle: (AlarmEntity) -> Unit
) {
    if (folder == null) return

    val folderAlarms = alarms.filter { it.folderId == folder.id }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 頂部導航列
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "返回",
                        tint = TextPrimary, modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        folder.emoji + "  " + folder.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        folderAlarms.size.toString() + " 個鬧鐘",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                NexToggle(
                    checked = folder.isEnabled,
                    onCheckedChange = { onToggleFolder() }
                )
            }

            // 鬧鐘列表
            if (folderAlarms.isEmpty()) {
                EmptyState(emoji = "🕐", title = "此資料夾尚無鬧鐘", subtitle = "點擊 + 新增")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(folderAlarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            folder = null,
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
            onClick = onAddAlarm,
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
