package com.nexalarm.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.nexalarm.app.util.AppSettingsProvider

val LocalMenuAction = staticCompositionLocalOf<() -> Unit> { {} }

// Global app state - Delegated to AppSettingsProvider for thread-safe access
// Use AppSettingsProvider.isDarkThemeMutableState in Compose, or AppSettingsProvider.getDarkMode() elsewhere
var isDarkTheme: Boolean
    get() = AppSettingsProvider.isDarkThemeMutableState.value
    set(value) = AppSettingsProvider.setDarkMode(value)

var isAppEnglish: Boolean
    get() = AppSettingsProvider.isAppEnglishMutableState.value
    set(value) = AppSettingsProvider.setLanguageEnglish(value)

@Composable
fun NexAlarmTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = PrimaryBlue,
            onPrimary = Color.White,
            primaryContainer = PrimaryBlueVariant,
            secondary = SecondaryBlue,
            background = DarkBackground,
            onBackground = TextPrimary,
            surface = DarkBackground,
            onSurface = TextPrimary,
            surfaceVariant = DarkCard,
            onSurfaceVariant = TextSecondary,
            surfaceContainerHighest = DarkSurface,
            outline = DarkBorder
        )
    } else {
        lightColorScheme(
            primary = PrimaryBlue,
            onPrimary = Color.White,
            primaryContainer = PrimaryBlueVariant,
            secondary = SecondaryBlue,
            background = DarkBackground,
            onBackground = TextPrimary,
            surface = DarkBackground,
            onSurface = TextPrimary,
            surfaceVariant = DarkCard,
            onSurfaceVariant = TextSecondary,
            surfaceContainerHighest = DarkSurface,
            outline = DarkBorder
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
