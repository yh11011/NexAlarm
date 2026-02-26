package com.nexalarm.app.test

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.nexalarm.app.util.AlarmTestHook
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * NexAlarm 鬧鐘可靠性自動化測試
 *
 * 成功定義 (Level 2 — 使用者層)：
 *   ① 延遲 ≤ 3 秒
 *   ② 播放持續 ≥ 5 秒 (Service 存活 ≥ 5s)
 *   ③ 無 ANR / crash
 *   ④ AudioManager STREAM_ALARM 音量 > 0
 *   ⑤ 有通知或全螢幕顯示
 *
 * 失敗分類：
 *   F1 — 完全沒觸發
 *   F2 — 延遲 > 10 秒
 *   F3 — 有觸發但無聲音
 *   F4 — 觸發後立刻 crash
 *   F5 — 被系統延後 (3-10s)
 *
 * 報告包含：
 *   P50 / P90 / P95 / P99 / 標準差 / Tail Ratio
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AlarmReliabilityTest {

    private lateinit var context: Context
    private lateinit var device: UiDevice
    private lateinit var monitor: ReliableAlarmMonitor
    private val results = mutableListOf<AlarmTestResult>()
    private var isEmulator = false

    companion object {
        private const val TAG = "AlarmReliabilityTest"
        private const val REPEAT = 5                 // 每場景重複次數
        private const val ALARM_DELAY_MS = 30_000L   // 鬧鐘排在 30 秒後
        private const val WAIT_TIMEOUT_SEC = 90L     // 最長等待觸發秒數
        private const val POST_TRIGGER_WAIT = 8000L  // 觸發後等 8 秒驗證 Level 2

        private const val ACTION_TRIGGER = "com.nexalarm.app.ALARM_TRIGGER"
        private const val PKG = "com.nexalarm.app"
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        monitor = ReliableAlarmMonitor(context)
        isEmulator = detectEmulator()

        // 確保 STREAM_ALARM 音量 > 0
        ensureAlarmVolume()

        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "裝置: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.d(TAG, "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.d(TAG, "模擬器: $isEmulator")
        Log.d(TAG, "═══════════════════════════════════════")
    }

    @After
    fun teardown() {
        monitor.stopMonitoring()
        // 生成報告
        TestReportGenerator(context, results, isEmulator).generate()
    }

    // ==================== 測試場景 ====================

    /**
     * 場景 1: 螢幕開啟 — 基本鬧鐘（最基礎的正確性測試）
     */
    @Test
    fun test01_ScreenOn_BasicAlarm() {
        runScenario(
            scenarioName = "螢幕開啟_基本鬧鐘",
            deviceState = "screen_on",
            setup = {
                device.wakeUp()
                Thread.sleep(1000)
            },
            cleanup = {}
        )
    }

    /**
     * 場景 2: 螢幕關閉
     */
    @Test
    fun test02_ScreenOff() {
        runScenario(
            scenarioName = "螢幕關閉",
            deviceState = "screen_off",
            setup = {
                device.wakeUp()
                Thread.sleep(2000)
                device.sleep()
                Thread.sleep(2000)
            },
            cleanup = { device.wakeUp() }
        )
    }

    /**
     * 場景 3: 裝置鎖定
     */
    @Test
    fun test03_DeviceLocked() {
        runScenario(
            scenarioName = "裝置鎖定",
            deviceState = "locked",
            setup = {
                device.wakeUp()
                Thread.sleep(1000)
                device.sleep()
                Thread.sleep(3000)
            },
            cleanup = { device.wakeUp() }
        )
    }

    /**
     * 場景 4: Synthetic Doze（僅真機，且明確標註為強制 Doze）
     */
    @Test
    fun test04_SyntheticDoze() {
        if (isEmulator) {
            Log.w(TAG, "⚠️ 跳過 Doze：模擬器行為不可靠")
            addSkipped("Synthetic_Doze", "doze_synthetic", "模擬器不支援")
            return
        }

        runScenario(
            scenarioName = "Synthetic_Doze",
            deviceState = "doze_synthetic",
            alarmDelayMs = 60_000L,
            waitTimeout = 120L,
            setup = {
                device.wakeUp()
                Thread.sleep(2000)
                device.sleep()
                Thread.sleep(2000)
                device.executeShellCommand("dumpsys deviceidle enable")
                device.executeShellCommand("dumpsys deviceidle force-idle")
                Thread.sleep(5000)
                val state = device.executeShellCommand("dumpsys deviceidle get deep").trim()
                Log.d(TAG, "Doze 狀態: $state")
            },
            cleanup = {
                device.executeShellCommand("dumpsys deviceidle unforce")
                device.executeShellCommand("dumpsys deviceidle disable")
                device.wakeUp()
                Thread.sleep(2000)
            }
        )
    }

    /**
     * 場景 5: 短間隔連續鬧鐘（測試 PendingIntent 覆蓋問題）
     */
    @Test
    fun test05_RapidSuccessiveAlarms() {
        val scenarioName = "短間隔連續鬧鐘"
        Log.d(TAG, "=== 開始: $scenarioName ===")

        device.wakeUp()
        Thread.sleep(1000)

        val totalAlarms = 5
        val baseTime = System.currentTimeMillis() + 20_000L

        for (i in 0 until totalAlarms) {
            val alarmId = (7000 + i).toLong()
            val triggerTime = baseTime + (15_000L * i)

            // 清除舊數據
            AlarmTestHook.clearForAlarm(context, alarmId)

            // 排定鬧鐘
            scheduleTestAlarm(triggerTime, alarmId)
            Log.d(TAG, "排定連續鬧鐘 #$i: ID=$alarmId, ${fmtTime(triggerTime)}")
        }

        // 依序等待驗證
        for (i in 0 until totalAlarms) {
            val alarmId = (7000 + i).toLong()
            val expectedTime = baseTime + (15_000L * i)

            monitor.startMonitoring(alarmId)

            val timeToWait = maxOf((expectedTime - System.currentTimeMillis() + 30_000L) / 1000, 15L)
            val data = monitor.waitForResult(expectedTime, timeToWait, POST_TRIGGER_WAIT)
            val result = buildResult("連續鬧鐘_${i + 1}", scenarioName, i + 1, expectedTime, data, "screen_on")
            results.add(result)
            logResult(result)

            // 關閉鬧鐘
            dismissAlarm(alarmId)
            monitor.stopMonitoring()
            Thread.sleep(2000)
        }
    }

    /**
     * 場景 6: Process 被殺（使用 am kill，不用 force-stop）
     *
     * ⚠️ 不使用 force-stop 因為它會殺掉 instrumentation runner。
     * am kill 只殺 background process，已排定的 AlarmManager 不受影響。
     */
    @Test
    fun test06_ProcessKilled() {
        val scenarioName = "Process被回收"
        Log.d(TAG, "=== 開始: $scenarioName ===")

        for (i in 1..REPEAT) {
            val alarmId = (9000 + i).toLong()
            val scheduledTime = System.currentTimeMillis() + 60_000L

            AlarmTestHook.clearForAlarm(context, alarmId)
            scheduleTestAlarm(scheduledTime, alarmId)
            Log.d(TAG, "排定 alarm $alarmId, 60s 後觸發")

            // 等 3 秒後殺掉背景 process
            Thread.sleep(3000)
            device.executeShellCommand("am kill $PKG")
            Log.d(TAG, "已執行 am kill（僅殺背景 process）")
            Thread.sleep(5000)

            monitor.startMonitoring(alarmId)
            val remaining = maxOf((scheduledTime - System.currentTimeMillis() + 30_000L) / 1000, 30L)
            val data = monitor.waitForResult(scheduledTime, remaining, POST_TRIGGER_WAIT)
            val result = buildResult("Process回收_$i", scenarioName, i, scheduledTime, data, "process_killed")
            results.add(result)
            logResult(result)

            dismissAlarm(alarmId)
            monitor.stopMonitoring()
            Thread.sleep(5000)
        }
    }

    /**
     * 場景 7: 用 kill -9 模擬非正常終止（更狠）
     */
    @Test
    fun test07_HardKill() {
        val scenarioName = "kill-9_非正常終止"
        Log.d(TAG, "=== 開始: $scenarioName ===")

        for (i in 1..REPEAT) {
            val alarmId = (8000 + i).toLong()
            val scheduledTime = System.currentTimeMillis() + 60_000L

            AlarmTestHook.clearForAlarm(context, alarmId)
            scheduleTestAlarm(scheduledTime, alarmId)
            Log.d(TAG, "排定 alarm $alarmId, 60s 後觸發")

            Thread.sleep(3000)

            // 取得 app PID 然後 kill -9
            val pid = device.executeShellCommand("pidof $PKG").trim()
            if (pid.isNotEmpty()) {
                Log.d(TAG, "kill -9 pid=$pid")
                device.executeShellCommand("kill -9 $pid")
            } else {
                Log.w(TAG, "找不到 $PKG 的 PID")
            }
            Thread.sleep(5000)

            monitor.startMonitoring(alarmId)
            val remaining = maxOf((scheduledTime - System.currentTimeMillis() + 30_000L) / 1000, 30L)
            val data = monitor.waitForResult(scheduledTime, remaining, POST_TRIGGER_WAIT)
            val result = buildResult("HardKill_$i", scenarioName, i, scheduledTime, data, "hard_killed")
            results.add(result)
            logResult(result)

            dismissAlarm(alarmId)
            monitor.stopMonitoring()
            Thread.sleep(5000)
        }
    }

    /**
     * 場景 8: Alarm Queue 驗證
     * 排程後立刻用 dumpsys alarm 確認是否進入 RTC_WAKEUP 佇列
     */
    @Test
    fun test08_AlarmQueueVerification() {
        val scenarioName = "AlarmQueue驗證"
        Log.d(TAG, "=== 開始: $scenarioName ===")

        device.wakeUp()
        Thread.sleep(1000)

        for (i in 1..REPEAT) {
            val alarmId = (6000 + i).toLong()
            val scheduledTime = System.currentTimeMillis() + 45_000L

            AlarmTestHook.clearForAlarm(context, alarmId)

            // 排程前 dump
            val beforeDump = device.executeShellCommand("dumpsys alarm | grep $PKG").trim()
            Log.d(TAG, "排程前 alarm queue 筆數: ${beforeDump.lines().size}")

            // 排程
            scheduleTestAlarm(scheduledTime, alarmId)

            // 排程後 dump
            Thread.sleep(1000)
            val afterDump = device.executeShellCommand("dumpsys alarm | grep $PKG").trim()
            Log.d(TAG, "排程後 alarm queue 筆數: ${afterDump.lines().size}")

            val inQueue = afterDump.contains("RTC_WAKEUP") || afterDump.contains("ELAPSED_WAKEUP")
                    || afterDump.lines().size > beforeDump.lines().size
            Log.d(TAG, "鬧鐘是否在 queue 中: $inQueue")

            // 等待觸發
            monitor.startMonitoring(alarmId)
            val remaining = maxOf((scheduledTime - System.currentTimeMillis() + 30_000L) / 1000, 30L)
            val data = monitor.waitForResult(scheduledTime, remaining, POST_TRIGGER_WAIT)

            // 觸發後再 dump 確認已移除
            Thread.sleep(1000)
            val afterTriggerDump = device.executeShellCommand("dumpsys alarm | grep $PKG").trim()
            Log.d(TAG, "觸發後 alarm queue 筆數: ${afterTriggerDump.lines().size}")

            val result = buildResult("Queue驗證_$i", scenarioName, i, scheduledTime, data, "screen_on")
            results.add(result)
            logResult(result)

            dismissAlarm(alarmId)
            monitor.stopMonitoring()
            Thread.sleep(3000)
        }
    }

    // ==================== 核心輔助方法 ====================

    /**
     * 通用場景執行器
     */
    private fun runScenario(
        scenarioName: String,
        deviceState: String,
        alarmDelayMs: Long = ALARM_DELAY_MS,
        waitTimeout: Long = WAIT_TIMEOUT_SEC,
        setup: () -> Unit,
        cleanup: () -> Unit
    ) {
        Log.d(TAG, "=== 開始場景: $scenarioName ===")

        for (i in 1..REPEAT) {
            Log.d(TAG, "--- 第 $i / $REPEAT 次 ---")

            val alarmId = (scenarioName.hashCode().and(0xFFFF) + i).toLong()

            setup()
            AlarmTestHook.clearForAlarm(context, alarmId)
            monitor.startMonitoring(alarmId)

            val scheduledTime = System.currentTimeMillis() + alarmDelayMs
            scheduleTestAlarm(scheduledTime, alarmId)
            Log.d(TAG, "排定: alarm=$alarmId at ${fmtTime(scheduledTime)}")

            val data = monitor.waitForResult(scheduledTime, waitTimeout, POST_TRIGGER_WAIT)
            val result = buildResult("${scenarioName}_$i", scenarioName, i, scheduledTime, data, deviceState)
            results.add(result)
            logResult(result)

            dismissAlarm(alarmId)
            monitor.stopMonitoring()
            cleanup()
            Thread.sleep(5000)
        }

        val successes = results.filter { it.scenario == scenarioName }.count { it.level2Success }
        Log.d(TAG, "=== 場景 $scenarioName 完成: Level2 成功 $successes/$REPEAT ===")
    }

    /**
     * 排程測試鬧鐘
     * 使用 setAlarmClock()（商業級鬧鐘的正確做法）
     */
    private fun scheduleTestAlarm(triggerTime: Long, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_TRIGGER).apply {
            setPackage(PKG)
            putExtra("alarm_id", alarmId)
            putExtra("alarm_title", "Test_$alarmId")
            putExtra("alarm_vibrate_only", false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 商業鬧鐘必用 setAlarmClock (可 bypass Doze)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
            pendingIntent
        )
    }

    /**
     * 關閉鬧鐘（透過 Intent 發送 DISMISS）
     */
    private fun dismissAlarm(alarmId: Long) {
        try {
            val intent = Intent("com.nexalarm.app.ALARM_DISMISS").apply {
                setPackage(PKG)
                putExtra("alarm_id", alarmId)
            }
            context.sendBroadcast(intent)
            Thread.sleep(1000)
        } catch (e: Exception) {
            Log.w(TAG, "dismissAlarm failed: ${e.message}")
        }
    }

    /**
     * 從 CollectedData 建立 AlarmTestResult
     */
    private fun buildResult(
        testCase: String,
        scenario: String,
        iteration: Int,
        scheduledTime: Long,
        data: ReliableAlarmMonitor.CollectedData,
        deviceState: String
    ): AlarmTestResult {
        return AlarmTestResult(
            testCase = testCase,
            scenario = scenario,
            iteration = iteration,
            scheduledTime = scheduledTime,
            receiverTime = data.receiverTime,
            serviceStartTime = data.serviceStartTime,
            mediaPlayTime = data.mediaPlayTime,
            fullScreenTime = data.fullScreenTime,
            serviceLastAliveTime = data.serviceLastAliveTime,
            delayMs = data.delayMs,
            serviceAliveSeconds = data.serviceAliveSec,
            streamVolume = data.streamVolume,
            crashInfo = data.crashInfo,
            level0Success = data.level0,
            level1Success = data.level1,
            level2Success = data.level2,
            failureType = data.failureType,
            deviceState = deviceState,
            isRealDevice = !isEmulator
        )
    }

    /**
     * 確保 STREAM_ALARM 音量 > 0
     */
    private fun ensureAlarmVolume() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = am.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (current == 0) {
            am.setStreamVolume(AudioManager.STREAM_ALARM, max / 2, 0)
            Log.d(TAG, "STREAM_ALARM 音量設為 ${max / 2}/$max")
        } else {
            Log.d(TAG, "STREAM_ALARM 音量: $current/$max ✅")
        }
    }

    private fun detectEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.PRODUCT.contains("sdk_gphone") ||
                Build.PRODUCT.contains("emulator") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")
    }

    private fun addSkipped(scenario: String, state: String, reason: String) {
        results.add(AlarmTestResult(
            testCase = "${scenario}_SKIPPED",
            scenario = scenario,
            iteration = 0,
            scheduledTime = 0L,
            receiverTime = 0L,
            serviceStartTime = 0L,
            mediaPlayTime = 0L,
            fullScreenTime = 0L,
            serviceLastAliveTime = 0L,
            delayMs = -1L,
            serviceAliveSeconds = 0.0,
            streamVolume = -1,
            crashInfo = null,
            level0Success = false,
            level1Success = false,
            level2Success = false,
            failureType = FailureType.F1_NO_TRIGGER,
            deviceState = state,
            isRealDevice = !isEmulator
        ))
    }

    private fun logResult(r: AlarmTestResult) {
        val l2icon = if (r.level2Success) "✅" else "❌"
        Log.d(TAG, buildString {
            appendLine("┌──────────────────────────────────────")
            appendLine("│ ${r.testCase}")
            appendLine("│ Level 0 (Receiver): ${if (r.level0Success) "✅" else "❌"}")
            appendLine("│ Level 1 (Service):  ${if (r.level1Success) "✅" else "❌"}")
            appendLine("│ Level 2 (使用者):   $l2icon")
            appendLine("│ 延遲: ${if (r.delayMs >= 0) "${r.delayMs}ms (${r.delayRating})" else "N/A"}")
            appendLine("│ Service 存活: ${"%.1f".format(r.serviceAliveSeconds)}s")
            appendLine("│ 音量: ${r.streamVolume}")
            appendLine("│ 失敗類型: ${r.failureType}")
            if (r.crashInfo != null) appendLine("│ Crash: ${r.crashInfo}")
            appendLine("└──────────────────────────────────────")
        })
    }

    private fun fmtTime(ms: Long): String =
        java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date(ms))
}

