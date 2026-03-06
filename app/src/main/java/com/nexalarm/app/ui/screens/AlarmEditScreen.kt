package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.nexalarm.app.ui.components.NexToggle
import com.nexalarm.app.ui.components.WheelPicker
import com.nexalarm.app.ui.theme.*

@Composable
fun AlarmEditScreen(
    alarm: AlarmEntity?,
    folders: List<FolderEntity>,
    defaultFolderId: Long? = null,
    onSave: (AlarmEntity) -> Unit,
    onBack: () -> Unit,
    onDelete: ((AlarmEntity) -> Unit)? = null
) {
    val isEditing = alarm != null
    key(alarm) { AlarmEditContent(alarm, isEditing, folders, defaultFolderId, onSave, onBack, onDelete) }
}

@Composable
private fun AlarmEditContent(
    alarm: AlarmEntity?,
    isEditing: Boolean,
    folders: List<FolderEntity>,
    defaultFolderId: Long?,
    onSave: (AlarmEntity) -> Unit,
    onBack: () -> Unit,
    onDelete: ((AlarmEntity) -> Unit)?
) {
    var hour by remember { mutableIntStateOf(alarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(alarm?.minute ?: 0) }
    var title by remember { mutableStateOf(alarm?.title ?: "") }
    var isRecurring by remember { mutableStateOf(alarm?.isRecurring ?: false) }
    var repeatDays by remember { mutableStateOf(alarm?.repeatDays ?: emptyList()) }
    var selectedFolderId by remember { mutableStateOf(alarm?.folderId ?: defaultFolderId) }
    var vibrateOnly by remember { mutableStateOf(alarm?.vibrateOnly ?: false) }
    var snoozeEnabled by remember { mutableStateOf(true) }
    var snoozeDelay by remember { mutableIntStateOf(alarm?.snoozeDelay ?: 5) }
    var showFolderPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
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
                Text(
                    if (isEditing) "編輯鬧鐘" else "新增鬧鐘",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    onSave(
                        AlarmEntity(
                            id = alarm?.id ?: 0,
                            hour = hour,
                            minute = minute,
                            title = title,
                            isEnabled = true,
                            isRecurring = isRecurring,
                            repeatDays = if (isRecurring) repeatDays else emptyList(),
                            folderId = selectedFolderId,
                            vibrateOnly = vibrateOnly,
                            volume = alarm?.volume ?: 80,
                            snoozeDelay = snoozeDelay,
                            maxSnoozeCount = alarm?.maxSnoozeCount ?: 3,
                            keepAfterRinging = alarm?.keepAfterRinging ?: false,
                            createdAt = alarm?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                }) {
                    Text("儲存", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 時間選擇區（滾輪）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(DarkSurface, RoundedCornerShape(18.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WheelPicker(
                            items = (0..23).toList(),
                            selectedItem = hour,
                            onItemSelected = { hour = it },
                            label = "小時"
                        )
                        Text(
                            text = ":",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Light,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        WheelPicker(
                            items = (0..59).toList(),
                            selectedItem = minute,
                            onItemSelected = { minute = it },
                            label = "分鐘"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 標題輸入
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("鬧鐘標題（選填）", color = TextTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = DarkSurface,
                        focusedContainerColor = DarkSurface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = PrimaryBlue,
                        cursorColor = PrimaryBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 資料夾（與重複互斥）
                val repeatDisabled = selectedFolderId != null
                val folderDisabled = !repeatDisabled && repeatDays.isNotEmpty()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(DarkSurface, RoundedCornerShape(18.dp))
                        .then(if (!folderDisabled) Modifier.clickable { showFolderPicker = !showFolderPicker } else Modifier)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("資料夾", fontSize = 15.sp, color = if (folderDisabled) TextTertiary else TextPrimary)
                        Text(
                            (folders.find { it.id == selectedFolderId }?.name ?: "無") + " >",
                            fontSize = 14.sp,
                            color = if (folderDisabled) TextTertiary else TextSecondary
                        )
                    }
                }
                if (showFolderPicker && !folderDisabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(DarkCard, RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedFolderId = null; showFolderPicker = false }
                                .padding(vertical = 10.dp)
                        ) {
                            Text("無", fontSize = 14.sp, color = TextSecondary)
                        }
                        folders.forEach { f ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedFolderId = f.id; showFolderPicker = false }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    f.emoji + " " + f.name,
                                    fontSize = 14.sp,
                                    color = if (selectedFolderId == f.id) SecondaryBlue else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 重複日選擇
                EditLabel("重複日")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { i, lbl ->
                        val day = i + 1
                        val sel = day in repeatDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel && !repeatDisabled) AccentDim else DarkSurface)
                                .then(if (!repeatDisabled) Modifier.clickable {
                                    repeatDays = if (day in repeatDays) repeatDays - day else repeatDays + day
                                    isRecurring = repeatDays.isNotEmpty()
                                } else Modifier)
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                lbl, fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    repeatDisabled -> TextTertiary
                                    sel -> SecondaryBlue
                                    else -> TextSecondary
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 設定區塊
                EditLabel("設定")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(DarkSurface, RoundedCornerShape(18.dp))
                ) {
                    EditToggleRow("貪睡", "延後響鈴", snoozeEnabled) { snoozeEnabled = it }
                    EditDiv()
                    EditRow("貪睡間隔", snoozeDelay.toString() + " 分鐘", true) {
                        snoozeDelay = if (snoozeDelay >= 10) 5 else snoozeDelay + 1
                    }
                    EditDiv()
                    EditToggleRow("僅震動", "靜音時仍震動", vibrateOnly) { vibrateOnly = it }
                }

                // 刪除按鈕
                if (isEditing && onDelete != null && alarm != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(DangerRed.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .clickable { onDelete(alarm) }
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("刪除鬧鐘", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

    }
}

@Composable
private fun EditLabel(text: String) {
    Text(
        text, fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.09.sp,
        color = TextTertiary,
        modifier = Modifier.padding(start = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun EditRow(
    label: String,
    value: String,
    showArrow: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = TextPrimary)
        Text(if (showArrow) "$value >" else value, fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun EditToggleRow(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 15.sp, color = TextPrimary)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
        NexToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EditDiv() {
    HorizontalDivider(color = DarkBorder, thickness = 1.dp)
}
