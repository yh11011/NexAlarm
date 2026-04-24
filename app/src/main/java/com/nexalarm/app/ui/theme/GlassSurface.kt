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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexalarm.app.util.AppSettingsProvider

@Composable
fun NexBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = AppSettingsProvider.currentThemeMutableState.value
    val colors = if (theme == AppTheme.GLASSMORPHISM) {
        listOf(
            Color(0xFF060E20),
            Color(0xFF0B1326),
            Color(0xFF1E3A8A),
            Color(0xFF4C1D95),
            Color(0xFF831843)
        )
    } else {
        listOf(DarkBackground, DarkBackground)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors)),
        content = content
    )
}

fun Modifier.nexGlassSurface(
    cornerRadius: Dp = 16.dp,
    elevated: Boolean = false
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return clip(shape)
        .background(if (elevated) DarkCard else DarkSurface, shape)
        .border(1.dp, DarkBorder, shape)
}

