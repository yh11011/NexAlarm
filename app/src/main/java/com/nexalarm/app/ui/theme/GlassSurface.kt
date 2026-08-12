package com.nexalarm.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NexBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        content = content
    )
}

fun Modifier.nexGlassSurface(
    cornerRadius: Dp = 16.dp,
    elevated: Boolean = false
): Modifier {
    val refinedRadius = when {
        cornerRadius.value >= 32f -> cornerRadius
        else -> minOf(cornerRadius, 8.dp)
    }
    val shape = RoundedCornerShape(refinedRadius)
    val surfaceColor = if (elevated) DarkCard else DarkSurface
    return clip(shape)
        .background(surfaceColor, shape)
        .border(1.dp, DarkBorder, shape)
}
