package com.nexalarm.app.ui.theme

import androidx.compose.ui.graphics.Color

// Fixed colors (same in both themes)
val PrimaryBlue = Color(0xFF1A73E8)
val PrimaryBlueVariant = Color(0xFF1558B0)
val SecondaryBlue = Color(0xFF4CA8FF)
val AccentDim = Color(0x261A73E8)
val DangerRed = Color(0xFFF44336)
val LapFast = Color(0xFF34A853)
val LapSlow = Color(0xFFEA4335)

// Theme-adaptive colors (read isDarkTheme state, auto-trigger recomposition)
val DarkBackground: Color get() = if (isDarkTheme) Color(0xFF000000) else Color(0xFFF2F2F7)
val DarkSurface: Color get() = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
val DarkCard: Color get() = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
val DarkBorder: Color get() = if (isDarkTheme) Color(0x12FFFFFF) else Color(0x12000000)
val TextPrimary: Color get() = if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
val TextSecondary: Color get() = if (isDarkTheme) Color(0x8CFFFFFF) else Color(0x8C3C3C43)
val TextTertiary: Color get() = if (isDarkTheme) Color(0x4DFFFFFF) else Color(0x4D3C3C43)
val ToggleOff: Color get() = if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
