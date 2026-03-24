package com.nexalarm.app.data.model

import org.junit.Assert.*
import org.junit.Test

class RepeatDaysConverterTest {

    private val converter = RepeatDaysConverter()

    @Test
    fun `fromList converts days to comma-separated string`() {
        assertEquals("1,2,3", converter.fromList(listOf(1, 2, 3)))
    }

    @Test
    fun `fromList handles empty list`() {
        assertEquals("", converter.fromList(emptyList()))
    }

    @Test
    fun `fromList single day`() {
        assertEquals("5", converter.fromList(listOf(5)))
    }

    @Test
    fun `toList parses comma-separated string`() {
        assertEquals(listOf(1, 2, 3), converter.toList("1,2,3"))
    }

    @Test
    fun `toList handles blank string`() {
        assertEquals(emptyList<Int>(), converter.toList(""))
    }

    @Test
    fun `toList handles whitespace around values`() {
        assertEquals(listOf(1, 2, 3), converter.toList("1, 2, 3"))
    }

    @Test
    fun `toList handles single value`() {
        assertEquals(listOf(7), converter.toList("7"))
    }

    @Test
    fun `roundtrip consistency for weekday list`() {
        val days = listOf(1, 2, 3, 4, 5)
        assertEquals(days, converter.toList(converter.fromList(days)))
    }

    @Test
    fun `roundtrip consistency for weekend list`() {
        val days = listOf(6, 7)
        assertEquals(days, converter.toList(converter.fromList(days)))
    }
}
