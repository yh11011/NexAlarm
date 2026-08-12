package com.nexalarm.app.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.ui.theme.*
import java.util.Locale
@Composable
fun AlarmCard(
    alarm: AlarmEntity,
    folder: FolderEntity?,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .nexGlassSurface(20.dp, elevated = alarm.isEnabled)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (alarm.isEnabled) TextPrimary else TextSecondary,
                    letterSpacing = 0.sp,
                    lineHeight = 52.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = alarm.title.ifBlank { S.alarmDefaultTitle },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (folder != null) {
                        Box(
                            modifier = Modifier
                                .background(AccentDim, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = folder.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SecondaryBlue
                            )
                        }
                    }
                    Text(
                        text = formatRepeatDays(alarm),
                        fontSize = 13.sp,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }
            }
            NexToggle(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
fun formatRepeatDays(alarm: AlarmEntity): String {
    val days = alarm.repeatDays
    if (days.isEmpty()) return S.once
    val sortedDays = days.sorted()
    if (sortedDays == listOf(1, 2, 3, 4, 5, 6, 7)) return S.everyDay
    if (sortedDays == listOf(1, 2, 3, 4, 5)) return S.weekdays
    if (sortedDays == listOf(6, 7)) return S.weekend
    val separator = if (isAppEnglish) ", " else "、"
    return sortedDays.joinToString(separator) { S.weekdayName(it) }
}
