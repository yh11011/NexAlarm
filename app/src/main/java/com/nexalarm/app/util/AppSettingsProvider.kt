package com.nexalarm.app.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.ui.theme.AppTheme
import com.nexalarm.app.ui.theme.colors
import com.nexalarm.app.ui.theme.toSupportedTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 統一的應用設定提供者
 *
 * 設計目的：
 * - 作為全域設定的單一真實源（Single Source of Truth）
 * - 在 Compose UI 層和非 Compose 背景服務層之間協調狀態
 * - 防止競態條件和狀態不一致
 *
 * 使用方式：
 * - Compose UI：使用 isDarkThemeMutableState 和 isAppEnglishMutableState（自動感應變更）
 * - 非 Compose（AlarmService/BroadcastReceiver）：使用 get/set 函式，確保讀的是最新值
 *
 * 執行緒安全性：
 * - SharedPreferences 操作在主執行緒安全（Android 框架保證）
 * - Compose 的 MutableState 自動在主執行緒上更新
 * - 背景服務透過 SettingsManager 讀取，避免全域狀態競態條件
 */
object AppSettingsProvider {
    private lateinit var settingsManager: SettingsManager

    // Compose 層使用（自動感應 UI 更新）
    val isDarkThemeMutableState = mutableStateOf(true)
    val isAppEnglishMutableState = mutableStateOf(false)
    val currentThemeMutableState = mutableStateOf(AppTheme.MINIMALIST)

    /**
     * 初始化設定提供者
     * 必須在應用啟動時呼叫一次（NexAlarmApp.onCreate 中）
     */
    fun init(context: Context) {
        settingsManager = SettingsManager(context)
        // 舊主題 ID 會正規化成支援的模式；深淺狀態必須跟隨正規化後的色票。
        val supportedTheme = AppTheme.fromId(settingsManager.themeId).toSupportedTheme()
        settingsManager.isDarkMode = supportedTheme.colors().isDark
        currentThemeMutableState.value = supportedTheme
        isDarkThemeMutableState.value = supportedTheme.colors().isDark
        isAppEnglishMutableState.value = settingsManager.isEnglish
    }

    /**
     * 取得目前的深色模式設定
     * 推薦在非 Compose 環境中使用（如 AlarmService）
     */
    fun getDarkMode(): Boolean {
        return if (::settingsManager.isInitialized) {
            settingsManager.isDarkMode
        } else {
            isDarkThemeMutableState.value
        }
    }

    /**
     * 設定深色模式
     * 同時更新 Compose State 和 SharedPreferences
     */
    fun setDarkMode(isDark: Boolean) {
        if (::settingsManager.isInitialized) {
            settingsManager.isDarkMode = isDark
        }
        isDarkThemeMutableState.value = isDark
    }

    /**
     * 取得目前的語言設定
     * 推薦在非 Compose 環境中使用（如 AlarmService）
     */
    fun getLanguageEnglish(): Boolean {
        return if (::settingsManager.isInitialized) {
            settingsManager.isEnglish
        } else {
            isAppEnglishMutableState.value
        }
    }

    /**
     * 設定語言
     * 同時更新 Compose State 和 SharedPreferences
     */
    fun setLanguageEnglish(isEnglish: Boolean) {
        if (::settingsManager.isInitialized) {
            settingsManager.isEnglish = isEnglish
        }
        isAppEnglishMutableState.value = isEnglish
    }

    /**
     * 同步所有設定（當需要確保與 SharedPreferences 一致時）
     * 用於 AlarmService 或其他背景服務確保狀態同步
     */
    /** 設定主題，同時更新 Compose State 和 SharedPreferences */
    fun setTheme(theme: AppTheme) {
        val supportedTheme = theme.toSupportedTheme()
        if (::settingsManager.isInitialized) {
            settingsManager.themeId = supportedTheme.id
            // 同步深色模式標誌（給非主題感知的系統元件使用）
            settingsManager.isDarkMode = supportedTheme.colors().isDark
        }
        currentThemeMutableState.value = supportedTheme
        isDarkThemeMutableState.value = supportedTheme.colors().isDark
    }

    fun syncFromSharedPreferences() {
        if (::settingsManager.isInitialized) {
            val supportedTheme = AppTheme.fromId(settingsManager.themeId).toSupportedTheme()
            settingsManager.isDarkMode = supportedTheme.colors().isDark
            currentThemeMutableState.value = supportedTheme
            isDarkThemeMutableState.value = supportedTheme.colors().isDark
            isAppEnglishMutableState.value = settingsManager.isEnglish
        }
    }
}
