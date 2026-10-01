package com.nexalarm.app.data

import android.content.Context

class SyncFailure(val statusCode: Int) : RuntimeException("Synchronization request failed")

object SyncDiagnostics {
    fun save(context: Context, phase: String, success: Boolean = false) {
        val edit = context.getSharedPreferences("sync_diagnostics", Context.MODE_PRIVATE).edit()
            .putString("phase", phase).putLong("attempt_at", System.currentTimeMillis())
        if (success) edit.putLong("success_at", System.currentTimeMillis())
        edit.commit()
    }
}
