package com.nexalarm.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class RefinedMinimalistThemeTest {

    @Test
    fun `only focus light and night mode are selectable`() {
        assertEquals(
            listOf(AppTheme.FOCUS_BLUE, AppTheme.MINIMALIST),
            AppTheme.selectableThemes
        )
    }

    @Test
    fun `selectable modes use focus and night display names`() {
        assertEquals("日間專注", AppTheme.FOCUS_BLUE.displayNameZh)
        assertEquals("Focus Light", AppTheme.FOCUS_BLUE.displayNameEn)
        assertEquals("夜間模式", AppTheme.MINIMALIST.displayNameZh)
        assertEquals("Night Mode", AppTheme.MINIMALIST.displayNameEn)
    }

    @Test
    fun `focus light palette uses the specified opaque tokens`() {
        assertPalette(
            AppTheme.FOCUS_BLUE,
            background = Color(0xFFEFF4F9),
            surface = Color(0xFFF7F9FC),
            card = Color(0xFFFFFFFF),
            primary = Color(0xFF0369A1)
        )
    }

    @Test
    fun `quiet minimal palette uses the specified opaque tokens`() {
        assertPalette(
            AppTheme.MINIMALIST,
            background = Color(0xFF121417),
            surface = Color(0xFF1B1E22),
            card = Color(0xFF24282D),
            primary = Color(0xFF38BDF8)
        )
    }

    @Test
    fun `legacy IDs resolve to their supported palettes`() {
        listOf(
            AppTheme.MORNING_GLOW,
            AppTheme.FOREST_MIST,
            AppTheme.GLASSMORPHISM,
            AppTheme.CREAM_JOURNAL,
            AppTheme.CANDY_JELLY,
            AppTheme.MATERIAL_YOU
        ).forEach { theme -> assertEquals(focusBlueColors, theme.colors()) }

        listOf(AppTheme.RETRO_FLIP, AppTheme.STARRY_UNIVERSE).forEach { theme ->
            assertEquals(minimalistColors, theme.colors())
        }
    }

    @Test
    fun `legacy IDs resolve to the expected canonical themes`() {
        listOf(
            AppTheme.MORNING_GLOW,
            AppTheme.FOREST_MIST,
            AppTheme.GLASSMORPHISM,
            AppTheme.CREAM_JOURNAL,
            AppTheme.CANDY_JELLY,
            AppTheme.MATERIAL_YOU
        ).forEach { theme -> assertEquals(AppTheme.FOCUS_BLUE, theme.toSupportedTheme()) }

        listOf(AppTheme.RETRO_FLIP, AppTheme.STARRY_UNIVERSE).forEach { theme ->
            assertEquals(AppTheme.MINIMALIST, theme.toSupportedTheme())
        }
        assertEquals(AppTheme.MINIMALIST, AppTheme.fromId("night_neon"))
    }

    private fun assertPalette(
        theme: AppTheme,
        background: Color,
        surface: Color,
        card: Color,
        primary: Color
    ) {
        val colors = theme.colors()
        assertEquals(background, colors.background)
        assertEquals(surface, colors.surface)
        assertEquals(card, colors.card)
        assertEquals(primary, colors.primary)
        assertEquals(1f, colors.background.alpha)
        assertEquals(1f, colors.surface.alpha)
        assertEquals(1f, colors.card.alpha)
    }
}
