package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
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
    onDuplicateFolder: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onFolderClick: (FolderEntity) -> Unit,
    showAddDialog: Boolean,
    onAddDialogDismiss: () -> Unit,
    onAddFolderClick: () -> Unit
) {
    val openMenu = LocalMenuAction.current
    val userFolders = folders.filter { !it.isSystem }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var folderForActions by remember { mutableStateOf<FolderEntity?>(null) }

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
                    onLongClick = { folderForActions = folder },
                    onClick = { onFolderClick(folder) }
                )
            }

            // Add folder button (dashed border)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nexGlassSurface(NexCornerRadius.card)
                        .border(
                            width = 1.5.dp,
                            color = DarkBorder,
                            shape = RoundedCornerShape(NexCornerRadius.card)
                        )
                        .clip(RoundedCornerShape(NexCornerRadius.card))
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

    folderForActions?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderForActions = null },
            title = { Text(S.folderActions) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            folderForActions = null
                            onDuplicateFolder(folder)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(S.duplicateFolder)
                    }
                    TextButton(
                        onClick = {
                            folderForActions = null
                            folderToDelete = folder
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = DangerRed)
                        Spacer(Modifier.width(8.dp))
                        Text(S.deleteFolder, color = DangerRed)
                    }
                }
            },
            confirmButton = {},
        )
    }

    folderToDelete?.let { folder ->
        val alarmCount = alarmCountMap[folder.id] ?: 0
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text(S.deleteFolder) },
            text = { Text(S.deleteFolderMessage(folder.name, alarmCount)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFolder(folder)
                        folderToDelete = null
                    }
                ) {
                    Text(S.deleteFolder, color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text(S.cancel)
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FolderListCard(
    folder: FolderEntity,
    alarmCount: Int,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .nexGlassSurface(NexCornerRadius.card, elevated = folder.isEnabled)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
                    .background(AccentDim, RoundedCornerShape(NexCornerRadius.compact)),
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

