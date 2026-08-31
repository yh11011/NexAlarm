package com.nexalarm.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumStatusPolicyTest {

    @Test
    fun `play store query keeps cached premium when no play purchase exists`() {
        val result = PremiumStatusPolicy.resolveAfterPlayStoreQuery(
            currentIsPremium = true,
            hasPlayStorePurchase = false
        )

        assertTrue(result)
    }

    @Test
    fun `play store query enables premium when play purchase exists`() {
        val result = PremiumStatusPolicy.resolveAfterPlayStoreQuery(
            currentIsPremium = false,
            hasPlayStorePurchase = true
        )

        assertTrue(result)
    }

    @Test
    fun `account sync downgrades only when account and play purchase are both false`() {
        val result = PremiumStatusPolicy.resolveFromAccountSync(
            accountIsPremium = false,
            hasPlayStorePurchase = false
        )

        assertFalse(result)
    }

    @Test
    fun `account sync preserves premium when play purchase exists`() {
        val result = PremiumStatusPolicy.resolveFromAccountSync(
            accountIsPremium = false,
            hasPlayStorePurchase = true
        )

        assertTrue(result)
    }

    @Test
    fun `manual deactivate does not disable active play purchase`() {
        val result = PremiumStatusPolicy.resolveAfterManualDeactivate(
            hasPlayStorePurchase = true
        )

        assertTrue(result)
    }

    @Test
    fun `manual deactivate disables promo premium when no play purchase exists`() {
        val result = PremiumStatusPolicy.resolveAfterManualDeactivate(
            hasPlayStorePurchase = false
        )

        assertFalse(result)
    }
}
