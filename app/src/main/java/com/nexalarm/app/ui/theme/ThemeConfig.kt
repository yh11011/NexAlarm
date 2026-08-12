package com.nexalarm.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Complete, opaque color tokens used by the app theme. */
data class ThemeColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val border: Color,
    val primary: Color,
    val onPrimary: Color,
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
    MINIMALIST      ("minimalist", "夜間模式", "Night Mode"),
    MORNING_GLOW    ("morning_glow", "晨曦漸層", "Morning Glow"),
    FOREST_MIST     ("forest_mist", "森霧自然", "Forest Mist"),
    GLASSMORPHISM   ("glassmorphism", "光霧玻璃", "Atmospheric Glass"),
    RETRO_FLIP      ("retro_flip", "復古翻頁鐘", "Retro Flip"),
    FOCUS_BLUE      ("focus_blue", "日間專注", "Focus Light"),
    CREAM_JOURNAL   ("cream_journal", "奶油手帳", "Cream Journal"),
    STARRY_UNIVERSE ("starry_universe", "星夜宇宙", "Starry Universe"),
    CANDY_JELLY     ("candy_jelly", "糖果果凍", "Candy Jelly"),
    MATERIAL_YOU    ("material_you", "Material You", "Material You");

    companion object {
        val selectableThemes = listOf(FOCUS_BLUE, MINIMALIST)

        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: MINIMALIST
    }
}

/** Maps retained stored IDs onto the two supported productivity themes. */
fun AppTheme.toSupportedTheme(): AppTheme = when (this) {
    AppTheme.FOCUS_BLUE, AppTheme.MINIMALIST -> this
    AppTheme.MORNING_GLOW,
    AppTheme.FOREST_MIST,
    AppTheme.GLASSMORPHISM,
    AppTheme.CREAM_JOURNAL,
    AppTheme.CANDY_JELLY,
    AppTheme.MATERIAL_YOU -> AppTheme.FOCUS_BLUE
    AppTheme.RETRO_FLIP,
    AppTheme.STARRY_UNIVERSE -> AppTheme.MINIMALIST
}

val focusBlueColors = ThemeColors(
    background = Color(0xFFEFF4F9),
    surface = Color(0xFFF7F9FC),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFB8C7D6),
    primary = Color(0xFF0369A1),
    onPrimary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFF075985),
    secondary = Color(0xFF0F5F8B),
    textPrimary = Color(0xFF102A43),
    textSecondary = Color(0xFF405A70),
    textTertiary = Color(0xFF5E7184),
    danger = Color(0xFFB42318),
    toggleOff = Color(0xFFD3DEE8),
    isDark = false,
)

val minimalistColors = ThemeColors(
    background = Color(0xFF121417),
    surface = Color(0xFF1B1E22),
    card = Color(0xFF24282D),
    border = Color(0xFF343A40),
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF121417),
    primaryVariant = Color(0xFFCBD5E1),
    secondary = Color(0xFFCBD5E1),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFFB8C2CC),
    textTertiary = Color(0xFF89939E),
    danger = Color(0xFFFB7185),
    toggleOff = Color(0xFF30353B),
    isDark = true,
)

val morningGlowColors = ThemeColors(
    background = Color(0xFFFBF7F2),
    surface = Color(0xFFF5EEE7),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFD8CDC4),
    primary = Color(0xFF8E4B32),
    onPrimary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFFB96A4C),
    secondary = Color(0xFF9E7D67),
    textPrimary = Color(0xFF2F211A),
    textSecondary = Color(0xFF6E5B50),
    textTertiary = Color(0xFF93847B),
    danger = Color(0xFFB94D44),
    toggleOff = Color(0xFFD8CDC4),
    isDark = false,
)

val forestMistColors = ThemeColors(
    background = Color(0xFFF3F1EA),
    surface = Color(0xFFECE8DF),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFD0CCC2),
    primary = Color(0xFF3F654D),
    onPrimary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFF5F7E68),
    secondary = Color(0xFF6F8179),
    textPrimary = Color(0xFF2D3B35),
    textSecondary = Color(0xFF65736B),
    textTertiary = Color(0xFF8D968F),
    danger = Color(0xFFB25D4D),
    toggleOff = Color(0xFFD0CCC2),
    isDark = false,
)

