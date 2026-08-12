package com.nexalarm.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleGroupIconTest {

    @Test
    fun `legacy business emoji resolves to the professional briefcase icon`() {
        assertEquals("briefcase", scheduleGroupIconId("💼"))
    }

    @Test
    fun `legacy study emojis resolve to the academic book icon`() {
        assertEquals("book", scheduleGroupIconId("📘"))
        assertEquals("book", scheduleGroupIconId("📚"))
    }

    @Test
    fun `unknown saved value stays useful with a calendar fallback`() {
        assertEquals("calendar", scheduleGroupIconId("unknown"))
    }
}
