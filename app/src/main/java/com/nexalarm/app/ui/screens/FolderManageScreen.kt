package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.ui.components.NexToggle
import com.nexalarm.app.ui.components.NewFolderDialog
import com.nexalarm.app.ui.theme.*

@Composable
fun FolderManageScreen(
    folders: List<FolderEntity>,
    alarmCountMap: Map<Long, Int>,
    onAddFolder: (String, String, String) -> Unit,
    onToggleFolder: (Long) -> Unit,
    onFolderClick: (FolderEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp)
        ) {
            Text(
                text = "資料夾",
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary,
                letterSpacing = (-0.3).sp
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(folders) { folder ->
                val count = alarmCountMap[folder.id] ?: 0
                FolderListCard(
                    folder = folder,
                    alarmCount = count,
                    onToggle = { onToggleFolder(folder.id) },
                    onClick = { onFolderClick(folder) }
                )
            }

            // Add folder button (dashed border)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showAddDialog = true }
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AccentDim, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("＋", fontSize = 22.sp, color = PrimaryBlue)
                        }
                        Text(
                            text = "新增資料夾",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryBlue
                        )
                    }
                }
            }

            // Quota display
            item {
                val userCount = folders.count { !it.isSystem }
                Text(
                    text = "免費版：已用 $userCount / 10 個資料夾",
                    fontSize = 12.sp,
                    color = TextTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }

    NewFolderDialog(
        visible = showAddDialog,
        onDismiss = { showAddDialog = false },
        onConfirm = { name, emoji ->
            onAddFolder(name, "#1A73E8", emoji)
            showAddDialog = false
        }
    )
}

@Composable
private fun FolderListCard(
    folder: FolderEntity,
    alarmCount: Int,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Emoji
            Text(
                text = folder.emoji,
                fontSize = 26.sp,
                lineHeight = 26.sp
            )

            // Name & count
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$alarmCount 個鬧鐘",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            // Chevron
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = TextTertiary
            )

            // Toggle
            NexToggle(
                checked = folder.isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

