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
}
