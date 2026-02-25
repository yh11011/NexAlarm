package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.viewmodel.StopwatchViewModel

@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel = viewModel()
) {
    val elapsedMs by viewModel.elapsedMs.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val laps by viewModel.laps.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp)
        ) {
            Text(
                text = "碼錶",
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary,
                letterSpacing = (-0.3).sp
            )
        }

        // Big time display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatStopwatch(elapsedMs),
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                letterSpacing = (-3).sp
            )
        }

        // Control buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lap button
            CircleButton(
                text = "圈次",
                enabled = isRunning,
                isPrimary = false,
                onClick = { viewModel.lap() }
            )

            Spacer(modifier = Modifier.width(20.dp))

            // Play/Pause button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
                    .clickable { viewModel.toggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "⏸" else "▶",
                    fontSize = 22.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Reset button
            CircleButton(
                text = "重置",
                enabled = !isRunning && elapsedMs > 0,
                isPrimary = false,
                onClick = { viewModel.reset() }
            )
        }

        // Lap list
        if (laps.isNotEmpty()) {
            val minLap = if (laps.size > 2) laps.min() else null
            val maxLap = if (laps.size > 2) laps.max() else null

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                itemsIndexed(laps) { index, lap ->
                    val lapColor = when {
                        laps.size > 2 && lap == minLap -> LapFast
                        laps.size > 2 && lap == maxLap -> LapSlow
                        else -> TextPrimary
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "圈 ${laps.size - index}",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = formatStopwatch(lap),
                            fontSize = 14.sp,
                            color = lapColor
                        )
                    }
                    if (index < laps.lastIndex) {
                        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleButton(
    text: String,
    enabled: Boolean,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (isPrimary) PrimaryBlue else DarkSurface)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .let { if (!enabled) it.then(Modifier) else it },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) TextPrimary else TextPrimary.copy(alpha = 0.3f)
        )
    }
}

private fun formatStopwatch(ms: Long): String {
    val minutes = (ms / 60000).toInt()
    val seconds = ((ms % 60000) / 1000).toInt()
    val centis = ((ms % 1000) / 10).toInt()
    return "%02d:%02d.%02d".format(minutes, seconds, centis)
}

