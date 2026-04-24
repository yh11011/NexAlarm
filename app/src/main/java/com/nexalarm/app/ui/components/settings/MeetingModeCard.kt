package com.nexalarm.app.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.ui.theme.*

/**
 * 會議模式設定卡片
 * 開啟後所有鬧鐘只震動，不響鈴
 */
@Composable
fun MeetingModeCard(settingsManager: SettingsManager) {
    var meetingMode by remember { mutableStateOf(settingsManager.isMeetingMode) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .nexGlassSurface(16.dp)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(S.meetingMode, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(S.meetingModeDesc, fontSize = 12.sp, color = TextSecondary)
            }
            Switch(
                checked = meetingMode,
                onCheckedChange = {
                    meetingMode = it
                    settingsManager.isMeetingMode = it
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextOnPrimary,
                    checkedTrackColor = PrimaryBlue,
                    uncheckedThumbColor = TextSecondary.copy(alpha = 0.5f),
                    uncheckedTrackColor = TextSecondary.copy(alpha = 0.2f)
                )
            )
        }
    }
}
