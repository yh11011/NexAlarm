package com.nexalarm.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val NexTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

object NexCornerRadius {
    val compact = 12.dp
    val control = 16.dp
    val card = 24.dp
    val panel = 28.dp
}

private val NexShapes = Shapes(
    extraSmall = RoundedCornerShape(NexCornerRadius.compact),
    small = RoundedCornerShape(NexCornerRadius.compact),
    medium = RoundedCornerShape(NexCornerRadius.control),
    large = RoundedCornerShape(NexCornerRadius.card),
    extraLarge = RoundedCornerShape(NexCornerRadius.panel)
)

@Composable
fun NexAlarmTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = PrimaryBlue,
            onPrimary = TextOnPrimary,
            primaryContainer = PrimaryBlueVariant,
            secondary = SecondaryBlue,
            background = DarkBackground,
            onBackground = TextPrimary,
            surface = DarkSurface,
            onSurface = TextPrimary,
            surfaceVariant = DarkCard,
            onSurfaceVariant = TextSecondary,
            surfaceContainerHighest = DarkCard,
            outline = DarkBorder
        )
    } else {
        lightColorScheme(
            primary = PrimaryBlue,
            onPrimary = TextOnPrimary,
            primaryContainer = PrimaryBlueVariant,
            secondary = SecondaryBlue,
            background = DarkBackground,
            onBackground = TextPrimary,
            surface = DarkSurface,
            onSurface = TextPrimary,
            surfaceVariant = DarkCard,
            onSurfaceVariant = TextSecondary,
            surfaceContainerHighest = DarkCard,
            outline = DarkBorder
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexTypography,
        shapes = NexShapes,
        content = content
    )
}
