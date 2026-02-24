package com.nexalarm.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarm: AlarmEntity?,
    folders: List<FolderEntity>,
    onSave: (AlarmEntity) -> Unit,
    onBack: () -> Unit
) {
    val isEditing = alarm != null

    // Use key(alarm) so state reinitializes when alarm data changes (e.g. loaded from DB)
    key(alarm) {
        AlarmEditContent(alarm = alarm, isEditing = isEditing, folders = folders, onSave = onSave, onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditContent(
    alarm: AlarmEntity?,
    isEditing: Boolean,
    folders: List<FolderEntity>,
    onSave: (AlarmEntity) -> Unit,
    onBack: () -> Unit
) {

    var hour by remember { mutableIntStateOf(alarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(alarm?.minute ?: 0) }
    var title by remember { mutableStateOf(alarm?.title ?: "") }
    var isRecurring by remember { mutableStateOf(alarm?.isRecurring ?: false) }
    var repeatDays by remember { mutableStateOf(alarm?.repeatDays ?: emptyList()) }
    var selectedFolderId by remember { mutableStateOf(alarm?.folderId) }
    var vibrateOnly by remember { mutableStateOf(alarm?.vibrateOnly ?: false) }
    var volume by remember { mutableFloatStateOf((alarm?.volume ?: 80).toFloat()) }
    var snoozeDelay by remember { mutableFloatStateOf((alarm?.snoozeDelay ?: 5).toFloat()) }
    var maxSnoozeCount by remember { mutableFloatStateOf((alarm?.maxSnoozeCount ?: 3).toFloat()) }
    var keepAfterRinging by remember { mutableStateOf(alarm?.keepAfterRinging ?: false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Alarm" else "New Alarm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val result = AlarmEntity(
                            id = alarm?.id ?: 0,
                            hour = hour,
                            minute = minute,
                            title = title,
                            isEnabled = true,
                            isRecurring = isRecurring,
                            repeatDays = if (isRecurring) repeatDays else emptyList(),
                            folderId = selectedFolderId,
                            vibrateOnly = vibrateOnly,
                            volume = volume.toInt(),
                            snoozeDelay = snoozeDelay.toInt(),
                            maxSnoozeCount = maxSnoozeCount.toInt(),
                            keepAfterRinging = keepAfterRinging,
                            createdAt = alarm?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(result)
                    }) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time picker card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = { showTimePicker = true }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Alarm Title (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
            )

            // Folder selection
            if (folders.isNotEmpty()) {
                var folderExpanded by remember { mutableStateOf(false) }
                val selectedFolder = folders.find { it.id == selectedFolderId }

                ExposedDropdownMenuBox(
                    expanded = folderExpanded,
                    onExpandedChange = { folderExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedFolder?.name ?: "No folder",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Folder") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderExpanded) },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                    )
                    ExposedDropdownMenu(
                        expanded = folderExpanded,
                        onDismissRequest = { folderExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No folder") },
                            onClick = {
                                selectedFolderId = null
                                folderExpanded = false
                            }
                        )
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = {
                                    selectedFolderId = folder.id
                                    folderExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Recurring toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Recurring", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Repeat on selected days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
            }

            // Day selector
            if (isRecurring) {
                val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dayLabels.forEachIndexed { index, label ->
                        val day = index + 1
                        FilterChip(
                            selected = day in repeatDays,
                            onClick = {
                                repeatDays = if (day in repeatDays) {
                                    repeatDays - day
                                } else {
                                    repeatDays + day
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Vibrate only
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Vibrate Only", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "No sound, vibration only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = vibrateOnly, onCheckedChange = { vibrateOnly = it })
            }

            // Volume
            if (!vibrateOnly) {
                Column {
                    Text(
                        "Volume: ${volume.toInt()}%",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0f..100f,
                        steps = 9
                    )
                }
            }

            HorizontalDivider()

            // Snooze settings
            Text("Snooze Settings", style = MaterialTheme.typography.titleSmall)

            Column {
                Text(
                    "Snooze delay: ${snoozeDelay.toInt()} min",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = snoozeDelay,
                    onValueChange = { snoozeDelay = it },
                    valueRange = 5f..10f,
                    steps = 4
                )
            }

            Column {
                Text(
                    "Max snooze count: ${maxSnoozeCount.toInt()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = maxSnoozeCount,
                    onValueChange = { maxSnoozeCount = it },
                    valueRange = 1f..5f,
                    steps = 3
                )
            }

            HorizontalDivider()

            // Keep after ringing (one-time alarm)
            if (!isRecurring) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Keep After Ringing", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Don't auto-delete one-time alarm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(checked = keepAfterRinging, onCheckedChange = { keepAfterRinging = it })
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val dialogTimePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = dialogTimePickerState.hour
                    minute = dialogTimePickerState.minute
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = dialogTimePickerState)
            }
        )
    }
}
