package com.nexalarm.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueVariant,
    secondary = SecondaryBlue,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkBackground,
    onSurface = Color.White,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHighest = DarkSurface,
    outline = DarkBorder
)

@Composable
fun NexAlarmTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
