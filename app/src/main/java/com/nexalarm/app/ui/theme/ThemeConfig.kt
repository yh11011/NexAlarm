package com.nexalarm.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 應用主題色彩系統
 * 每個 AppTheme 定義完整的色彩集合，供 Color.kt 中的計算屬性讀取
 */
data class ThemeColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val border: Color,
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val danger: Color,
    val toggleOff: Color,
    val isDark: Boolean,
)

enum class AppTheme(val id: String, val displayNameZh: String, val displayNameEn: String) {
    MINIMALIST     ("minimalist",      "極簡風",     "Minimalist"),
    MORNING_GLOW   ("morning_glow",    "晨曦漸層",   "Morning Glow"),
    NIGHT_NEON     ("night_neon",      "深夜霓虹",   "Night Neon"),
    FOREST_MIST    ("forest_mist",     "森霧自然",   "Forest Mist"),
    GLASSMORPHISM  ("glassmorphism",   "玻璃擬態",   "Glassmorphism"),
    RETRO_FLIP     ("retro_flip",      "復古翻頁鐘", "Retro Flip"),
    FOCUS_BLUE     ("focus_blue",      "專注深藍",   "Focus Blue"),
    CREAM_JOURNAL  ("cream_journal",   "奶油手帳",   "Cream Journal"),
    STARRY_UNIVERSE("starry_universe", "星夜宇宙",   "Starry Universe"),
    CANDY_JELLY    ("candy_jelly",     "糖果果凍",   "Candy Jelly"),
    MATERIAL_YOU   ("material_you",    "Material You", "Material You");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: MINIMALIST
    }
}

// ── 11 個主題色彩定義 ─────────────────────────────────────────

val minimalistColors = ThemeColors(
    background   = Color(0xFF000000),
    surface      = Color(0xFF1C1C1E),
    card         = Color(0xFF2C2C2E),
    border       = Color(0x12FFFFFF),
    primary      = Color(0xFF4DA6FF),
    primaryVariant = Color(0xFF1558B0),
    secondary    = Color(0xFF7CC4FF),
    textPrimary  = Color(0xFFFFFFFF),
    textSecondary= Color(0x8CFFFFFF),
    textTertiary = Color(0x4DFFFFFF),
    danger       = Color(0xFFFF4444),
    toggleOff    = Color(0xFF3A3A3C),
    isDark       = true,
)

val morningGlowColors = ThemeColors(
    background   = Color(0xFFFFF5EE),
    surface      = Color(0xFFFFF0E6),
    card         = Color(0xFFFEEBD8),
    border       = Color(0x12000000),
    primary      = Color(0xFFFF7043),
    primaryVariant = Color(0xFFE64A19),
    secondary    = Color(0xFFFFADB0),
    textPrimary  = Color(0xFF3D2314),
    textSecondary= Color(0x8C3D2314),
    textTertiary = Color(0x4D3D2314),
    danger       = Color(0xFFE53935),
    toggleOff    = Color(0xFFDBBCA8),
    isDark       = false,
)

val nightNeonColors = ThemeColors(
    background   = Color(0xFF050A1A),
    surface      = Color(0xFF0A1128),
    card         = Color(0xFF0F1A3A),
    border       = Color(0x1A00FFC6),
    primary      = Color(0xFF00FFC6),
    primaryVariant = Color(0xFF00CCA0),
    secondary    = Color(0xFF9D4EDD),
    textPrimary  = Color(0xFFE0F0FF),
    textSecondary= Color(0xCC7090C0),
    textTertiary = Color(0x80405080),
    danger       = Color(0xFFFF3860),
    toggleOff    = Color(0xFF1A2A50),
    isDark       = true,
)

val forestMistColors = ThemeColors(
    background   = Color(0xFFF0ECE3),
    surface      = Color(0xFFE8E4DB),
    card         = Color(0xFFDDD9D0),
    border       = Color(0x122D3B35),
    primary      = Color(0xFF5C8A6A),
    primaryVariant = Color(0xFF3D6B50),
    secondary    = Color(0xFF8BADB3),
    textPrimary  = Color(0xFF2D3B35),
    textSecondary= Color(0x8C2D3B35),
    textTertiary = Color(0x4D2D3B35),
    danger       = Color(0xFFC0634A),
    toggleOff    = Color(0xFFC0BCB3),
    isDark       = false,
)

val glassmorphismColors = ThemeColors(
    background   = Color(0xFF2A1260),
    surface      = Color(0x26FFFFFF),
    card         = Color(0x33FFFFFF),
    border       = Color(0x33FFFFFF),
    primary      = Color(0xFFFFFFFF),
    primaryVariant = Color(0xCCFFFFFF),
    secondary    = Color(0xFFC4B5FD),
    textPrimary  = Color(0xFFFFFFFF),
    textSecondary= Color(0xB3FFFFFF),
    textTertiary = Color(0x80FFFFFF),
    danger       = Color(0xFFFF6B9D),
    toggleOff    = Color(0x40FFFFFF),
    isDark       = true,
)

