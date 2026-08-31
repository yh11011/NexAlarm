package com.nexalarm.app

import com.nexalarm.app.util.FeatureFlags
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FeatureFlagsTest {

    @Before
    fun setUp() {
        FeatureFlags.isPremium = false
    }

    @Test
    fun `free user can create folder below limit`() {
        assertTrue(FeatureFlags.canCreateFolder(FeatureFlags.FREE_FOLDER_LIMIT - 1))
    }

    @Test
    fun `free user cannot create folder at limit`() {
        assertFalse(FeatureFlags.canCreateFolder(FeatureFlags.FREE_FOLDER_LIMIT))
    }

    @Test
    fun `free user cannot create folder above limit`() {
        assertFalse(FeatureFlags.canCreateFolder(FeatureFlags.FREE_FOLDER_LIMIT + 5))
    }

    @Test
    fun `premium user can create folder at limit`() {
        FeatureFlags.isPremium = true
        assertTrue(FeatureFlags.canCreateFolder(FeatureFlags.FREE_FOLDER_LIMIT))
    }

    @Test
    fun `premium user can create folder well above limit`() {
        FeatureFlags.isPremium = true
        assertTrue(FeatureFlags.canCreateFolder(FeatureFlags.FREE_FOLDER_LIMIT + 100))
    }

    @Test
    fun `free user can create alarm below limit`() {
        assertTrue(FeatureFlags.canCreateAlarm(FeatureFlags.FREE_ALARM_LIMIT - 1))
    }

    @Test
    fun `free user cannot create alarm at limit`() {
        assertFalse(FeatureFlags.canCreateAlarm(FeatureFlags.FREE_ALARM_LIMIT))
    }

    @Test
    fun `free user cannot create alarm above limit`() {
        assertFalse(FeatureFlags.canCreateAlarm(FeatureFlags.FREE_ALARM_LIMIT + 5))
    }

    @Test
    fun `premium user can create alarm at limit`() {
        FeatureFlags.isPremium = true
        assertTrue(FeatureFlags.canCreateAlarm(FeatureFlags.FREE_ALARM_LIMIT))
    }

    @Test
    fun `premium user can create alarm well above limit`() {
        FeatureFlags.isPremium = true
        assertTrue(FeatureFlags.canCreateAlarm(FeatureFlags.FREE_ALARM_LIMIT + 100))
    }

    @Test
    fun `free tier limit is positive`() {
        assertTrue(FeatureFlags.FREE_FOLDER_LIMIT > 0)
    }

    @Test
    fun `alarm limit is positive`() {
        assertTrue(FeatureFlags.FREE_ALARM_LIMIT > 0)
    }
}
