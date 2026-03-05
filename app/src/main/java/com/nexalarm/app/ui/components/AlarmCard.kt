package com.nexalarm.app.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.ui.theme.*
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
            .background(DarkSurface, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    color = if (alarm.isEnabled) TextPrimary else TextSecondary,
                    letterSpacing = (-2).sp,
                    lineHeight = 52.sp
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = alarm.title.ifBlank { "鬧鐘" },
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    if (folder != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .background(AccentDim, RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 1.dp)
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
                        text = " · " + formatRepeatDays(alarm),
                        fontSize = 13.sp,
                        color = TextSecondary
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
    if (days.isEmpty()) return "單次"
    if (days.sorted() == listOf(1, 2, 3, 4, 5, 6, 7)) return "每天"
    if (days.sorted() == listOf(1, 2, 3, 4, 5)) return "平日"
    if (days.sorted() == listOf(6, 7)) return "週末"
    val labels = listOf("", "週一", "週二", "週三", "週四", "週五", "週六", "週日")
    return days.sorted().joinToString("、") { labels.getOrElse(it) { "?" } }
}