val retroFlipColors = ThemeColors(
    background   = Color(0xFF1C1410),
    surface      = Color(0xFF2A1F17),
    card         = Color(0xFF352920),
    border       = Color(0x1AC8A96E),
    primary      = Color(0xFFC8A96E),
    primaryVariant = Color(0xFFA07840),
    secondary    = Color(0xFF8B6040),
    textPrimary  = Color(0xFFF5E6CC),
    textSecondary= Color(0x8CF5E6CC),
    textTertiary = Color(0x4DF5E6CC),
    danger       = Color(0xFFCC4422),
    toggleOff    = Color(0xFF4A3020),
    isDark       = true,
)

val focusBlueColors = ThemeColors(
    background   = Color(0xFF0F1B2D),
    surface      = Color(0xFF1A2742),
    card         = Color(0xFF2D3748),
    border       = Color(0x1A63B3ED),
    primary      = Color(0xFF63B3ED),
    primaryVariant = Color(0xFF4299E1),
    secondary    = Color(0xFF4299E1),
    textPrimary  = Color(0xFFEBF8FF),
    textSecondary= Color(0xB390CDF4),
    textTertiary = Color(0x804A7FA0),
    danger       = Color(0xFFFC8181),
    toggleOff    = Color(0xFF2D4060),
    isDark       = true,
)

val creamJournalColors = ThemeColors(
    background   = Color(0xFFFBF7F0),
    surface      = Color(0xFFF5EFE0),
    card         = Color(0xFFEEE5D0),
    border       = Color(0x124A3728),
    primary      = Color(0xFFC9956C),
    primaryVariant = Color(0xFFA87050),
    secondary    = Color(0xFFF2C4CE),
    textPrimary  = Color(0xFF4A3728),
    textSecondary= Color(0x8C4A3728),
    textTertiary = Color(0x4D4A3728),
    danger       = Color(0xFFE07070),
    toggleOff    = Color(0xFFD5C5B5),
    isDark       = false,
)

val starryUniverseColors = ThemeColors(
    background   = Color(0xFF0B0C1E),
    surface      = Color(0xFF111228),
    card         = Color(0xFF1A1B35),
    border       = Color(0x1A7B9FE8),
    primary      = Color(0xFF7B9FE8),
    primaryVariant = Color(0xFF5A7ED0),
    secondary    = Color(0xFF6C5CE7),
    textPrimary  = Color(0xFFE8EEFF),
    textSecondary= Color(0xBF8090CC),
    textTertiary = Color(0x80505080),
    danger       = Color(0xFFFF6B6B),
    toggleOff    = Color(0xFF252545),
    isDark       = true,
)

val candyJellyColors = ThemeColors(
    background   = Color(0xFFFFFFFF),
    surface      = Color(0xFFF8F4FF),
    card         = Color(0xFFF0ECFC),
    border       = Color(0x122D2D40),
    primary      = Color(0xFFFF6B6B),
    primaryVariant = Color(0xFFFF4444),
    secondary    = Color(0xFF4ECDC4),
    textPrimary  = Color(0xFF2D2D40),
    textSecondary= Color(0x8C2D2D40),
    textTertiary = Color(0x4D2D2D40),
    danger       = Color(0xFFFF4455),
    toggleOff    = Color(0xFFE0DCF0),
    isDark       = false,
)

val materialYouColors = ThemeColors(
    background   = Color(0xFFFFFBFE),
    surface      = Color(0xFFF3EDF7),
    card         = Color(0xFFECE6F0),
    border       = Color(0x126750A4),
    primary      = Color(0xFF6750A4),
    primaryVariant = Color(0xFF4F378B),
    secondary    = Color(0xFF625B71),
    textPrimary  = Color(0xFF1C1B1F),
    textSecondary= Color(0x8C1C1B1F),
    textTertiary = Color(0x4D1C1B1F),
    danger       = Color(0xFFB3261E),
    toggleOff    = Color(0xFFE7E0EC),
    isDark       = false,
)

/** 依 AppTheme 取得對應色彩集 */
fun AppTheme.colors(): ThemeColors = when (this) {
    AppTheme.MINIMALIST      -> minimalistColors
    AppTheme.MORNING_GLOW    -> morningGlowColors
    AppTheme.NIGHT_NEON      -> nightNeonColors
    AppTheme.FOREST_MIST     -> forestMistColors
    AppTheme.GLASSMORPHISM   -> glassmorphismColors
    AppTheme.RETRO_FLIP      -> retroFlipColors
    AppTheme.FOCUS_BLUE      -> focusBlueColors
    AppTheme.CREAM_JOURNAL   -> creamJournalColors
    AppTheme.STARRY_UNIVERSE -> starryUniverseColors
    AppTheme.CANDY_JELLY     -> candyJellyColors
    AppTheme.MATERIAL_YOU    -> materialYouColors
}

/** 主題代表色（色票顯示用：背景、卡片、主色、次色） */
fun AppTheme.swatchColors(): List<Color> {
    val c = colors()
    return listOf(c.background, c.card, c.primary, c.secondary)
}
