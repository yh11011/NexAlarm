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
private fun WheelPicker(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    label: String
) {
    val itemHeight = 44.dp
    val visibleItems = 5
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedItem
    )

    // Snap to nearest item when scrolling stops
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex +
                    (visibleItems / 2)
            val adjustedIndex = (centerIndex - 2).coerceIn(0, items.lastIndex)
            onItemSelected(items[adjustedIndex])
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextTertiary,
            letterSpacing = 0.06.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.height(itemHeight * visibleItems).width(80.dp)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = itemHeight * 2)
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    val isSelected = item == selectedItem
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clickable {
                                onItemSelected(item)
                                scope.launch {
                                    listState.animateScrollToItem(
                                        index.coerceAtLeast(0)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d", item),
                            fontSize = if (isSelected) 32.sp else 26.sp,
                            fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Light,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            // Selection indicator
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

            // Fade gradients
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .align(Alignment.TopCenter)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(
                                listOf(DarkSurface, Color.Transparent)
                            )
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .align(Alignment.BottomCenter)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, DarkSurface)
                            )
                        )
                    }
            )
        }
    }
}

