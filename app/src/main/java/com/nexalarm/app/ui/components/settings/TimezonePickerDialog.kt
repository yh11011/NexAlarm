package com.nexalarm.app.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.ui.theme.PrimaryBlue
import com.nexalarm.app.ui.theme.S
import com.nexalarm.app.ui.theme.TextPrimary
import com.nexalarm.app.ui.theme.TextSecondary
import java.util.TimeZone

/**
 * 時區選擇對話框
 * 提供搜尋和選擇世界各地的時區
 */
@Composable
fun TimezonePickerDialog(
    currentTimezoneId: String?,
    onSelect: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val allTimezones = remember {
        TimeZone.getAvailableIDs()
            .map { it }
            .sortedWith(compareBy(
                { TimeZone.getTimeZone(it).rawOffset },
                { it }
            ))
    }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) allTimezones
        else allTimezones.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.nexalarm.app.ui.theme.DarkSurface,
        title = {
            Text(S.timezoneSelect, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(S.timezoneSearch, color = TextSecondary, fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Reset to system option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onReset)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(S.timezoneReset, color = PrimaryBlue, fontSize = 14.sp)
                    if (currentTimezoneId == null) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(filtered) { tzId ->
                        val isSelected = tzId == currentTimezoneId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { onSelect(tzId) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tzId,
                                    fontSize = 14.sp,
                                    color = if (isSelected) PrimaryBlue else TextPrimary
                                )
                                Text(
                                    text = formatUtcOffset(tzId),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(S.cancel, color = TextSecondary)
            }
        }
    )
}
