package com.nexalarm.app.ui.screens

import android.app.KeyguardManager
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
import com.nexalarm.app.receiver.AlarmReceiver
import com.nexalarm.app.service.AlarmService
import com.nexalarm.app.ui.theme.NexAlarmTheme
import com.nexalarm.app.util.AlarmTestHook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 全螢幕鬧鐘觸發 Activity
 * 在鎖定螢幕或桌面上顯示全螢幕鬧鐘介面
 */
class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 喚醒螢幕 + 在鎖定畫面上方顯示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KeyguardManager::class.java)
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

        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        val alarmTitle = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TITLE) ?: "鬧鐘"

        // ===== 測試 Hook: Level 2 - 全螢幕顯示 =====
        AlarmTestHook.onFullScreenShown(this, alarmId)

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
                    fallbackTitle = alarmTitle,
                    onDismiss = {
                        sendDismiss(alarmId)
                        finish()
                    },
                    onSnooze = {
                        sendSnooze(alarmId)
                        finish()
                    }
                )
            }
        }
    }

    /**
     * 發送關閉鬧鐘指令到 AlarmReceiver
     */
    private fun sendDismiss(alarmId: Long) {
        // 先停止 AlarmService 的鈴聲/震動
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        startService(stopIntent)

        // 通知 AlarmReceiver 處理後續（刪除單次鬧鐘 / 排程重複鬧鐘）
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(intent)
    }

    /**
     * 發送貪睡指令到 AlarmReceiver
     */
    private fun sendSnooze(alarmId: Long) {
        // 先停止 AlarmService 的鈴聲/震動
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        startService(stopIntent)

        // 通知 AlarmReceiver 排程貪睡
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(intent)
    }
}

@Composable
fun AlarmRingingScreen(
    alarm: AlarmEntity?,
    fallbackTitle: String = "鬧鐘",
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
            // 鬧鐘圖示（脈動動畫）
            Icon(
                Icons.Default.Alarm,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale),
                tint = Color.White
            )

            // 時間
            Text(
                text = alarm?.let {
                    String.format(Locale.getDefault(), "%02d:%02d", it.hour, it.minute)
                } ?: "--:--",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // 標題
            val title = alarm?.title?.takeIf { it.isNotBlank() } ?: fallbackTitle
            Text(
                text = title,
                fontSize = 24.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 按鈕列
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // 貪睡
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
                        Icon(
                            Icons.Default.Snooze,
                            contentDescription = "貪睡",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("延後", fontSize = 12.sp)
                    }
                }

                // 關閉
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
                        Icon(
                            Icons.Default.AlarmOff,
                            contentDescription = "關閉",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("關閉", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

