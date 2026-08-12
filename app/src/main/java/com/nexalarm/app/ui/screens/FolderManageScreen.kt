package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
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
import com.nexalarm.app.ui.components.NexIconBadge
import com.nexalarm.app.ui.components.NexMetricCard
import com.nexalarm.app.ui.components.NexToggle
import com.nexalarm.app.ui.components.NexTopBar
import com.nexalarm.app.ui.components.NewFolderDialog
import com.nexalarm.app.ui.components.ScheduleGroupIcon
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.FeatureFlags

@Composable
fun FolderManageScreen(
    folders: List<FolderEntity>,
    alarmCountMap: Map<Long, Int>,
    onAddFolder: (String, String, String) -> Unit,
    onToggleFolder: (Long) -> Unit,
    onFolderClick: (FolderEntity) -> Unit,
    showAddDialog: Boolean,
    onAddDialogDismiss: () -> Unit,
    onAddFolderClick: () -> Unit
) {
    val openMenu = LocalMenuAction.current
    val userFolders = folders.filter { !it.isSystem }

    Column(modifier = Modifier.fillMaxSize()) {
        NexTopBar(title = S.folders, onMenuClick = openMenu)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NexMetricCard(
                value = userFolders.size.toString(),
                label = S.customFolders,
                modifier = Modifier.weight(1f),
                elevated = true
            )
            NexMetricCard(
                value = "${userFolders.size}/${FeatureFlags.FREE_FOLDER_LIMIT}",
                label = S.freePlan,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(userFolders) { folder ->
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
                        .nexGlassSurface(20.dp)
                        .border(
                            width = 1.5.dp,
                            color = DarkBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAddFolderClick() }
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        NexIconBadge(
                            icon = Icons.Default.Add,
                            contentDescription = S.newFolder,
                            selected = false
                        )
                        Text(
                            text = S.newFolder,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryBlue
                        )
                    }
                }
            }

            // Quota display
            item {
                Text(
                    text = S.folderQuota(userFolders.size, FeatureFlags.FREE_FOLDER_LIMIT),
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
        onDismiss = onAddDialogDismiss,
        onConfirm = { name, emoji ->
            onAddFolder(name, "#1A73E8", emoji)
            onAddDialogDismiss()
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
            .nexGlassSurface(20.dp, elevated = folder.isEnabled)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(AccentDim, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                ScheduleGroupIcon(
                    savedValue = folder.emoji,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp)
                )
            }

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
                    text = S.alarmCount(alarmCount),
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