val glassmorphismColors = ThemeColors(
    background = Color(0xFFFBF9F6),
    surface = Color(0xFFF5F3F0),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFC5C6CA),
    primary = Color(0xFF1A1C1E),
    onPrimary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFF454749),
    secondary = Color(0xFFB8422E),
    textPrimary = Color(0xFF1B1C1A),
    textSecondary = Color(0xFF595F65),
    textTertiary = Color(0xFF75777A),
    danger = Color(0xFFBA1A1A),
    toggleOff = Color(0xFFE4E2DF),
    isDark = false,
)

val retroFlipColors = ThemeColors(
    background = Color(0xFF17130F),
    surface = Color(0xFF211B15),
    card = Color(0xFF2A231C),
    border = Color(0xFF4A3C2F),
    primary = Color(0xFFE8D8BE),
    onPrimary = Color(0xFF221A12),
    primaryVariant = Color(0xFFC8AE87),
    secondary = Color(0xFFB99A70),
    textPrimary = Color(0xFFF0E6D4),
    textSecondary = Color(0xFFC7B8A1),
    textTertiary = Color(0xFF95856F),
    danger = Color(0xFFD17B64),
    toggleOff = Color(0xFF3A3028),
    isDark = true,
)

val creamJournalColors = ThemeColors(
    background = Color(0xFFFBF7F0),
    surface = Color(0xFFF5EFE0),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFD8CABB),
    primary = Color(0xFF8B5E40),
    onPrimary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFFA77A58),
    secondary = Color(0xFF9A7C65),
    textPrimary = Color(0xFF4A3728),
    textSecondary = Color(0xFF765F4B),
    textTertiary = Color(0xFF9A8B7D),
    danger = Color(0xFFB45C52),
    toggleOff = Color(0xFFD8CABB),
    isDark = false,
)

val starryUniverseColors = ThemeColors(
    background = Color(0xFF11131B),
    surface = Color(0xFF181B25),
    card = Color(0xFF212430),
    border = Color(0xFF383D4B),
    primary = Color(0xFFDDE2F0),
    onPrimary = Color(0xFF081029),
    primaryVariant = Color(0xFFADB6CE),
    secondary = Color(0xFFA9B2CC),
    textPrimary = Color(0xFFE8EEFF),
    textSecondary = Color(0xFFB9C0D4),
    textTertiary = Color(0xFF858CA3),
    danger = Color(0xFFE18A7F),
    toggleOff = Color(0xFF303442),
    isDark = true,
)

val candyJellyColors = ThemeColors(
    background = Color(0xFFFBF8FA),
    surface = Color(0xFFF4EEF2),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFD8CDD4),
    primary = Color(0xFF8C4F5C),
    onPrimary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFFA96F7B),
    secondary = Color(0xFF846E78),
    textPrimary = Color(0xFF2D2D40),
    textSecondary = Color(0xFF636071),
    textTertiary = Color(0xFF8F8994),
    danger = Color(0xFFB94D5A),
    toggleOff = Color(0xFFD8CDD4),
    isDark = false,
)

val materialYouColors = ThemeColors(
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFF3EDF7),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFD0CAD6),
    primary = Color(0xFF5E536F),
    onPrimary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFF7A6E89),
    secondary = Color(0xFF6E6775),
    textPrimary = Color(0xFF1C1B1F),
    textSecondary = Color(0xFF625B66),
    textTertiary = Color(0xFF8E8791),
    danger = Color(0xFFB3261E),
    toggleOff = Color(0xFFE7E0EC),
    isDark = false,
)

/** Gets the canonical palette for a selectable theme or retained legacy ID. */
fun AppTheme.colors(): ThemeColors = when (toSupportedTheme()) {
    AppTheme.FOCUS_BLUE -> focusBlueColors
    AppTheme.MINIMALIST -> minimalistColors
    else -> error("Unsupported canonical theme")
}

/** WCAG contrast ratio for two opaque sRGB colors. */
fun contrastRatio(foreground: Color, background: Color): Double {
    fun linear(channel: Float): Double = channel.toDouble().let {
        if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(color: Color): Double =
        0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)

    val foregroundLuminance = luminance(foreground)
    val backgroundLuminance = luminance(background)
    return (max(foregroundLuminance, backgroundLuminance) + 0.05) /
        (min(foregroundLuminance, backgroundLuminance) + 0.05)
}

/** Theme representative colors for a settings swatch. */
fun AppTheme.swatchColors(): List<Color> {
    val colors = colors()
    return listOf(colors.background, colors.card, colors.primary, colors.secondary)
}
