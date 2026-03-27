package com.nexalarm.app.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * 崩潰報告管理器
 *
 * 設計目的：
 * - 統一管理本地和遠程（Firebase Crashlytics）的崩潰報告
 * - 自動捕獲未捕獲的異常
 * - 記錄自定義日誌，幫助診斷問題
 * - 區分開發環境和生產環境
 *
 * 初始化：
 * - 在 NexAlarmApp.onCreate() 中透過 CrashReportingManager.init() 初始化
 * - 同時安裝本地 CrashHandler 和 Firebase 全域異常處理器
 *
 * Firebase Crashlytics 配置：
 * - 需要在 Firebase Console 建立專案並下載 google-services.json
 * - google-services.json 放在 app/ 目錄中
 * - 需要在 build.gradle.kts 中添加 Firebase 依賴和 google-services 外掛
 *
 * 使用方式：
 * 1. 自動捕獲：所有未捕獲的異常自動上報到 Firebase
 * 2. 手動記錄：
 *    CrashReportingManager.recordException(exception)
 *    CrashReportingManager.log("diagnostic message")
 * 3. 設定自定義鍵值：
 *    CrashReportingManager.setCustomKey("feature", "timer")
 */
object CrashReportingManager {
    private const val TAG = "CrashReportingManager"
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        val crashlytics = FirebaseCrashlytics.getInstance()

        // 設定用戶標識（如有用戶系統可添加）
        crashlytics.setUserId("anonymous")

        // 設定自定義鍵值，幫助分類崩潰報告
        crashlytics.setCustomKey("app_version", getAppVersion(context))
        crashlytics.setCustomKey("device_model", "${Build.MANUFACTURER} ${Build.MODEL}")
        crashlytics.setCustomKey("android_version", Build.VERSION.RELEASE)
        crashlytics.setCustomKey("sdk_int", Build.VERSION.SDK_INT)

        // 設定發送未捕獲異常到 Firebase（默認開啟，但明確設定）
        crashlytics.setCrashlyticsCollectionEnabled(true)

        // 安裝全域未捕獲異常處理器
        installGlobalExceptionHandler(context)

        isInitialized = true
        Log.d(TAG, "CrashReportingManager initialized successfully")
    }

    /**
     * 安裝全域異常處理器
     * 捕獲所有未被捕獲的異常，同時上報到 Firebase 和本地檔案
     */
    private fun installGlobalExceptionHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        val crashlytics = FirebaseCrashlytics.getInstance()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // 上報到本地檔案
                CrashHandler.recordException(context, thread, throwable)

                // 上報到 Firebase Crashlytics
                crashlytics.recordException(throwable)

                Log.e(TAG, "Uncaught exception recorded", throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record exception", e)
            } finally {
                // 執行系統預設異常處理（顯示系統錯誤對話框）
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * 手動記錄異常到 Firebase
     * 用於在 catch 塊中主動上報異常（非致命）
     */
    fun recordException(throwable: Throwable) {
        if (!isInitialized) {
            Log.w(TAG, "CrashReportingManager not initialized")
            return
        }
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    /**
     * 記錄自定義日誌到 Firebase Crashlytics
     * 這些日誌會附在下一次崩潰報告中，幫助診斷問題
     */
    fun log(message: String) {
        if (!isInitialized) return
        FirebaseCrashlytics.getInstance().log(message)
    }

    /**
     * 設定自定義鍵值對，幫助分類和搜索崩潰報告
     */
    fun setCustomKey(key: String, value: String) {
        if (!isInitialized) return
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    /**
     * 設定自定義鍵值對（整數）
     */
    fun setCustomKey(key: String, value: Int) {
        if (!isInitialized) return
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    /**
     * 設定自定義鍵值對（布林值）
     */
    fun setCustomKey(key: String, value: Boolean) {
        if (!isInitialized) return
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    /**
     * 記錄警告訊息到 Firebase（用於非致命問題）
     */
    fun logWarning(message: String) {
        log("[WARNING] $message")
    }

    /**
     * 記錄錯誤訊息到 Firebase
     */
    fun logError(message: String, throwable: Throwable? = null) {
        log("[ERROR] $message")
        if (throwable != null) {
            recordException(throwable)
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
