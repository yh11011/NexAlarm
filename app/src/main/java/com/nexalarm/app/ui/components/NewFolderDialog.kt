package com.nexalarm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import com.nexalarm.app.ui.theme.*

@Composable
fun NewFolderDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit
) {
    if (!visible) return

    var name by remember { mutableStateOf("") }
    var selectedIconId by remember { mutableStateOf("briefcase") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(310.dp)
                .nexGlassSurface(22.dp, elevated = true)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = S.newFolder,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Professional schedule categories, stored as stable IDs rather than emoji glyphs.
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(116.dp)
            ) {
                items(scheduleGroupIconOptions) { option ->
                    val isSelected = option.id == selectedIconId
                    Box(
                        modifier = Modifier
                            .height(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) AccentDim else DarkSurface
                            )
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    PrimaryBlue,
                                    RoundedCornerShape(10.dp)
                                ) else Modifier
                            )
                            .clickable { selectedIconId = option.id },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ScheduleGroupIcon(
                                savedValue = option.id,
                                contentDescription = option.label,
                                modifier = Modifier.size(19.dp),
                                tint = if (isSelected) PrimaryBlue else TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(option.label, fontSize = 10.sp, color = if (isSelected) PrimaryBlue else TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text(S.folderLabel, color = TextTertiary)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
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

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = TextPrimary
                    )
                ) {
                    Text("取消", fontSize = 15.sp)
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name.trim(), selectedIconId)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = TextOnPrimary
                    )
                ) {
                    Text("建立", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

