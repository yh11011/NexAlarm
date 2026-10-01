package com.nexalarm.app.data

import org.junit.Assert.*
import org.junit.Test

class SyncBatchPolicyTest {
    @Test fun largeCollectionsPreserveOrderAndFitExistingRequestLimit() {
        val values = List(100) { "鬧鐘$it".repeat(50) }
        val groups = SyncBatchPolicy.group(values)
        assertEquals(values, groups.flatten())
        assertTrue(groups.size > 1)
        assertTrue(groups.all { group -> group.sumOf { it.toByteArray(Charsets.UTF_8).size + 1 } <= 8000 })
    }
    @Test fun emptyCollectionStillFetchesCloudChanges() { assertEquals(listOf(emptyList<String>()), SyncBatchPolicy.group(emptyList())) }
    @Test(expected = IllegalArgumentException::class) fun oversizedSingleAlarmIsRejected() { SyncBatchPolicy.group(listOf("x".repeat(8000))) }
}
