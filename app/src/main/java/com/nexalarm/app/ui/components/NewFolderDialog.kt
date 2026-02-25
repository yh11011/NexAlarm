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

val FOLDER_EMOJIS = listOf(
    "📘", "💼", "🎉", "🏋️", "🌙", "☕",
    "🎵", "🏠", "✈️", "📚", "🌿", "⚡"
)

@Composable
fun NewFolderDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit
) {
    if (!visible) return

    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf(FOLDER_EMOJIS[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(310.dp)
                .background(DarkCard, RoundedCornerShape(22.dp))
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "新增資料夾",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Emoji grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(100.dp)
            ) {
                items(FOLDER_EMOJIS) { emoji ->
                    val isSelected = emoji == selectedEmoji
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                            .clickable { selectedEmoji = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text("資料夾名稱", color = TextTertiary)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
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
                    shape = RoundedCornerShape(12.dp),
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
                            onConfirm(name.trim(), selectedEmoji)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("建立", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

