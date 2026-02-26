package com.nexalarm.app.test

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 測試報告生成器
 *
 * 輸出：
 *   1. CSV 原始數據
 *   2. 摘要報告（含 P50/P90/P95/P99、失敗分類、場景對比）
 */
class TestReportGenerator(
    private val context: Context,
    private val results: List<AlarmTestResult>,
    private val isEmulator: Boolean
) {
    private val TAG = "TestReport"
    private val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    fun generate() {
        if (results.isEmpty()) {
            Log.w(TAG, "無測試結果可匯出")
            return
        }
        exportCsv()
        exportSummary()
    }

    private fun exportCsv() {
        val file = File(context.getExternalFilesDir(null), "alarm_raw_$timestamp.csv")
        FileWriter(file).use { w ->
            w.write(AlarmTestResult.csvHeader() + "\n")
            results.forEach { w.write(it.toCsvRow() + "\n") }
        }
        Log.d(TAG, "📄 CSV: ${file.absolutePath}")
    }

    private fun exportSummary() {
        val file = File(context.getExternalFilesDir(null), "alarm_report_$timestamp.txt")
        val report = buildReport()
        FileWriter(file).use { it.write(report) }
        Log.d(TAG, "📋 Report: ${file.absolutePath}")
        // 同時印到 logcat
        report.lines().forEach { Log.i(TAG, it) }
    }

    private fun buildReport(): String = buildString {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        appendLine("╔═══════════════════════════════════════════════════════════╗")
        appendLine("║     NexAlarm 鬧鐘可靠性自動化測試報告                      ║")
        appendLine("║     $now                                    ║")
        appendLine("╠═══════════════════════════════════════════════════════════╣")
        appendLine()

        // ===== 裝置資訊 =====
        appendLine("📋 裝置資訊:")
        appendLine("  製造商:  ${Build.MANUFACTURER}")
        appendLine("  型號:    ${Build.MODEL}")
        appendLine("  Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("  類型:    ${if (isEmulator) "模擬器 ⚠️" else "真機"}")
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val battOpt = !pm.isIgnoringBatteryOptimizations(context.packageName)
        appendLine("  電池優化: ${if (battOpt) "開啟 ⚠️" else "已關閉 ✅"}")
        appendLine()

        // ===== 成功定義 =====
        appendLine("📏 成功定義 (Level 2):")
        appendLine("  ① 延遲 ≤ 3 秒")
        appendLine("  ② 播放/震動持續 ≥ 5 秒 (Service 存活 ≥ 5s)")
        appendLine("  ③ 無 ANR / crash")
        appendLine("  ④ AudioManager STREAM_ALARM 音量 > 0")
        appendLine("  ⑤ 有通知或全螢幕顯示")
        appendLine()

        // ===== 三層成功率總覽 =====
        appendLine("📊 三層成功率總覽:")
        appendLine("─".repeat(50))
        val l0 = results.count { it.level0Success }
        val l1 = results.count { it.level1Success }
        val l2 = results.count { it.level2Success }
        val n = results.size
        appendLine("  Level 0 (系統層 - Receiver 呼叫):    $l0/$n (${pct(l0, n)})")
        appendLine("  Level 1 (應用層 - Service+播放):     $l1/$n (${pct(l1, n)})")
        appendLine("  Level 2 (使用者層 - 商業標準):       $l2/$n (${pct(l2, n)})")
        appendLine()

        // ===== 各場景 Level 2 成功率 =====
        appendLine("📊 各場景 Level 2 成功率:")
        appendLine("─".repeat(65))
        appendLine(String.format("  %-25s %6s %6s %6s %8s", "場景", "L0", "L1", "L2", "平均延遲"))
        appendLine("─".repeat(65))

        val grouped = results.groupBy { it.scenario }
        grouped.forEach { (scenario, items) ->
            val s0 = items.count { it.level0Success }
            val s1 = items.count { it.level1Success }
            val s2 = items.count { it.level2Success }
            val avgDelay = items.filter { it.delayMs >= 0 }.map { it.delayMs }.average()
                .let { if (it.isNaN()) -1.0 else it }
            val delayStr = if (avgDelay >= 0) "${avgDelay.toLong()}ms" else "N/A"
            appendLine(String.format(
                "  %-25s %4s %4s %4s %8s",
                scenario,
                "${s0}/${items.size}",
                "${s1}/${items.size}",
                "${s2}/${items.size}",
                delayStr
            ))
        }
        appendLine("─".repeat(65))
        appendLine()

        // ===== 失敗類型分析 =====
        appendLine("🔴 失敗類型分析:")
        appendLine("─".repeat(50))
        val failures = results.filter { it.failureType != FailureType.NONE }
        if (failures.isEmpty()) {
            appendLine("  無失敗案例 ✅")
        } else {
            val byType = failures.groupBy { it.failureType }
            FailureType.entries.filter { it != FailureType.NONE }.forEach { type ->
                val count = byType[type]?.size ?: 0
                if (count > 0) {
                    val desc = when (type) {
                        FailureType.F1_NO_TRIGGER -> "完全沒觸發"
                        FailureType.F2_EXCESSIVE_DELAY -> "延遲 > 10s"
                        FailureType.F3_NO_SOUND -> "有觸發但無聲音"
                        FailureType.F4_CRASH -> "觸發後 crash / Service 存活 < 5s"
                        FailureType.F5_SYSTEM_DEFERRED -> "系統延後 (3-10s)"
                        else -> ""
                    }
                    appendLine("  ${type.name}: $count 次 — $desc")
                }
            }
        }
        appendLine()

        // ===== 延遲統計 (Percentile) =====
        val validDelays = results.filter { it.delayMs >= 0 }.map { it.delayMs }.sorted()
        if (validDelays.isNotEmpty()) {
            appendLine("⏱ 延遲分布 (Percentile):")
            appendLine("─".repeat(50))
            appendLine("  最小:    ${validDelays.first()}ms")
            appendLine("  P50:     ${percentile(validDelays, 50)}ms")
            appendLine("  P90:     ${percentile(validDelays, 90)}ms")
            appendLine("  P95:     ${percentile(validDelays, 95)}ms")
            appendLine("  P99:     ${percentile(validDelays, 99)}ms")
            appendLine("  最大:    ${validDelays.last()}ms")
            appendLine("  平均:    ${"%.1f".format(validDelays.average())}ms")
            appendLine("  標準差:  ${"%.1f".format(stdDev(validDelays))}ms")
            appendLine("  Tail Ratio (P99/P50): ${"%.2f".format(
                percentile(validDelays, 99).toDouble() / maxOf(percentile(validDelays, 50), 1)
            )}")
            appendLine()
            appendLine("  延遲分級:")
            appendLine("    ≤1s (完美):    ${validDelays.count { it <= 1000 }} (${pct(validDelays.count { it <= 1000 }, validDelays.size)})")
            appendLine("    1-3s (可接受): ${validDelays.count { it in 1001..3000 }} (${pct(validDelays.count { it in 1001..3000 }, validDelays.size)})")
            appendLine("    3-10s (邊緣):  ${validDelays.count { it in 3001..10000 }} (${pct(validDelays.count { it in 3001..10000 }, validDelays.size)})")
            appendLine("    >10s (失敗):   ${validDelays.count { it > 10000 }} (${pct(validDelays.count { it > 10000 }, validDelays.size)})")
            appendLine()
        }

        // ===== Service 存活時間 =====
        val aliveValues = results.filter { it.serviceAliveSeconds > 0 }.map { it.serviceAliveSeconds }
        if (aliveValues.isNotEmpty()) {
            appendLine("🔧 Service 存活時間:")
            appendLine("─".repeat(50))
            appendLine("  平均: ${"%.1f".format(aliveValues.average())}s")
            appendLine("  最小: ${"%.1f".format(aliveValues.min())}s")
            appendLine("  最大: ${"%.1f".format(aliveValues.max())}s")
            appendLine("  < 5s (失敗): ${aliveValues.count { it < 5.0 }}")
            appendLine()
        }

        // ===== 功能驗證 =====
        val triggered = results.filter { it.level0Success }
        if (triggered.isNotEmpty()) {
            appendLine("📱 功能驗證 (僅計算已觸發的):")
            appendLine("─".repeat(50))
            val notif = triggered.count { it.fullScreenTime > 0 || it.serviceStartTime > 0 }
            appendLine("  通知/全螢幕顯示: $notif/${triggered.size} (${pct(notif, triggered.size)})")
            val sound = triggered.count { it.mediaPlayTime > 0 }
            appendLine("  鈴聲播放:        $sound/${triggered.size} (${pct(sound, triggered.size)})")
            val volOk = triggered.filter { it.mediaPlayTime > 0 }.count { it.streamVolume > 0 }
            val volTotal = triggered.count { it.mediaPlayTime > 0 }
            appendLine("  音量 > 0:        $volOk/$volTotal (${pct(volOk, maxOf(volTotal, 1))})")
            appendLine()
        }

        // ===== 商業目標達成 =====
        appendLine("🎯 商業目標達成:")
        appendLine("─".repeat(50))
        grouped.forEach { (scenario, items) ->
            val rate = items.count { it.level2Success }.toDouble() / items.size * 100
            val target = when {
                scenario.contains("Doze") -> 99.0
                scenario.contains("低電量") -> 99.0
                scenario.contains("重開機") -> 100.0
                scenario.contains("force", ignoreCase = true) -> -1.0 // 不列入
                else -> 100.0
            }
            val status = when {
                target < 0 -> "⚠️ 系統設計限制，不列入"
                rate >= target -> "✅ 達標 (${"%.1f".format(rate)}% ≥ ${"%.0f".format(target)}%)"
                else -> "❌ 未達標 (${"%.1f".format(rate)}% < ${"%.0f".format(target)}%)"
            }
            appendLine("  $scenario: $status")
        }
        appendLine()

        // ===== 注意事項 =====
        appendLine("⚠️ 注意事項:")
        if (isEmulator) {
            appendLine("  • ⚠️ 此測試在模擬器上執行，Doze / 螢幕行為可能不準確")
        }
        appendLine("  • Force-stop 屬於系統設計限制，不列入成功率統計")
        appendLine("  • setAlarmClock() 會 bypass Doze，報告已標註使用的 API")
        appendLine("  • 電池優化開啟時 setExactAndAllowWhileIdle 有最短 9 分鐘間隔")
        appendLine()
        appendLine("╚═══════════════════════════════════════════════════════════╝")
    }

    // ===== 統計工具 =====

    private fun pct(count: Int, total: Int): String =
        if (total > 0) "${"%.1f".format(count.toDouble() / total * 100)}%" else "N/A"

    private fun percentile(sorted: List<Long>, p: Int): Long {
        if (sorted.isEmpty()) return 0L
        val idx = (p / 100.0 * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
        return sorted[idx]
    }

    private fun stdDev(values: List<Long>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}

