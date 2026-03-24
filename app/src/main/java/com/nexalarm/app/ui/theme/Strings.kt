package com.nexalarm.app.ui.theme

object S {
    // Navigation & Screen titles
    val home: String get() = if (isAppEnglish) "Home" else "首頁"
    val alarm: String get() = if (isAppEnglish) "Alarm" else "鬧鐘"
    val folders: String get() = if (isAppEnglish) "Folders" else "資料夾"
    val stopwatch: String get() = if (isAppEnglish) "Stopwatch" else "碼錶"
    val timer: String get() = if (isAppEnglish) "Timer" else "計時"
    val settings: String get() = if (isAppEnglish) "Settings" else "設定"
    val account: String get() = if (isAppEnglish) "Account" else "帳號"
    val menu: String get() = if (isAppEnglish) "Menu" else "選單"
    val back: String get() = if (isAppEnglish) "Back" else "返回"

    // Alarm screen
    val single: String get() = if (isAppEnglish) "Single" else "單次"
    val repeat: String get() = if (isAppEnglish) "Repeat" else "多次"
    val noSingleAlarms: String get() = if (isAppEnglish) "No single alarms" else "尚無單次鬧鐘"
    val noRepeatAlarms: String get() = if (isAppEnglish) "No repeat alarms" else "尚無重複鬧鐘"
    val tapPlusToAdd: String get() = if (isAppEnglish) "Tap + to add" else "點擊 + 新增"

    // Alarm edit screen
    val editAlarm: String get() = if (isAppEnglish) "Edit Alarm" else "編輯鬧鐘"
    val newAlarm: String get() = if (isAppEnglish) "New Alarm" else "新增鬧鐘"
    val save: String get() = if (isAppEnglish) "Save" else "儲存"
    val folderLabel: String get() = if (isAppEnglish) "Folder" else "資料夾"
    val repeatDaysLabel: String get() = if (isAppEnglish) "Repeat days" else "重複日"
    val snoozeLabel: String get() = if (isAppEnglish) "Snooze" else "貪睡"
    val snoozeSubtitle: String get() = if (isAppEnglish) "Delay alarm" else "延後響鈴"
    val snoozeIntervalLabel: String get() = if (isAppEnglish) "Snooze Interval" else "貪睡間隔"
    val vibrateOnlyLabel: String get() = if (isAppEnglish) "Vibrate Only" else "僅震動"
    val vibrateOnlySubtitle: String get() = if (isAppEnglish) "Silent but vibrate" else "靜音時仍震動"
    val deleteAlarmLabel: String get() = if (isAppEnglish) "Delete Alarm" else "刪除鬧鐘"
    val keepAfterRingingLabel: String get() = if (isAppEnglish) "Keep After Ringing" else "響鈴後保留"
    val keepAfterRingingSubtitle: String get() = if (isAppEnglish) "Don't delete one-time alarms" else "單次鬧鐘不自動刪除"
    val maxSnoozeCountLabel: String get() = if (isAppEnglish) "Max Snooze" else "最多貪睡"
    val unlimited: String get() = if (isAppEnglish) "Unlimited" else "無限制"
    fun times(n: Int): String = if (isAppEnglish) "$n×" else "$n 次"
    val addOneMinute: String get() = if (isAppEnglish) "+1 min" else "+1 分"
    val noneLabel: String get() = if (isAppEnglish) "None" else "無"
    val alarmTitleHint: String get() = if (isAppEnglish) "Alarm title (optional)" else "鬧鐘標題（選填）"
    val hourFullLabel: String get() = if (isAppEnglish) "Hour" else "小時"
    val minuteFullLabel: String get() = if (isAppEnglish) "Minute" else "分鐘"
    fun minutesSuffix(min: Int): String = if (isAppEnglish) "$min min" else "$min 分鐘"

    // Alarm ringing screen
    fun snoozeReminder(min: Int): String = if (isAppEnglish) "Remind in $min min" else "${min}分鐘後提醒"
    val slideToClose: String get() = if (isAppEnglish) "Slide up to dismiss" else "滑動關閉鬧鐘"

