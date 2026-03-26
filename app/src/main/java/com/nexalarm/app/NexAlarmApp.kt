package com.nexalarm.app

import android.app.Application
import androidx.work.*
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.ui.theme.isDarkTheme
import com.nexalarm.app.ui.theme.isAppEnglish
import com.nexalarm.app.util.BillingManager
import com.nexalarm.app.util.CrashHandler
import com.nexalarm.app.util.FeatureFlags
import com.nexalarm.app.util.NotificationHelper
import com.nexalarm.app.worker.AlarmSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Application 類別
 * 負責 App 啟動時的初始化工作，包含背景服務場景（AlarmService 等）
 */
class NexAlarmApp : Application() {

    /** Application 級單例，避免每次重組重建 BillingClient 連線 */
    val billingManager: BillingManager by lazy { BillingManager(this) }

    override fun onCreate() {
        super.onCreate()

        // 安裝全域崩潰處理器（最先初始化，確保後續的崩潰也能被捕獲）
        CrashHandler.install(this)

        // 建立通知頻道
        NotificationHelper.createNotificationChannels(this)

        // 初始化全域設定變數，確保背景服務啟動時也能讀到正確設定
        val settings = SettingsManager(this)
        isDarkTheme = settings.isDarkMode
        isAppEnglish = settings.isEnglish
        FeatureFlags.isPremium = settings.isPremium

        // 排程背景同步（每 15 分鐘，Android WorkManager 最小間隔）
        schedulePeriodicSync()

        android.util.Log.d("NexAlarmApp", "Application initialized")
    }

    private fun schedulePeriodicSync() {
        val syncRequest = PeriodicWorkRequestBuilder<AlarmSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "alarm_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
