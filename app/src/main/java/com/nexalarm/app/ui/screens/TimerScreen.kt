package com.nexalarm.app.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexalarm.app.ui.components.WheelPicker
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.viewmodel.TimerViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = viewModel()
) {
    val openMenu = LocalMenuAction.current
    val totalSec by viewModel.totalSeconds.collectAsState()
    val remainingSec by viewModel.remainingSeconds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()

    val progress = if (totalSec > 0) remainingSec.toFloat() / totalSec.toFloat() else 1f
    val tickActiveColor = if (isFinished) LapFast else PrimaryBlue
    val tickInactiveColor = DarkSurface

    val isSetup = !isRunning && remainingSec == totalSec && !isFinished
    var showEditDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(isFinished) {
        if (isFinished) {
            playTimerFinishAlert(context)
        }
    }

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
                text = S.timer,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Center content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Circular timer with tick marks
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .drawBehind {
                            val tickCount = 120
                            val tickLength = 14.dp.toPx()
                            val tickWidth = 2.dp.toPx()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val outerRadius = (size.minDimension / 2f) - 4.dp.toPx()
                            val innerRadius = outerRadius - tickLength

                            for (i in 0 until tickCount) {
                                val angle = Math.toRadians((360.0 / tickCount * i) - 90.0)
                                val cosVal = cos(angle).toFloat()
                                val sinVal = sin(angle).toFloat()

                                val start = Offset(
                                    center.x + innerRadius * cosVal,
                                    center.y + innerRadius * sinVal
                                )
                                val end = Offset(
                                    center.x + outerRadius * cosVal,
                                    center.y + outerRadius * sinVal
                                )

                                val tickProgress = i.toFloat() / tickCount
                                val color = if (tickProgress <= progress) tickActiveColor else tickInactiveColor

                                drawLine(
                                    color = color,
                                    start = start,
                                    end = end,
                                    strokeWidth = tickWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = if (!isRunning && !isFinished) Modifier.clickable { showEditDialog = true } else Modifier
                    ) {
                        Text(
                            text = formatTimer(remainingSec),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Light,
                            color = TextPrimary,
                            letterSpacing = (-2).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when {
                                isFinished -> S.timeUp
                                isSetup -> S.tapToEdit
                                else -> formatTotalDuration(totalSec)
                            },
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Presets (only in setup state)
                if (isSetup) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf(1 to 60, 5 to 300, 10 to 600, 30 to 1800, 60 to 3600).forEach { (mins, seconds) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(DarkSurface)
                                    .clickable { viewModel.setDuration(seconds) }
                                    .padding(horizontal = 16.dp, vertical = 7.dp)
                            ) {
                                Text(S.timerPreset(mins), fontSize = 13.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Controls at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 56.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Stop/Reset
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .clickable { viewModel.reset() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = S.reset,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Right: Play/Pause
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

        // Time edit dialog
        if (showEditDialog) {
            TimeEditDialog(
                currentSeconds = totalSec,
                onConfirm = { viewModel.setDuration(it) },
                onDismiss = { showEditDialog = false }
            )
        }
    }
}

@Composable
private fun TimeEditDialog(
    currentSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(currentSeconds / 3600) }
    var selectedMinute by remember { mutableIntStateOf((currentSeconds % 3600) / 60) }
    var selectedSecond by remember { mutableIntStateOf(currentSeconds % 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                S.setTime,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    items = (0..23).toList(),
                    selectedItem = selectedHour,
                    onItemSelected = { selectedHour = it },
                    label = S.hourLabel,
                    backgroundColor = DarkCard
                )
                Text(
                    text = ":",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                WheelPicker(
                    items = (0..59).toList(),
                    selectedItem = selectedMinute,
                    onItemSelected = { selectedMinute = it },
                    label = S.minuteLabel,
                    backgroundColor = DarkCard
                )
                Text(
                    text = ":",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                WheelPicker(
                    items = (0..59).toList(),
                    selectedItem = selectedSecond,
                    onItemSelected = { selectedSecond = it },
                    label = S.secondLabel,
                    backgroundColor = DarkCard
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val total = selectedHour * 3600 + selectedMinute * 60 + selectedSecond
                if (total > 0) onConfirm(total)
                onDismiss()
            }) {
                Text(S.confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(S.cancel)
            }
        },
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary
    )
}

private fun formatTimer(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun formatTotalDuration(totalSeconds: Int): String = S.totalDuration(totalSeconds)

private fun playTimerFinishAlert(context: Context) {
    try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            play()
        }
    } catch (_: Exception) { }

    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 300, 200, 300), -1)
        }
    } catch (_: Exception) { }
}