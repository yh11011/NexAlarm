package com.nexalarm.app.test

/**
 * NexAlarm 完整測試矩陣 v2 映射表
 *
 * 維度定義 (8+2):
 *   D1: Android API (26-30, 31-32, 33+)
 *   D2: App 狀態 (前景, 背景, 被滑掉, Process death)
 *   D3: 裝置狀態 (解鎖, 鎖屏, 充電, 待機, 重開機後)
 *   D4: 系統限制 (無, Doze, 電池優化未白名單, DND)
 *   D5: 精確鬧鐘權限 (USE_EXACT, SCHEDULE_EXACT granted/denied)
 *   D6: 通知權限 (N/A<33, POST_NOTIFICATIONS granted/denied)
 *   D7: 帳號狀態 (未登入, 已登入, Token 過期)
 *   D8: 網路狀態 (正常, 離線, 弱網, 伺服器 5xx)
 *   D9: 安裝來源 (fresh install, upgrade, backup-restore)
 *   D10: 時間條件 (今日未到, 今日已過, 跨日, 跨週, 跨月, 跨年, DST)
 *
 * 方案狀態: free, premium_play, premium_promo
 *
 * 驗證訊號:
 *   DB: Room DB 查詢
 *   AM: dumpsys alarm
 *   NOTIF: dumpsys notification
 *   UI: logcat + UI Automator
 */
object TestMatrix {

    // ==================== 維度枚舉 ====================

    enum class D1_Api(val range: String) {
        LEGACY("26-30"),
        EXACT_PERM("31-32"),
        NOTIF_PERM("33+")
    }

    enum class D2_AppState {
        FOREGROUND, BACKGROUND, SWIPED_AWAY, PROCESS_DEATH
    }

    enum class D3_DeviceState {
        UNLOCKED, LOCKED, CHARGING, STANDBY, POST_REBOOT
    }

    enum class D4_SystemLimit {
        NONE, DOZE, BATTERY_OPT_NOT_WHITELISTED, DND
    }

    enum class D5_ExactAlarmPerm {
        USE_EXACT_ALARM,
        SCHEDULE_EXACT_GRANTED,
        SCHEDULE_EXACT_DENIED
    }

    enum class D6_NotifPerm {
        NA_PRE_33,
        POST_NOTIFICATIONS_GRANTED,
        POST_NOTIFICATIONS_DENIED
    }

    enum class D7_AccountState {
        NOT_LOGGED_IN, LOGGED_IN, TOKEN_EXPIRED
    }

    enum class D8_NetworkState {
        NORMAL, OFFLINE, WEAK, SERVER_5XX
    }

    enum class D9_InstallSource {
        FRESH_INSTALL, UPGRADE, BACKUP_RESTORE
    }

    enum class D10_TimeCondition {
        TODAY_NOT_YET, TODAY_PASSED, CROSS_DAY, CROSS_WEEK,
        CROSS_MONTH, CROSS_YEAR, DST_SWITCH
    }

    enum class PlanState {
        FREE, PREMIUM_PLAY, PREMIUM_PROMO
    }

    // ==================== 測試案例定義 ====================

    data class TestCase(
        val id: String,
        val layer: Int,
        val feature: String,
        val description: String,
        val priority: Int,
        val dimensions: Map<String, Any>,
        val planState: PlanState = PlanState.FREE,
        val expectedEvidence: List<Evidence>,
        val automation: AutomationType
    )

    enum class Evidence { DB, ALARM_MANAGER, NOTIFICATION, UI }
    enum class AutomationType { ADB_SCRIPT, INSTRUMENTATION, MANUAL }

    // ==================== Layer 1 — Smoke (12 組) ====================

