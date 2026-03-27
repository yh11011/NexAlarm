package com.nexalarm.app.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全域崩潰處理器
 *
 * 功能：
 * - 捕獲所有未處理的例外，寫入 crash log 檔案
 * - Log 檔案存放於 context.filesDir/crash_logs/
 * - 最多保留 10 個 crash log 檔案，超過自動刪除最舊的
 *
 * 整合方式：在 NexAlarmApp.onCreate() 中呼叫 CrashHandler.install(this)
 */
object CrashHandler {

    private const val TAG = "CrashHandler"
    private const val MAX_CRASH_LOGS = 10
    private const val CRASH_LOG_DIR = "crash_logs"

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordException(context, thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "寫入 crash log 失敗", e)
            } finally {
                // 繼續執行系統預設的崩潰處理（顯示系統錯誤對話框）
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        Log.d(TAG, "CrashHandler 已安裝")
    }

    /**
     * 記錄異常到本地檔案
     * 可從 CrashReportingManager 或直接調用
     */
    fun recordException(context: Context, thread: Thread, throwable: Throwable) {
        saveCrashLog(context, thread, throwable)
    }

    private fun saveCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        val crashDir = File(context.filesDir, CRASH_LOG_DIR).apply { mkdirs() }

        // 清理舊 log，只保留最新 MAX_CRASH_LOGS 個
        val existingLogs = crashDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
        if (existingLogs.size >= MAX_CRASH_LOGS) {
            existingLogs.take(existingLogs.size - MAX_CRASH_LOGS + 1).forEach { it.delete() }
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val crashFile = File(crashDir, "crash_$timestamp.txt")

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        val report = buildString {
            appendLine("=== NexAlarm Crash Report ===")
            appendLine("時間：$timestamp")
            appendLine("執行緒：${thread.name}")
            appendLine("裝置：${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android 版本：${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App 版本：${getAppVersion(context)}")
            appendLine()
            appendLine("=== Stack Trace ===")
            appendLine(sw.toString())
        }

        crashFile.writeText(report)
        Log.e(TAG, "Crash log 已寫入：${crashFile.absolutePath}")
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * 取得所有 crash log 檔案列表（最新的在前）
     */
    fun getCrashLogs(context: Context): List<File> {
        val crashDir = File(context.filesDir, CRASH_LOG_DIR)
        return crashDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 清除所有 crash log
     */
    fun clearCrashLogs(context: Context) {
        File(context.filesDir, CRASH_LOG_DIR).listFiles()?.forEach { it.delete() }
    }
}
