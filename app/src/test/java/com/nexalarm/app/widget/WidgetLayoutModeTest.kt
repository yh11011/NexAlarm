package com.nexalarm.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutModeTest {

    @Test
    fun `uses expanded presentation when the widget meets both expanded dimensions`() {
        val mode = WidgetLayoutMode.from(minWidthDp = 180, minHeightDp = 72)

        assertEquals(WidgetLayoutMode.EXPANDED, mode)
        assertTrue(mode.showsTemperature)
        assertEquals(58f, mode.timeTextSizeSp)
    }

    @Test
    fun `uses compact presentation when either expanded dimension is unavailable`() {
        assertEquals(
            WidgetLayoutMode.COMPACT,
            WidgetLayoutMode.from(minWidthDp = 179, minHeightDp = 72)
        )
        assertEquals(
            WidgetLayoutMode.COMPACT,
            WidgetLayoutMode.from(minWidthDp = 180, minHeightDp = 71)
        )
        assertFalse(WidgetLayoutMode.COMPACT.showsTemperature)
        assertEquals(44f, WidgetLayoutMode.COMPACT.timeTextSizeSp)
    }
}
