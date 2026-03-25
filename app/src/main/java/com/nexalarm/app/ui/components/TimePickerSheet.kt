package com.nexalarm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import com.nexalarm.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TimePickerSheet(
    visible: Boolean,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    if (!visible) return

    var selectedHour by remember(visible) { mutableIntStateOf(initialHour) }
    var selectedMinute by remember(visible) { mutableIntStateOf(initialMinute) }

    // Overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    )

    // Sheet
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    DarkSurface,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {} // consume clicks
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "設定時間",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Drum pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    items = (0..23).toList(),
                    selectedItem = selectedHour,
                    onItemSelected = { selectedHour = it },
                    label = "小時"
                )
                Text(
                    text = ":",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 22.dp)
                )
                WheelPicker(
                    items = (0..59).toList(),
                    selectedItem = selectedMinute,
                    onItemSelected = { selectedMinute = it },
                    label = "分鐘"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onConfirm(selectedHour, selectedMinute) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                )
            ) {
                Text("確認", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun WheelPicker(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    label: String = "",
    backgroundColor: Color = DarkSurface,
    itemHeightDp: Dp = 44.dp,
    visibleItemCount: Int = 5,
    selectedFontSize: TextUnit = 32.sp,
    otherFontSize: TextUnit = 26.sp,
    showDividers: Boolean = true,
    pickerWidth: Dp = 80.dp
) {
    val itemHeight = itemHeightDp
    val visibleItems = visibleItemCount
    val scope = rememberCoroutineScope()
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    val itemCount = items.size
    val multiplier = 1000
    val totalCount = itemCount * multiplier
    val middleBase = (multiplier / 2) * itemCount
    val initialIndex = middleBase + items.indexOf(selectedItem).coerceAtLeast(0)

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex
    )
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // 從滾動位置即時推導出視覺上居中的項目，避免等待父元件重組造成卡頓
    val centeredItem by remember(itemCount) {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val centerIndex = if (offset > itemHeightPx / 2f) firstIndex + 1 else firstIndex
            val realIndex = ((centerIndex % itemCount) + itemCount) % itemCount
            items[realIndex]
        }
    }

    // 滾動停止後回報選取值給父元件
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    val firstIndex = listState.firstVisibleItemIndex
                    val offset = listState.firstVisibleItemScrollOffset
                    val targetVirtualIndex =
                        if (offset > itemHeightPx / 2f) firstIndex + 1 else firstIndex
                    val targetRealIndex =
                        ((targetVirtualIndex % itemCount) + itemCount) % itemCount
                    onItemSelected(items[targetRealIndex])
                    if (offset != 0) {
                        listState.animateScrollToItem(targetVirtualIndex, scrollOffset = 0)
                    }
                }
            }
    }

    // 處理外部選取變更（例如預設按鈕）
    LaunchedEffect(selectedItem) {
        if (centeredItem != selectedItem) {
            val targetIndex = middleBase + items.indexOf(selectedItem).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex, scrollOffset = 0)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary,
                letterSpacing = 0.06.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Box(modifier = Modifier
            .height(itemHeight * visibleItems)
            .width(pickerWidth)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                flingBehavior = snapFlingBehavior,
                contentPadding = PaddingValues(vertical = itemHeight * (visibleItems / 2))
            ) {
                items(totalCount) { virtualIndex ->
                    val realIndex = ((virtualIndex % itemCount) + itemCount) % itemCount
                    val item = items[realIndex]
                    val isSelected = item == centeredItem
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clickable {
                                onItemSelected(item)
                                scope.launch {
                                    listState.animateScrollToItem(
                                        virtualIndex.coerceAtLeast(0), scrollOffset = 0
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d", item),
                            fontSize = if (isSelected) selectedFontSize else otherFontSize,
                            fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Light,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            if (showDividers) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .align(Alignment.Center)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.align(Alignment.TopStart),
                        color = Color.White.copy(alpha = 0.12f)
                    )
                    HorizontalDivider(
                        modifier = Modifier.align(Alignment.BottomStart),
                        color = Color.White.copy(alpha = 0.12f)
                    )
                }
            }

            // 漸層遮罩：縮小高度，避免遮住選中項目的文字
            val fadeHeight = itemHeight * (visibleItems / 2) - itemHeight * 0.2f
            // Top fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fadeHeight)
                    .align(Alignment.TopCenter)
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.verticalGradient(listOf(backgroundColor, Color.Transparent)))
                    }
            )
            // Bottom fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fadeHeight)
                    .align(Alignment.BottomCenter)
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, backgroundColor)))
                    }
            )
        }
    }
}

