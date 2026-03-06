package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
    val openMenu = LocalMenuAction.current
    val elapsedMs by viewModel.elapsedMs.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val laps by viewModel.laps.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp)
        ) {
            IconButton(
                onClick = openMenu,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = S.menu,
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = S.stopwatch,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Big time display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (laps.isEmpty()) Modifier.weight(1f)
                    else Modifier.padding(vertical = 28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatStopwatch(elapsedMs),
                fontSize = if (laps.isEmpty()) 80.sp else 72.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                letterSpacing = (-3).sp
            )
        }

        // Lap list
        if (laps.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(laps) { index, lap ->
                    val lapNumber = laps.size - index
                    val cumulative = laps.subList(index, laps.size).sum()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "%02d".format(lapNumber),
                            fontSize = 16.sp,
                            color = TextSecondary,
                            modifier = Modifier.width(56.dp)
                        )
                        Text(
                            text = "+ ${formatStopwatch(lap)}",
                            fontSize = 16.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatStopwatch(cumulative),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Control buttons at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 56.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left button: Lap (running) or Stop/Reset (paused with time)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .then(
                        when {
                            isRunning -> Modifier.clickable { viewModel.lap() }
                            elapsedMs > 0 -> Modifier.clickable { viewModel.reset() }
                            else -> Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val enabled = isRunning || elapsedMs > 0
                if (isRunning || elapsedMs == 0L) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = S.lap,
                        tint = if (enabled) PrimaryBlue else TextPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = S.reset,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Right button: Pause (running) or Play (paused)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .clickable { viewModel.toggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) S.pause else S.start,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private fun formatStopwatch(ms: Long): String {
    val minutes = (ms / 60000).toInt()
    val seconds = ((ms % 60000) / 1000).toInt()
    val centis = ((ms % 1000) / 10).toInt()
    return "%02d:%02d.%02d".format(minutes, seconds, centis)
}