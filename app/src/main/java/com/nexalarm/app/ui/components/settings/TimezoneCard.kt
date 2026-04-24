package com.nexalarm.app.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.ui.theme.*
import java.util.TimeZone

/**
 * 時區設定卡片
 * 顯示目前選擇的時區，點擊開啟選擇對話框
 */
@Composable
fun TimezoneCard(
    currentTimezoneId: String?,
    onClick: () -> Unit
) {
    val displayText = if (currentTimezoneId == null) {
        S.timezoneSystem
    } else {
        formatTimezoneDisplay(currentTimezoneId)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .nexGlassSurface(16.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(S.timezone, fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayText,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 格式化 UTC 偏移量顯示
 */
fun formatUtcOffset(tzId: String): String {
    val tz = TimeZone.getTimeZone(tzId)
    val offsetMs = tz.rawOffset
    val sign = if (offsetMs >= 0) "+" else "-"
    val absMs = Math.abs(offsetMs)
    val hours = absMs / 3_600_000
    val minutes = (absMs % 3_600_000) / 60_000
    return "UTC${sign}%02d:%02d".format(hours, minutes)
}

/**
 * 格式化時區顯示文字
 */
fun formatTimezoneDisplay(tzId: String): String {
    return "${formatUtcOffset(tzId)} $tzId"
}
