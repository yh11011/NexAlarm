package com.nexalarm.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SettingsManager(context: Context) {
    // 一般設定（非敏感，使用普通 SharedPreferences）
    private val prefs = context.getSharedPreferences("nexalarm_settings", Context.MODE_PRIVATE)

    // 敏感認證資料（使用 EncryptedSharedPreferences，AES256 加密）
    private val securePrefs = runCatching {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "nexalarm_auth_secure",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        // 極少數裝置不支援 Keystore 時 fallback（仍安全於一般 prefs）
        context.getSharedPreferences("nexalarm_auth_fallback", Context.MODE_PRIVATE)
    }

    // ── 一般設定 ──

    var isDarkMode: Boolean
        get() = prefs.getBoolean("dark_mode", true)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    var isEnglish: Boolean
        get() = prefs.getBoolean("is_english", false)
        set(value) = prefs.edit().putBoolean("is_english", value).apply()

    // 使用者選擇的時區 ID（e.g. "Asia/Taipei"）；null 表示跟隨系統時區
    var timeZoneId: String?
        get() = prefs.getString("time_zone_id", null)
        set(value) {
            if (value != null) prefs.edit().putString("time_zone_id", value).apply()
            else prefs.edit().remove("time_zone_id").apply()
        }

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

    // ── 敏感認證資料（加密儲存）──

    // JWT Token（AES256 加密）
    var authToken: String?
        get() = securePrefs.getString("auth_token", null)
        set(value) {
            if (value != null) securePrefs.edit().putString("auth_token", value).apply()
            else securePrefs.edit().remove("auth_token").apply()
        }

    // 使用者 ID（加密）
    var authUserId: Int
        get() = securePrefs.getInt("auth_user_id", -1)
        set(value) = securePrefs.edit().putInt("auth_user_id", value).apply()

    // 帳號名稱（加密）
    var authUsername: String?
        get() = securePrefs.getString("auth_username", null)
        set(value) {
            if (value != null) securePrefs.edit().putString("auth_username", value).apply()
            else securePrefs.edit().remove("auth_username").apply()
        }

    // 顯示名稱（加密）
    var authDisplayName: String?
        get() = securePrefs.getString("auth_display_name", null)
        set(value) {
            if (value != null) securePrefs.edit().putString("auth_display_name", value).apply()
            else securePrefs.edit().remove("auth_display_name").apply()
        }

    fun clearAuth() {
        securePrefs.edit()
            .remove("auth_token")
            .remove("auth_user_id")
            .remove("auth_username")
            .remove("auth_display_name")
            .apply()
    }
}
