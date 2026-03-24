package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.AlarmScheduler
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen(
    alarms: List<AlarmEntity>,
    onGoToAlarms: () -> Unit
) {
    val openMenu = LocalMenuAction.current
    val context = LocalContext.current
    val scheduler = AlarmScheduler(context)

    val enabledAlarms = alarms.filter { it.isEnabled }
    val nextAlarm = enabledAlarms.minByOrNull { scheduler.getNextTriggerTime(it) }
    val timeUntil = nextAlarm?.let { scheduler.getTimeUntilText(it, isAppEnglish) } ?: ""

    Column(modifier = Modifier.fillMaxSize()) {
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
                text = S.home,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 下一個鬧鐘卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Text(
                        text = S.homeNextAlarm,
                        fontSize = 12.sp,
                        color = TextTertiary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (nextAlarm != null) {
                        Text(
                            text = String.format("%02d:%02d", nextAlarm.hour, nextAlarm.minute),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Light,
                            color = TextPrimary,
                            letterSpacing = (-1).sp
                        )
                        if (nextAlarm.title.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = nextAlarm.title,
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = timeUntil,
                            fontSize = 14.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = S.homeNoActiveAlarm,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 統計列
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = S.alarm,
                    value = alarms.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = S.homeActiveCount(enabledAlarms.size),
                    value = enabledAlarms.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            // 查看鬧鐘按鈕
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccentDim)
                    .clickable(onClick = onGoToAlarms)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = S.goToAlarms,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(DarkSurface, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Light, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
        }
    }
}
