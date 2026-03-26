package com.nexalarm.app.data

import android.content.Context

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("nexalarm_settings", Context.MODE_PRIVATE)

    var isDarkMode: Boolean
        get() = prefs.getBoolean("dark_mode", true)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    var isEnglish: Boolean
        get() = prefs.getBoolean("is_english", false)
        set(value) = prefs.edit().putBoolean("is_english", value).apply()

    var isPremium: Boolean
        get() = prefs.getBoolean("is_premium", false)
        set(value) = prefs.edit().putBoolean("is_premium", value).apply()

    var hasRequestedExactAlarmPerm: Boolean
        get() = prefs.getBoolean("requested_exact_alarm", false)
        set(value) = prefs.edit().putBoolean("requested_exact_alarm", value).apply()

    var hasShownBatteryOptDialog: Boolean
        get() = prefs.getBoolean("shown_battery_opt", false)
        set(value) = prefs.edit().putBoolean("shown_battery_opt", value).apply()

    // 首次安裝標記：true = 尚未完成首次登入引導
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("is_first_launch", true)
        set(value) = prefs.edit().putBoolean("is_first_launch", value).apply()

    // Auth token（JWT）
    var authToken: String?
        get() = prefs.getString("auth_token", null)
        set(value) = prefs.edit().putString("auth_token", value).apply()

    // 登入使用者資訊
    var authUserId: Int
        get() = prefs.getInt("auth_user_id", -1)
        set(value) = prefs.edit().putInt("auth_user_id", value).apply()

    var authUsername: String?
        get() = prefs.getString("auth_username", null)
        set(value) = prefs.edit().putString("auth_username", value).apply()

    var authDisplayName: String?
        get() = prefs.getString("auth_display_name", null)
        set(value) = prefs.edit().putString("auth_display_name", value).apply()

    fun clearAuth() {
        prefs.edit()
            .remove("auth_token")
            .remove("auth_user_id")
            .remove("auth_username")
            .remove("auth_display_name")
            .apply()
    }
}
