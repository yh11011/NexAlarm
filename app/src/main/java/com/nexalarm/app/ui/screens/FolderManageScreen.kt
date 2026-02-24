package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexalarm.app.data.model.FolderEntity

private val PRESET_COLORS = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
    "#009688", "#4CAF50", "#8BC34A", "#FF9800",
    "#FF5722", "#795548", "#607D8B"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderManageScreen(
    folders: List<FolderEntity>,
    onAddFolder: (String, String) -> Unit,
    onUpdateFolder: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onToggleFolder: (Long) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<FolderEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Folders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(folders) { folder ->
                FolderCard(
                    folder = folder,
                    onToggle = { onToggleFolder(folder.id) },
                    onEdit = { editingFolder = folder },
                    onDelete = { onDeleteFolder(folder) }
                )
            }
        }
    }

    // Add folder dialog
    if (showAddDialog) {
        FolderDialog(
            title = "New Folder",
            initialName = "",
            initialColor = "#2196F3",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                onAddFolder(name, color)
                showAddDialog = false
            }
        )
    }

    // Edit folder dialog
    editingFolder?.let { folder ->
        FolderDialog(
            title = "Edit Folder",
            initialName = folder.name,
            initialColor = folder.color,
            onDismiss = { editingFolder = null },
            onConfirm = { name, color ->
                onUpdateFolder(folder.copy(name = name, color = color))
                editingFolder = null
            }
        )
    }
}

@Composable
fun FolderCard(
    folder: FolderEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val folderColor = try {
        Color(android.graphics.Color.parseColor(folder.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(folderColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (folder.isSystem) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("System", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            if (!folder.isSystem) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Switch(
                checked = folder.isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
fun FolderDialog(
    title: String,
    initialName: String,
    initialColor: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Color", style = MaterialTheme.typography.labelMedium)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PRESET_COLORS) { color ->
                        val c = try {
                            Color(android.graphics.Color.parseColor(color))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
