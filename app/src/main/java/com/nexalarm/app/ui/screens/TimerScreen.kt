package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.viewmodel.TimerViewModel

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = viewModel()
) {
    val totalSec by viewModel.totalSeconds.collectAsState()
    val remainingSec by viewModel.remainingSeconds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()

    val progress = if (totalSec > 0) remainingSec.toFloat() / totalSec.toFloat() else 1f
    val arcColor = if (isFinished) LapFast else PrimaryBlue
    val trackColor = DarkSurface

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp)
        ) {
            Text(
                text = "計時器",
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary,
                letterSpacing = (-0.3).sp
            )
        }

        // Circular timer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .drawBehind {
                        val strokeWidth = 10.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2f
                        val topLeft = Offset(
                            (size.width - 2 * radius) / 2f,
                            (size.height - 2 * radius) / 2f
                        )
                        val arcSize = Size(radius * 2, radius * 2)

                        // Track
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Progress arc
                        drawArc(
                            color = arcColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%02d:%02d".format(remainingSec / 60, remainingSec % 60),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Light,
                        color = TextPrimary,
                        letterSpacing = (-2).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            isFinished -> "時間到！"
                            isRunning -> "計時中..."
                            remainingSec < totalSec -> "已暫停"
                            else -> "點擊開始"
                        },
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            listOf(
                "1 分" to 60,
                "3 分" to 180,
                "5 分" to 300,
                "10 分" to 600,
                "25 分" to 1500,
                "1 時" to 3600
            ).forEach { (label, seconds) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurface)
                        .clickable { viewModel.setDuration(seconds) }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(label, fontSize = 13.sp, color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .clickable { viewModel.reset() },
                contentAlignment = Alignment.Center
            ) {
                Text("↺", fontSize = 22.sp, color = TextPrimary)
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Play/Pause
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

            // +1 minute
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .clickable { viewModel.addOneMinute() },
                contentAlignment = Alignment.Center
            ) {
                Text("+1m", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
    }
}

