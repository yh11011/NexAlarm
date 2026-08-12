package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.ui.components.NexIconBadge
import com.nexalarm.app.ui.components.NexMetricCard
import com.nexalarm.app.ui.components.NexTopBar
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.AlarmScheduler
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.nexalarm.app.ui.components.AlarmReliabilityCard
import java.util.Locale
import com.nexalarm.app.util.AlarmReliabilityChecker
import com.nexalarm.app.util.TestAlarmScheduler
import com.nexalarm.app.util.ScheduleGroupPlanner

@Composable
fun HomeScreen(
    alarms: List<AlarmEntity>,
    groups: List<FolderEntity>,
    onGoToAlarms: () -> Unit
) {
    val openMenu = LocalMenuAction.current
    val context = LocalContext.current
    val scheduler = AlarmScheduler(context)

    val enabledAlarms = ScheduleGroupPlanner.activeAlarms(alarms, groups)
    val nextAlarm = enabledAlarms.minByOrNull { scheduler.getNextTriggerTime(it) }
    val timeUntil = nextAlarm?.let { scheduler.getTimeUntilText(it, isAppEnglish) } ?: ""
    val reliabilityState = AlarmReliabilityChecker.evaluate(context)

    Column(modifier = Modifier.fillMaxSize()) {
        NexTopBar(title = S.home, onMenuClick = openMenu)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 下一個鬧鐘卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .nexGlassSurface(24.dp, elevated = true)
                    .padding(horizontal = 22.dp, vertical = 22.dp)
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
                            text = String.format(Locale.getDefault(), "%02d:%02d", nextAlarm.hour, nextAlarm.minute),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            letterSpacing = 0.sp
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

            AlarmReliabilityCard(
                state = reliabilityState,
                onTestAlarm = {
                    TestAlarmScheduler.schedule(context)
                    Toast.makeText(context, S.testAlarmScheduled, Toast.LENGTH_LONG).show()
                }
            )

            // 統計列
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NexMetricCard(
                    label = S.alarm,
                    value = alarms.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                NexMetricCard(
                    label = S.homeActiveCount(enabledAlarms.size),
                    value = enabledAlarms.size.toString(),
                    modifier = Modifier.weight(1f),
                    elevated = true
                )
            }

            // 查看鬧鐘按鈕
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentDim)
                    .clickable(onClick = onGoToAlarms)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NexIconBadge(
                        icon = Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        selected = false
                    )
                    Text(
                        text = S.goToAlarms,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
