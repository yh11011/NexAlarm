package com.nexalarm.app

import android.app.Application
import androidx.work.*
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.util.AppSettingsProvider
import com.nexalarm.app.util.BillingManager
import com.nexalarm.app.util.CrashReportingManager
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

        // 初始化遠程崩潰報告（最先初始化，自動安裝本地和遠程異常處理器）
        CrashReportingManager.init(this)

        // 建立通知頻道
        NotificationHelper.createNotificationChannels(this)

        // 初始化應用設定提供者（統一管理所有設定，確保執行緒安全）
        AppSettingsProvider.init(this)

        // 初始化功能標誌
        val settings = SettingsManager(this)
        FeatureFlags.isPremium = settings.isPremium

        // 排程背景同步（每 15 分鐘，Android WorkManager 最小間隔）
        schedulePeriodicSync()

        android.util.Log.d("NexAlarmApp", "Application initialized with CrashReportingManager, AppSettingsProvider, and AlarmSyncWorker")
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
