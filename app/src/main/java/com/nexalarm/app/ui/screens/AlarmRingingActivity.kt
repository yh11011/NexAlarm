package com.nexalarm.app.ui.screens

import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.receiver.AlarmReceiver
import com.nexalarm.app.service.AlarmService
import com.nexalarm.app.ui.theme.NexAlarmTheme
import com.nexalarm.app.ui.theme.S
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

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
        val alarmTitle = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TITLE) ?: S.alarmDefaultTitle

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
    fallbackTitle: String = S.alarmDefaultTitle,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Current time, ticking every second
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = Calendar.getInstance()
        }
    }

    // Load device wallpaper once as a blurred background
    val wallpaperBitmap = remember {
        try {
            val wm = WallpaperManager.getInstance(context)
            val drawable = wm.drawable
            if (drawable != null) {
                val dm = context.resources.displayMetrics
                val w = dm.widthPixels.coerceAtLeast(1)
                val h = dm.heightPixels.coerceAtLeast(1)
                val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bm)
                drawable.setBounds(0, 0, w, h)
                drawable.draw(canvas)
                bm.asImageBitmap()
            } else null
        } catch (_: Exception) { null }
    }

    val snoozeMin = alarm?.snoozeDelay ?: 10
    val snoozeEnabled = alarm?.snoozeEnabled ?: true
    val dismissThresholdPx = with(density) { 100.dp.toPx() }
    var swipeDelta by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (swipeDelta < -dismissThresholdPx) onDismiss()
                        swipeDelta = 0f
                    },
                    onDragCancel = { swipeDelta = 0f }
                ) { _, dragAmount -> swipeDelta += dragAmount }
            }
    ) {
        // ── Background ──────────────────────────────────────────────────────
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF3D2318), Color(0xFF5C3820), Color(0xFF1C0D06))
                        )
                    )
            )
        }
        // Subtle dark overlay for text contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
        )

        // ── Content ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.28f))

            // Current time
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val minute = now.get(Calendar.MINUTE)
            Text(
                text = String.format("%02d:%02d", hour, minute),
                fontSize = 88.sp,
                fontWeight = FontWeight.Thin,
                color = Color.White.copy(alpha = 0.93f),
                letterSpacing = (-2).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Date
            val month = now.get(Calendar.MONTH) + 1
            val day = now.get(Calendar.DAY_OF_MONTH)
            val dowNames = listOf("", "日", "一", "二", "三", "四", "五", "六")
            Text(
                text = "${month}月${day}日 星期${dowNames[now.get(Calendar.DAY_OF_WEEK)]}",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.72f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Snooze card（僅在貪睡功能啟用時顯示）────────────────────────
            if (snoozeEnabled) {
                Surface(
                    onClick = onSnooze,
                    modifier = Modifier
                        .padding(horizontal = 36.dp)
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFFF2EDE6).copy(alpha = 0.88f),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = S.snoozeReminder(snoozeMin),
                            color = Color(0xFF1A1A1A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(52.dp))

            // ── Dismiss: bouncing arrow + label ──────────────────────────────
            val infiniteTransition = rememberInfiniteTransition(label = "arrow")
            val arrowOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "arrowBounce"
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = S.slideToClose,
                tint = Color.White.copy(alpha = 0.82f),
                modifier = Modifier
                    .size(28.dp)
                    .offset(y = arrowOffset.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = S.slideToClose,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(44.dp))
        }
    }
}

