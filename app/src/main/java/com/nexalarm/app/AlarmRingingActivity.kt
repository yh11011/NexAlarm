package com.nexalarm.app

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.service.AlarmService
import com.nexalarm.app.ui.theme.NexAlarmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake + show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmId = intent.getLongExtra(AlarmService.EXTRA_ALARM_ID, -1L)

        setContent {
            NexAlarmTheme {
                var alarm by remember { mutableStateOf<AlarmEntity?>(null) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(alarmId) {
                    if (alarmId != -1L) {
                        scope.launch(Dispatchers.IO) {
                            val db = NexAlarmDatabase.getDatabase(this@AlarmRingingActivity)
                            alarm = db.alarmDao().getAlarmById(alarmId)
                        }
                    }
                }

                AlarmRingingScreen(
                    alarm = alarm,
                    onDismiss = {
                        sendAction(AlarmService.ACTION_DISMISS, alarmId)
                        finish()
                    },
                    onSnooze = {
                        sendAction(AlarmService.ACTION_SNOOZE, alarmId)
                        finish()
                    }
                )
            }
        }
    }

    private fun sendAction(action: String, alarmId: Long) {
        val intent = Intent(this, AlarmService::class.java).apply {
            this.action = action
            putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
        }
        startService(intent)
    }
}

@Composable
fun AlarmRingingScreen(
    alarm: AlarmEntity?,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A237E),
                        Color(0xFF0D47A1),
                        Color(0xFF01579B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                Icons.Default.Alarm,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale),
                tint = Color.White
            )

            Text(
                text = alarm?.let { String.format("%02d:%02d", it.hour, it.minute) } ?: "--:--",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (alarm != null && alarm.title.isNotBlank()) {
                Text(
                    text = alarm.title,
                    fontSize = 24.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Snooze button
                FilledTonalButton(
                    onClick = onSnooze,
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Snooze, contentDescription = "Snooze", modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Snooze", fontSize = 12.sp)
                    }
                }

                // Dismiss button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AlarmOff, contentDescription = "Dismiss", modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Dismiss", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
