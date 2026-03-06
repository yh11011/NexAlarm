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

    // Alarm screen
    val single: String get() = if (isAppEnglish) "Single" else "單次"
    val repeat: String get() = if (isAppEnglish) "Repeat" else "多次"
    val noSingleAlarms: String get() = if (isAppEnglish) "No single alarms" else "尚無單次鬧鐘"
    val noRepeatAlarms: String get() = if (isAppEnglish) "No repeat alarms" else "尚無重複鬧鐘"
    val tapPlusToAdd: String get() = if (isAppEnglish) "Tap + to add" else "點擊 + 新增"

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

    // Other
    val welcomeNexAlarm: String get() = if (isAppEnglish) "Welcome to NexAlarm" else "歡迎使用 NexAlarm"
    val accountPage: String get() = if (isAppEnglish) "Account Page" else "帳號頁面"
}
