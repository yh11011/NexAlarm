package com.nexalarm.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.receiver.AlarmReceiver
import java.util.*

/**
 * 鬧鐘排程管理器
 * 負責使用 AlarmManager 設定精確的鬧鐘觸發時間
 */
class AlarmScheduler(private val context: Context) {
    data class ScheduleResult(val status: String, val triggerAt: Long? = null, val reason: String? = null)

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 排程鬧鐘
     * @param alarm 鬧鐘實體
     */
    private val ledger = com.nexalarm.app.data.ScheduleLedger(context)

    fun schedule(alarm: AlarmEntity): ScheduleResult {
        return try {
            scheduleInternal(alarm).also { ledger.record(alarm, it) }
        } catch (e: Exception) {
            ledger.record(alarm, ScheduleResult("failed", reason = "Android scheduler rejected the alarm"))
            throw e
        }
    }

    private fun scheduleInternal(alarm: AlarmEntity): ScheduleResult {
        // 如果鬧鐘未啟用，取消排程
        if (!alarm.isEnabled) {
            cancel(alarm)
            return ScheduleResult("cancelled")
        }

        // 計算下次觸發時間
        val triggerTime = calculateNextTriggerTime(alarm)
        if (triggerTime <= System.currentTimeMillis()) {
            cancel(alarm)
            return ScheduleResult("failed", reason = "Requested date/time has passed")
        }

        // 建立 PendingIntent
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
            putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE_ONLY, alarm.vibrateOnly)
            putExtra(AlarmReceiver.EXTRA_ALARM_SNOOZE_ENABLED, alarm.snoozeEnabled)
            putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, alarm.volume)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 設定鬧鐘，並記錄實際使用的 API 類型供測試工具解析
        val apiUsed: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // 無精確鬧鐘權限：使用 setAndAllowWhileIdle 作為 fallback（精度較低但鬧鐘不會消失）
            // 權限引導由 MainActivity 在首次啟動時處理，排程器不重複彈出
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            android.util.Log.w("AlarmScheduler",
                "No SCHEDULE_EXACT_ALARM permission, using inexact fallback for alarm ${alarm.id}")
            apiUsed = "setAndAllowWhileIdle"
        } else {
            // 正常路徑：使用 setAlarmClock 可 bypass Doze，是商業鬧鐘的標準做法
            val showIntent = PendingIntent.getActivity(
                context, alarm.id.toInt(),
                Intent(context, com.nexalarm.app.MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, showIntent),
                pendingIntent
            )
            android.util.Log.d("AlarmScheduler",
                "Scheduled alarm ${alarm.id} at ${Date(triggerTime)}")
            apiUsed = "setAlarmClock"
        }

        // [NexAlarmTest] 事件 1/4：鬧鐘已寫入 AlarmManager
        android.util.Log.i("NexAlarmTest",
            "SCHEDULED|id=${alarm.id}|title=${alarm.title}" +
            "|triggerMs=$triggerTime|api=$apiUsed|ts=${System.currentTimeMillis()}")
        return ScheduleResult(if (apiUsed == "setAlarmClock") "scheduled" else "fallback", triggerTime)
    }

    /**
     * 取消鬧鐘排程
     */
    fun cancel(alarm: AlarmEntity) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        ledger.record(alarm, ScheduleResult("cancelled"))

        android.util.Log.d("AlarmScheduler", "Cancelled alarm ${alarm.id}")
        // [NexAlarmTest] 事件 2/4：鬧鐘已從 AlarmManager 移除
        android.util.Log.i("NexAlarmTest",
            "CANCELLED|id=${alarm.id}|ts=${System.currentTimeMillis()}")
    }

    /**
     * 排程貪睡鬧鐘
     */
    fun scheduleSnooze(alarm: AlarmEntity, snoozeMinutes: Int) {
        try {
            val trigger = scheduleSnoozeInternal(alarm, snoozeMinutes)
            ledger.record(alarm, ScheduleResult(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) "fallback" else "scheduled", trigger))
        } catch (e: Exception) {
            ledger.record(alarm, ScheduleResult("failed", reason = "Android scheduler rejected snooze"))
            throw e
        }
    }

    private fun scheduleSnoozeInternal(alarm: AlarmEntity, snoozeMinutes: Int): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // 無精確權限：fallback 到非精確鬧鐘
            val triggerFallback = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
            val intentFallback = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
                putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
                putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE_ONLY, alarm.vibrateOnly)
                putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, alarm.volume)
            }
            val piFallback = PendingIntent.getBroadcast(
                context, alarm.id.toInt(), intentFallback,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerFallback, piFallback)
            return triggerFallback
        }

        val triggerTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
            putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE_ONLY, alarm.vibrateOnly)
            putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, alarm.volume)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 貪睡也用 setAlarmClock，確保 bypass Doze
        val showIntent = PendingIntent.getActivity(
            context, alarm.id.toInt(),
            Intent(context, com.nexalarm.app.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerTime, showIntent),
            pendingIntent
        )

        android.util.Log.d("AlarmScheduler",
            "Snoozed alarm ${alarm.id} for $snoozeMinutes min, fires at ${Date(triggerTime)}")
        return triggerTime
    }

    /**
     * 計算下次觸發時間
     */
    private fun calculateNextTriggerTime(alarm: AlarmEntity): Long {
        return AlarmTimeCalculator.nextTrigger(alarm)
    }

    /**
     * 計算距離下次觸發的時間文字
     * @return 例如 "6小時30分鐘後"
     */
    fun getTimeUntilText(alarm: AlarmEntity, isEnglish: Boolean = false): String {
        if (!alarm.isEnabled) return ""

        val triggerTime = calculateNextTriggerTime(alarm)
        val now = System.currentTimeMillis()
        val diff = triggerTime - now

        if (diff <= 0) return if (isEnglish) "Ringing now" else "即將響鈴"

        val hours = (diff / (1000 * 60 * 60)).toInt()
        val minutes = ((diff % (1000 * 60 * 60)) / (1000 * 60)).toInt()

        return if (isEnglish) {
            when {
                hours > 0 && minutes > 0 -> "in ${hours}h ${minutes}m"
                hours > 0 -> "in ${hours}h"
                minutes > 0 -> "in ${minutes}m"
                else -> "in < 1 min"
            }
        } else {
            when {
                hours > 0 && minutes > 0 -> "${hours}小時${minutes}分鐘後"
                hours > 0 -> "${hours}小時後"
                minutes > 0 -> "${minutes}分鐘後"
                else -> "不到1分鐘"
            }
        }
    }

    /**
     * 取得下次觸發的時間戳記
     */
    fun getNextTriggerTime(alarm: AlarmEntity): Long {
        return if (alarm.isEnabled) {
            calculateNextTriggerTime(alarm)
        } else {
            0L
        }
    }

    companion object {
        /**
         * 快捷方法：建立實例並排程
         */
        fun schedule(context: Context, alarm: AlarmEntity) {
            AlarmScheduler(context).schedule(alarm)
        }

        /**
         * 快捷方法：建立實例並取消
         */
        fun cancel(context: Context, alarm: AlarmEntity) {
            AlarmScheduler(context).cancel(alarm)
        }

        /**
         * 快捷方法：建立實例並排程貪睡
         */
        fun scheduleSnooze(context: Context, alarm: AlarmEntity, snoozeMinutes: Int) {
            AlarmScheduler(context).scheduleSnooze(alarm, snoozeMinutes)
        }
    }
}
