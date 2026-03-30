package com.nexalarm.app.util

/**
 * 免費 / 付費版功能開關
 *
 * 免費版（一般使用者）：
 *   - 最多 FREE_ALARM_LIMIT  個鬧鐘
 *   - 最多 FREE_FOLDER_LIMIT 個自訂資料夾
 *   - 基本雲端同步、計時器、碼錶、AI 整合
 *
 * 付費版（重度使用者）：
 *   - 無限鬧鐘 + 無限資料夾
 *   - 完整雲端備份與還原
 *   - 優先客服支援
 */
object FeatureFlags {
    var isPremium: Boolean = false

    const val FREE_FOLDER_LIMIT = 10
    const val FREE_ALARM_LIMIT  = 30

    fun canCreateFolder(currentUserFolderCount: Int): Boolean =
        isPremium || currentUserFolderCount < FREE_FOLDER_LIMIT

    fun canCreateAlarm(currentAlarmCount: Int): Boolean =
        isPremium || currentAlarmCount < FREE_ALARM_LIMIT
}
