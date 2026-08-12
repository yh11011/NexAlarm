package com.nexalarm.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.ReliabilityLevel
import com.nexalarm.app.util.ReliabilityState

@Composable
fun AlarmReliabilityCard(
    state: ReliabilityState,
    modifier: Modifier = Modifier,
    showDetails: Boolean = false,
    onTestAlarm: (() -> Unit)? = null
) {
    val ready = state.level == ReliabilityLevel.READY
    val accent = if (ready) PrimaryBlue else DangerRed

    Column(
        modifier = modifier
            .fillMaxWidth()
            .nexGlassSurface(20.dp, elevated = true)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (ready) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = S.reliabilityTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Text(
                    text = if (ready) S.reliabilityReady else S.reliabilityNeedsAttention,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
            ) {
                Text(
                    text = state.scoreText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
        }

        Text(
            text = if (ready) S.reliabilityReadyDesc else S.reliabilityNeedsAttentionDesc,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        if (showDetails) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReliabilityCheckRow(S.reliabilityNotifications, "notifications", state)
                ReliabilityCheckRow(S.reliabilityExactAlarm, "exact_alarm", state)
                ReliabilityCheckRow(S.reliabilityFullScreen, "full_screen", state)
                ReliabilityCheckRow(S.reliabilityBattery, "battery", state)
                ReliabilityCheckRow(S.reliabilityBoot, "boot_reschedule", state)
            }
        }

        if (onTestAlarm != null) {
            if (ready) {
                Button(
                    onClick = onTestAlarm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = TextOnPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(S.testAlarm, fontWeight = FontWeight.SemiBold)
                }
            } else {
                OutlinedButton(
                    onClick = onTestAlarm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(S.testAlarm, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ReliabilityCheckRow(label: String, key: String, state: ReliabilityState) {
    val ok = key !in state.blockingKeys
    val tint = if (ok) PrimaryBlue else DangerRed
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = TextPrimary
        )
        Text(
            text = if (ok) S.reliabilityOk else S.reliabilityCheck,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}
