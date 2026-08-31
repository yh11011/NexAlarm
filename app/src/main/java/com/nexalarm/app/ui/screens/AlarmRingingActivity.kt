package com.nexalarm.app.ui.screens

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.receiver.AlarmReceiver
import com.nexalarm.app.service.AlarmService
import com.nexalarm.app.ui.theme.DarkBackground
import com.nexalarm.app.ui.theme.DarkBorder
import com.nexalarm.app.ui.theme.DarkCard
import com.nexalarm.app.ui.theme.NexAlarmTheme
import com.nexalarm.app.ui.theme.PrimaryBlue
import com.nexalarm.app.ui.theme.S
import com.nexalarm.app.ui.theme.TextOnPrimary
import com.nexalarm.app.ui.theme.TextPrimary
import com.nexalarm.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

/**
 * 全螢幕鬧鐘觸發 Activity
 * 在鎖定螢幕或桌面上顯示全螢幕鬧鐘介面。
 * launchMode="singleInstance"：系統保證只有一個實例，後續鬧鐘觸發透過 onNewIntent 傳入。
 */
class AlarmRingingActivity : ComponentActivity() {

    // Compose-observable state：讓 onNewIntent 能即時更新 UI
    private val activeAlarmId = mutableLongStateOf(-1L)
    private val activeAlarmTitle = mutableStateOf(S.alarmDefaultTitle)

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

        updateFromIntent(intent)

        setContent {
            NexAlarmTheme {
                // 讀取 class-level state，onNewIntent 更新後自動觸發重組
                val alarmId = activeAlarmId.longValue
                val fallbackTitle = activeAlarmTitle.value

                var alarm by remember(alarmId) { mutableStateOf<AlarmEntity?>(null) }

                LaunchedEffect(alarmId) {
                    if (alarmId != -1L) {
                        alarm = withContext(Dispatchers.IO) {
                            NexAlarmDatabase.getDatabase(this@AlarmRingingActivity)
                                .alarmDao().getAlarmById(alarmId)
                        }
                    }
                }

                AlarmRingingScreen(
                    alarm = alarm,
                    fallbackTitle = fallbackTitle,
                    onDismiss = {
                        sendDismiss(activeAlarmId.longValue)
                        finish()
                    },
                    onSnooze = {
                        sendSnooze(activeAlarmId.longValue)
                        finish()
                    },
                    onSave = {
                        sendSaveAndDismiss(activeAlarmId.longValue)
                        finish()
                    }
                )
            }
        }
    }

    /**
     * singleInstance 模式下，後續鬧鐘觸發會呼叫此方法而非重建 Activity。
     * 更新 Compose state 即可讓 UI 即時切換到新鬧鐘資訊。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateFromIntent(intent)
    }

    private fun updateFromIntent(intent: Intent) {
        activeAlarmId.longValue = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        activeAlarmTitle.value = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TITLE) ?: S.alarmDefaultTitle
    }

    /**
     * 發送關閉鬧鐘並保存指令到 AlarmReceiver
     * 使用者選擇保存時呼叫，鬧鐘會保持存在並重新排程
     */
    private fun sendSaveAndDismiss(alarmId: Long) {
        // 先停止 AlarmService 的鈴聲/震動
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        startService(stopIntent)

        // 通知 AlarmReceiver 處理後續（保存鬧鐘並重新排程）
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_AND_SAVE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(intent)
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
    onSnooze: () -> Unit,
    onSave: () -> Unit
) {
    val density = LocalDensity.current

    // Current time, ticking every second
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = Calendar.getInstance()
        }
    }

    val snoozeMin = alarm?.snoozeDelay ?: 10
    val snoozeEnabled = alarm?.snoozeEnabled ?: false
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
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
                text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                fontSize = 88.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                letterSpacing = 0.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Date
            val month = now.get(Calendar.MONTH) + 1
            val day = now.get(Calendar.DAY_OF_MONTH)
            val dowNames = listOf("", "日", "一", "二", "三", "四", "五", "六")
            Text(
                text = "${month}月${day}日 星期${dowNames[now.get(Calendar.DAY_OF_WEEK)]}",
                fontSize = 16.sp,
                color = TextSecondary
            )

            val displayTitle = alarm?.title?.takeIf { it.isNotBlank() } ?: fallbackTitle
            Text(
                text = displayTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.padding(top = 18.dp, start = 32.dp, end = 32.dp)
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
                    shape = RoundedCornerShape(8.dp),
                    color = DarkCard,
                    border = BorderStroke(1.dp, DarkBorder),
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
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = S.snoozeReminder(snoozeMin),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Save card（僅對一般鬧鐘顯示）──────────────────────────────
            if (alarm != null && alarm.folderId == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    onClick = onSave,
                    modifier = Modifier
                        .padding(horizontal = 36.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlue,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = S.save,
                            color = TextOnPrimary,
                            fontSize = 16.sp,
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
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .offset(y = arrowOffset.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = S.slideToClose,
                color = TextSecondary,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(44.dp))
        }
    }
}

