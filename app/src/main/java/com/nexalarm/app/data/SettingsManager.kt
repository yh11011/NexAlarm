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
}
