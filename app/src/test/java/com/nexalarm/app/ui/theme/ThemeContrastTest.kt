package com.nexalarm.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {

    @Test
    fun `contrast ratio uses WCAG relative luminance`() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.001)
        assertEquals(1.0, contrastRatio(Color(0xFF123456), Color(0xFF123456)), 0.001)
    }

    @Test
    fun `supported palettes meet text and border contrast floors`() {
        AppTheme.selectableThemes.forEach { theme ->
            val colors = theme.colors()

            assertTrue("${theme.id} primary text", contrastRatio(colors.textPrimary, colors.background) >= 7.0)
            assertTrue("${theme.id} secondary text", contrastRatio(colors.textSecondary, colors.background) >= 4.5)
            assertTrue("${theme.id} tertiary text on surface", contrastRatio(colors.textTertiary, colors.surface) >= 4.5)
            assertTrue("${theme.id} tertiary text on card", contrastRatio(colors.textTertiary, colors.card) >= 4.5)
            assertTrue("${theme.id} border", contrastRatio(colors.border, colors.background) >= 1.5)
        }
    }
}
