package com.nexalarm.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.nexalarm.app.util.AppSettingsProvider

// ── 非主題固定色 ─────────────────────────────────────────────
val LapFast = Color(0xFF34A853)
val LapSlow = Color(0xFFEA4335)

// ── 主題自適應色（從 AppSettingsProvider 的當前主題讀取，自動觸發重組） ──

/** 快捷取得當前主題色彩集 */
private fun t(): ThemeColors = AppSettingsProvider.currentThemeMutableState.value.colors()

val PrimaryBlue: Color       get() = t().primary
val TextOnPrimary: Color     get() = t().onPrimary
val PrimaryBlueVariant: Color get() = t().primaryVariant
val SecondaryBlue: Color     get() = t().secondary
val AccentDim: Color         get() = t().primary.copy(alpha = 0.15f)
val DangerRed: Color         get() = t().danger

val DarkBackground: Color    get() = t().background
val DarkSurface: Color       get() = t().surface
val DarkCard: Color          get() = t().card
val DarkBorder: Color        get() = t().border
val TextPrimary: Color       get() = t().textPrimary
val TextSecondary: Color     get() = t().textSecondary
val TextTertiary: Color      get() = t().textTertiary
val ToggleOff: Color         get() = t().toggleOff
