package com.nexalarm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.ui.theme.S
import com.nexalarm.app.ui.theme.TextPrimary
import com.nexalarm.app.ui.theme.TextSecondary
import com.nexalarm.app.ui.theme.nexGlassSurface
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 底部導航 Tab 定義
 */
enum class BottomTab(val label: String, val icon: ImageVector, val route: String) {
    ALARM("鬧鐘", Icons.Default.Notifications, "alarm"),
    FOLDERS("資料夾", Icons.Default.Folder, "folders"),
    STOPWATCH("碼錶", Icons.Default.Timer, "stopwatch"),
    TIMER("計時", Icons.Default.HourglassBottom, "timer")
}

/**
 * 自定義底部導航欄，帶有滑動下劃線指示器
 * 在滑動手勢期間即時跟蹤 Pager 位置 — 符合三星時鐘應用參考風格
 */
@Composable
fun PagerBottomBar(
    tabs: List<BottomTab>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    currentRoute: String?,
    onTabClick: (Int) -> Unit
) {
    val onTabs = currentRoute == "tabs"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .nexGlassSurface(26.dp)
        ) {
            // Tab items
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, tab ->
                    val selected = onTabs && pagerState.currentPage == index
                    val label = when (tab) {
                        BottomTab.ALARM -> S.alarm
                        BottomTab.FOLDERS -> S.folders
                        BottomTab.STOPWATCH -> S.stopwatch
                        BottomTab.TIMER -> S.timer
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onTabClick(index) }
                            .padding(top = 10.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = label,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) TextPrimary else TextSecondary.copy(alpha = 0.55f)
                        )
                        Text(
                            label,
                            fontSize = 10.sp,
                            color = if (selected) TextPrimary else TextSecondary.copy(alpha = 0.55f)
                        )
                    }
                }
            }

            // 滑動下劃線指示器 — 使用佈局階段 offset lambda 避免組合
            // 在每次滑動幀上重新組合（僅重新佈局指示器 Box 本身）
            if (onTabs) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                ) {
                    val tabWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                        (maxWidth / tabs.size).toPx()
                    }
                    val indicatorWidthFraction = 0.45f
                    val indicatorWidthPx = tabWidthPx * indicatorWidthFraction
                    val indicatorWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) {
                        indicatorWidthPx.toDp()
                    }
                    val startOffsetPx = (tabWidthPx - indicatorWidthPx) / 2f

                    Box(
                        modifier = Modifier
                            .width(indicatorWidthDp)
                            .height(2.dp)
                            // Lambda 形式：在佈局階段讀取 pagerState，而非組合階段
                            .offset {
                                val pos = pagerState.currentPage + pagerState.currentPageOffsetFraction
                                IntOffset((startOffsetPx + tabWidthPx * pos).roundToInt(), 0)
                            }
                            .background(TextPrimary, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}