    val LAYER_1_SMOKE = listOf(
        TestCase(
            id = "L1-01", layer = 1, feature = "單次鬧鐘-新增觸發",
            description = "新增單次鬧鐘 → 等待觸發 → 全螢幕 UI + 鈴聲 + 震動",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM, "D6" to D6_NotifPerm.NA_PRE_33,
                "D9" to D9_InstallSource.FRESH_INSTALL, "D10" to D10_TimeCondition.TODAY_NOT_YET
            ),
            expectedEvidence = listOf(Evidence.DB, Evidence.ALARM_MANAGER, Evidence.UI),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L1-02", layer = 1, feature = "單次鬧鐘-時間已過",
            description = "設定已過時間 → 排程到明天",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D10" to D10_TimeCondition.TODAY_PASSED
            ),
            expectedEvidence = listOf(Evidence.DB, Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L1-03", layer = 1, feature = "重複鬧鐘-每天",
            description = "每天觸發，觸發後自動重排下一次",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D10" to D10_TimeCondition.CROSS_WEEK
            ),
            expectedEvidence = listOf(Evidence.DB, Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L1-04", layer = 1, feature = "鬧鐘觸發-鎖屏",
            description = "鎖屏狀態觸發 → 全螢幕覆蓋鎖定畫面",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.LOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L1-05", layer = 1, feature = "鬧鐘觸發-背景",
            description = "App 在背景時觸發 → Service 啟動 + 通知",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.NOTIFICATION, Evidence.UI),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L1-06", layer = 1, feature = "鬧鐘觸發-被滑掉",
            description = "從 recent 移除後觸發 → AlarmManager 仍存活",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.SWIPED_AWAY,
                "D3" to D3_DeviceState.STANDBY, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER, Evidence.UI),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L1-07", layer = 1, feature = "貪睡-基本",
            description = "響鈴時貪睡 → 延遲後再次響鈴",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.LOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.UI, Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L1-08", layer = 1, feature = "開機重排",
            description = "重開機 → 所有 enabled 鬧鐘重新排程",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D3" to D3_DeviceState.POST_REBOOT,
                "D4" to D4_SystemLimit.NONE, "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D7" to D7_AccountState.LOGGED_IN, "D8" to D8_NetworkState.OFFLINE
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L1-09", layer = 1, feature = "停用/啟用",
            description = "開關切換 → 排程/取消排程",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER, Evidence.DB),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L1-10", layer = 1, feature = "鬧鐘觸發-Doze",
            description = "Doze 模式下觸發 → setAlarmClock bypass Doze",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.STANDBY, "D4" to D4_SystemLimit.DOZE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER, Evidence.UI),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L1-11", layer = 1, feature = "鬧鐘觸發-DND",
            description = "DND 模式下觸發 → 鬧鐘穿透勿擾",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.NOTIF_PERM, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.DND,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D6" to D6_NotifPerm.POST_NOTIFICATIONS_GRANTED
            ),
            expectedEvidence = listOf(Evidence.NOTIFICATION, Evidence.UI),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L1-12", layer = 1, feature = "觸發後處理",
            description = "單次刪除 / 保留停用 / 重複重排",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D7" to D7_AccountState.LOGGED_IN, "D8" to D8_NetworkState.NORMAL
            ),
            expectedEvidence = listOf(Evidence.DB, Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        )
    )

    // ==================== 補充案例 (6 組) ====================

    val SUPPLEMENTARY = listOf(
        TestCase(
            id = "S11", layer = 1, feature = "首次啟動引導",
            description = "API 33+ SCHEDULE_EXACT denied→granted 授權流程",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.NOTIF_PERM,
                "D5" to D5_ExactAlarmPerm.SCHEDULE_EXACT_GRANTED,
                "D9" to D9_InstallSource.FRESH_INSTALL
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "S12", layer = 1, feature = "USE_EXACT_ALARM 安裝即有權限",
            description = "宣告 USE_EXACT_ALARM → 安裝即有權限 → 直接排程觸發",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.NOTIF_PERM,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D9" to D9_InstallSource.FRESH_INSTALL
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER, Evidence.UI),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "S13", layer = 2, feature = "精確權限撤銷",
            description = "授權後手動撤銷 → 既有 future alarms 被取消 → fallback",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.NOTIF_PERM,
                "D5" to D5_ExactAlarmPerm.SCHEDULE_EXACT_DENIED
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "N1", layer = 1, feature = "無通知權限",
            description = "POST_NOTIFICATIONS denied → FGS 仍啟動; 全螢幕仍顯示; 通知不可見",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.NOTIF_PERM,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D6" to D6_NotifPerm.POST_NOTIFICATIONS_DENIED
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "B1", layer = 1, feature = "重開機+離線",
            description = "BOOT_COMPLETED 重排 → 離線不影響排程",
            priority = 0,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D3" to D3_DeviceState.POST_REBOOT,
                "D8" to D8_NetworkState.OFFLINE
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "T5", layer = 3, feature = "DST 切換",
            description = "DST 切換日 + 非系統時區 + 重複鬧鐘 → triggerTime 正確",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D10" to D10_TimeCondition.DST_SWITCH
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER, Evidence.DB),
            automation = AutomationType.INSTRUMENTATION
        )
    )

    // ==================== Layer 2 — Regression (20 組) ====================

    val LAYER_2_REGRESSION = listOf(
        TestCase(
            id = "L2-01", layer = 2, feature = "重複鬧鐘-工作日",
            description = "僅週一~五觸發",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D10" to D10_TimeCondition.CROSS_WEEK
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L2-02", layer = 2, feature = "重複鬧鐘-週末",
            description = "僅週六日觸發",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D10" to D10_TimeCondition.CROSS_WEEK
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L2-03", layer = 2, feature = "重複鬧鐘-自訂天數",
            description = "不連續天數（如一三五）",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D10" to D10_TimeCondition.CROSS_WEEK
            ),
            expectedEvidence = listOf(Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L2-04", layer = 2, feature = "貪睡-次數上限",
            description = "超過 maxSnoozeCount 自動關閉",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.LOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L2-05", layer = 2, feature = "貪睡-無限",
            description = "maxSnoozeCount=0 可無限貪睡",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.LOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L2-06", layer = 2, feature = "震動模式",
            description = "vibrateOnly=true → 無鈴聲有震動",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L2-07", layer = 2, feature = "音量控制",
            description = "volume 0~100 映射正確",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L2-08", layer = 2, feature = "資料夾-新增",
            description = "含 emoji、計數、排序",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE
            ),
            expectedEvidence = listOf(Evidence.DB, Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L2-09", layer = 2, feature = "資料夾-系統不可刪",
            description = "系統資料夾保護",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L2-10", layer = 2, feature = "資料夾-開關",
            description = "切換 enabled → 下所有鬧鐘一併切換",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D7" to D7_AccountState.LOGGED_IN, "D8" to D8_NetworkState.NORMAL
            ),
            expectedEvidence = listOf(Evidence.DB),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L2-11", layer = 2, feature = "資料夾模式鬧鐘",
            description = "isFolderMode → isRecurring=false, keepAfterRinging=false",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.DB, Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L2-12", layer = 2, feature = "免費版鬧鐘上限",
            description = "30 個後無法新增",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE
            ),
            expectedEvidence = listOf(Evidence.DB, Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L2-13", layer = 2, feature = "免費版資料夾上限",
            description = "10 個後無法新增",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE
            ),
            expectedEvidence = listOf(Evidence.DB, Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L2-14", layer = 2, feature = "Deep Link-新增",
            description = "nexalarm://add 正確解析並排程",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D10" to D10_TimeCondition.TODAY_NOT_YET
            ),
            expectedEvidence = listOf(Evidence.DB, Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L2-15", layer = 2, feature = "Deep Link-刪除",
            description = "nexalarm://delete 正確刪除並取消排程",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D7" to D7_AccountState.LOGGED_IN, "D8" to D8_NetworkState.NORMAL
            ),
            expectedEvidence = listOf(Evidence.DB, Evidence.ALARM_MANAGER),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L2-16", layer = 2, feature = "Deep Link-去重",
            description = "重複新增相同鬧鐘 → 更新而非新增",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM,
                "D10" to D10_TimeCondition.TODAY_NOT_YET
            ),
            expectedEvidence = listOf(Evidence.DB),
            automation = AutomationType.ADB_SCRIPT
        ),
        TestCase(
            id = "L2-17", layer = 2, feature = "導航-三層結構",
            description = "Drawer + Pager + NavController 正常",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L2-18", layer = 2, feature = "倒數文字",
            description = "每 30 秒更新，雙語正確",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L2-19", layer = 2, feature = "全螢幕 UI-上滑關閉",
            description = "swipe > 100dp dismiss",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.LOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L2-20", layer = 2, feature = "全域狀態同步",
            description = "Compose ↔ SharedPreferences ↔ Background Service",
            priority = 1,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE
            ),
            expectedEvidence = listOf(Evidence.DB),
            automation = AutomationType.INSTRUMENTATION
        )
    )

    // ==================== Layer 3 — Reliability (14 組) ====================

    val LAYER_3_RELIABILITY = listOf(
        TestCase(
            id = "L3-01", layer = 3, feature = "Process death 恢復",
            description = "系統回收後 SharedPreferences 恢復狀態",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.PROCESS_DEATH,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D5" to D5_ExactAlarmPerm.USE_EXACT_ALARM
            ),
            expectedEvidence = listOf(Evidence.DB, Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-02", layer = 3, feature = "計時器-背景存活",
            description = "Home 後繼續，恢復時狀態正確",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.STANDBY, "D4" to D4_SystemLimit.NONE
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-03", layer = 3, feature = "計時器-Process death",
            description = "殺死重啟後計算已過時間",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.PROCESS_DEATH,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-04", layer = 3, feature = "碼錶-背景存活",
            description = "Home 後繼續，恢復時時間正確",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.STANDBY, "D4" to D4_SystemLimit.NONE
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-05", layer = 3, feature = "碼錶-Process death",
            description = "殺死重啟後 accumulatedTime + elapsed 正確",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.PROCESS_DEATH,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-06", layer = 3, feature = "雲端同步-即時",
            description = "CRUD 後立即觸發同步",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D7" to D7_AccountState.LOGGED_IN, "D8" to D8_NetworkState.NORMAL
            ),
            expectedEvidence = listOf(Evidence.DB),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-07", layer = 3, feature = "雲端同步-定期",
            description = "WorkManager 每 15 分鐘",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.BACKGROUND,
                "D3" to D3_DeviceState.STANDBY, "D4" to D4_SystemLimit.NONE,
                "D7" to D7_AccountState.LOGGED_IN, "D8" to D8_NetworkState.NORMAL
            ),
            expectedEvidence = listOf(Evidence.DB),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-08", layer = 3, feature = "雲端同步-離線",
            description = "離線跳過，不 crash",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D7" to D7_AccountState.LOGGED_IN, "D8" to D8_NetworkState.OFFLINE
            ),
            expectedEvidence = listOf(Evidence.DB),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-09", layer = 3, feature = "雲端同步-弱網",
            description = "超時重試（1s, 2s, 4s）",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D7" to D7_AccountState.LOGGED_IN, "D8" to D8_NetworkState.WEAK
            ),
            expectedEvidence = listOf(Evidence.DB),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-10", layer = 3, feature = "雲端同步-衝突",
            description = "serverUpdatedAt > local → 伺服器勝出",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D7" to D7_AccountState.LOGGED_IN, "D8" to D8_NetworkState.NORMAL
            ),
            expectedEvidence = listOf(Evidence.DB),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-11", layer = 3, feature = "帳號-登入/註冊",
            description = "正常流程 + 錯誤處理",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D7" to D7_AccountState.NOT_LOGGED_IN, "D8" to D8_NetworkState.NORMAL
            ),
            expectedEvidence = listOf(Evidence.DB),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-12", layer = 3, feature = "帳號-Token 過期",
            description = "401 自動登出",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D7" to D7_AccountState.TOKEN_EXPIRED, "D8" to D8_NetworkState.NORMAL
            ),
            expectedEvidence = listOf(Evidence.DB),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-13", layer = 3, feature = "付費-Premium",
            description = "Play 購買 / 優惠碼 / 停用邏輯",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE,
                "D7" to D7_AccountState.LOGGED_IN, "D8" to D8_NetworkState.NORMAL
            ),
            planState = PlanState.PREMIUM_PLAY,
            expectedEvidence = listOf(Evidence.DB),
            automation = AutomationType.INSTRUMENTATION
        ),
        TestCase(
            id = "L3-14", layer = 3, feature = "設定-語言/主題/時區",
            description = "切換正確套用",
            priority = 2,
            dimensions = mapOf(
                "D1" to D1_Api.LEGACY, "D2" to D2_AppState.FOREGROUND,
                "D3" to D3_DeviceState.UNLOCKED, "D4" to D4_SystemLimit.NONE
            ),
            expectedEvidence = listOf(Evidence.UI),
            automation = AutomationType.INSTRUMENTATION
        )
    )

    // ==================== 全部案例 ====================

    val ALL_CASES: List<TestCase> = LAYER_1_SMOKE + SUPPLEMENTARY + LAYER_2_REGRESSION + LAYER_3_RELIABILITY

    // ==================== 查詢工具 ====================

    fun getById(id: String): TestCase? = ALL_CASES.find { it.id == id }

    fun getByLayer(layer: Int): List<TestCase> = ALL_CASES.filter { it.layer == layer }

    fun getByAutomation(type: AutomationType): List<TestCase> = ALL_CASES.filter { it.automation == type }

    fun getByPriority(priority: Int): List<TestCase> = ALL_CASES.filter { it.priority == priority }

    fun summary(): String = buildString {
        appendLine("NexAlarm 測試矩陣 v2 摘要")
        appendLine("=".repeat(50))
        appendLine("總案例數: ${ALL_CASES.size}")
        appendLine("Layer 1 (Smoke):      ${LAYER_1_SMOKE.size + SUPPLEMENTARY.size} 組 (含 ${SUPPLEMENTARY.size} 補充)")
        appendLine("Layer 2 (Regression): ${LAYER_2_REGRESSION.size} 組")
        appendLine("Layer 3 (Reliability): ${LAYER_3_RELIABILITY.size} 組")
        appendLine()
        val adbCount = ALL_CASES.count { it.automation == AutomationType.ADB_SCRIPT }
        val instrCount = ALL_CASES.count { it.automation == AutomationType.INSTRUMENTATION }
        val manualCount = ALL_CASES.count { it.automation == AutomationType.MANUAL }
        appendLine("自動化分佈:")
        appendLine("  ADB Script:      $adbCount (${pct(adbCount, ALL_CASES.size)}%)")
        appendLine("  Instrumentation: $instrCount (${pct(instrCount, ALL_CASES.size)}%)")
        appendLine("  Manual:          $manualCount (${pct(manualCount, ALL_CASES.size)}%)")
    }

    private fun pct(count: Int, total: Int): String =
        if (total > 0) "${"%.0f".format(count.toDouble() / total * 100)}" else "0"
}
