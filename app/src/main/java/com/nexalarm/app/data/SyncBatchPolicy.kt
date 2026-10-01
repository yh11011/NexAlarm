package com.nexalarm.app.data

/** Stay below the existing server's 10 KB request guard, measured as UTF-8 bytes. */
object SyncBatchPolicy {
    fun group(items: List<String>, limitBytes: Int = 8000): List<List<String>> {
        val batches = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        var size = 0
        for (item in items) {
            val bytes = item.toByteArray(Charsets.UTF_8).size + 1
            require(bytes <= limitBytes) { "An alarm is too large to synchronize" }
            if (size + bytes > limitBytes && current.isNotEmpty()) {
                batches.add(current); current = mutableListOf(); size = 0
            }
            current.add(item); size += bytes
        }
        if (current.isNotEmpty() || batches.isEmpty()) batches.add(current)
        return batches
    }
}