    // Alarm service & notification
    val alarmDefaultTitle: String get() = if (isAppEnglish) "Alarm" else "鬧鐘"
    val alarmRinging: String get() = if (isAppEnglish) "Alarm ringing" else "鬧鐘響鈴中"
    val snoozeAction: String get() = if (isAppEnglish) "Snooze" else "延後"
    val dismissAction: String get() = if (isAppEnglish) "Dismiss" else "關閉"

    // Timer
    val tapToEdit: String get() = if (isAppEnglish) "Tap to edit" else "點擊編輯"
    val timeUp: String get() = if (isAppEnglish) "Time's up!" else "時間到！"
    val setTime: String get() = if (isAppEnglish) "Set Time" else "設定時間"
    val hourLabel: String get() = if (isAppEnglish) "H" else "時"
    val minuteLabel: String get() = if (isAppEnglish) "M" else "分"
    val secondLabel: String get() = if (isAppEnglish) "S" else "秒"
    val confirm: String get() = if (isAppEnglish) "Confirm" else "確認"
    val cancel: String get() = if (isAppEnglish) "Cancel" else "取消"

    // Stopwatch
    val lap: String get() = if (isAppEnglish) "Lap" else "圈次"
    val reset: String get() = if (isAppEnglish) "Reset" else "重置"
    val start: String get() = if (isAppEnglish) "Start" else "開始"
    val pause: String get() = if (isAppEnglish) "Pause" else "暫停"

    // Settings
    val language: String get() = if (isAppEnglish) "Language" else "語言"
    val theme: String get() = if (isAppEnglish) "Theme" else "主題"
    val darkMode: String get() = if (isAppEnglish) "Dark" else "深色"
    val lightMode: String get() = if (isAppEnglish) "Light" else "淺色"

    // Folders
    val newFolder: String get() = if (isAppEnglish) "New Folder" else "新增資料夾"
    val folderLimitReached: String get() = if (isAppEnglish) "Folder limit reached. Upgrade to Premium for unlimited folders." else "資料夾已達上限，升級付費版可建立無限資料夾"
    fun alarmCount(count: Int): String = if (isAppEnglish) "$count alarms" else "$count 個鬧鐘"
    fun folderQuota(used: Int, max: Int): String =
        if (isAppEnglish) "Free: $used / $max folders" else "免費版：已用 $used / $max 個資料夾"
    fun timerPreset(mins: Int): String = if (isAppEnglish) "$mins min" else "$mins 分"

    fun totalDuration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (isAppEnglish) {
            when {
                hours > 0 && minutes > 0 -> "Total: ${hours}h ${minutes}m"
                hours > 0 -> "Total: ${hours}h"
                else -> "Total: ${minutes}m"
            }
        } else {
            when {
                hours > 0 && minutes > 0 -> "共${hours}小時${minutes}分鐘"
                hours > 0 -> "共${hours}小時"
                else -> "共${minutes}分鐘"
            }
        }
    }

    // Home screen
    val homeNextAlarm: String get() = if (isAppEnglish) "Next alarm" else "下一個鬧鐘"
    val homeNoActiveAlarm: String get() = if (isAppEnglish) "No alarms set" else "尚未設定鬧鐘"
    fun homeActiveCount(n: Int): String = if (isAppEnglish) "$n active" else "$n 個已啟用"
    val goToAlarms: String get() = if (isAppEnglish) "View all alarms" else "查看所有鬧鐘"

    // Account screen
    val currentPlan: String get() = if (isAppEnglish) "Current Plan" else "目前方案"
    val freePlan: String get() = if (isAppEnglish) "Free" else "免費版"
    val premiumPlan: String get() = if (isAppEnglish) "Premium" else "付費版"
    val upgradeToPremium: String get() = if (isAppEnglish) "Upgrade to Premium" else "升級付費版"
    val deactivatePremium: String get() = if (isAppEnglish) "Deactivate Premium" else "停用付費版"
    val unlimitedFolders: String get() = if (isAppEnglish) "Unlimited folders" else "無限資料夾"
    val premiumFeatures: String get() = if (isAppEnglish) "Premium features" else "付費版功能"

    // Other
    val welcomeNexAlarm: String get() = if (isAppEnglish) "Welcome to NexAlarm" else "歡迎使用 NexAlarm"
}
