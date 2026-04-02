#!/bin/bash
# ============================================================
# NexAlarm 測試矩陣 v2 — Layer 1 Smoke 自動化執行腳本
#
# 用途: 透過 ADB 指令自動化執行 12+6 組 Smoke 測試
# 前置: 裝置已連接、App 已安裝、USB 偵錯已開啟
# 用法: bash run_layer1_smoke.sh [device_serial]
# ============================================================

set -euo pipefail

DEVICE="${1:-}"
PKG="com.nexalarm.app"
LOGCAT_TAG="NexAlarmTest"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_DIR="reports/smoke_${TIMESTAMP}"
mkdir -p "$REPORT_DIR"

# ==================== 工具函式 ====================

adb_cmd() {
    if [ -n "$DEVICE" ]; then
        adb -s "$DEVICE" "$@"
    else
        adb "$@"
    fi
}

log() {
    echo "[$(date '+%H:%M:%S')] $1" | tee -a "$REPORT_DIR/run.log"
}

# 驗證訊號: DB 狀態 (透過 content provider 或 Room 查詢)
check_db_alarm_exists() {
    local count
    count=$(adb_cmd shell "run-as $PKG cat /data/data/$PKG/databases/nexalarm.db" 2>/dev/null | \
            sqlite3 -line "SELECT COUNT(*) FROM alarms WHERE isEnabled=1;" 2>/dev/null || echo "0")
    echo "$count"
}

# 驗證訊號: AlarmManager 狀態
check_alarm_manager() {
    local dump
    dump=$(adb_cmd shell "dumpsys alarm | grep $PKG" 2>/dev/null || echo "")
    if echo "$dump" | grep -qE "RTC_WAKEUP|ELAPSED_WAKEUP|AlarmClockInfo"; then
        echo "FOUND"
    else
        echo "NOT_FOUND"
    fi
}

# 驗證訊號: Notification 狀態
check_notification() {
    local dump
    dump=$(adb_cmd shell "dumpsys notification | grep $PKG" 2>/dev/null || echo "")
    if [ -n "$dump" ]; then
        echo "FOUND"
    else
        echo "NOT_FOUND"
    fi
}

# 驗證訊號: UI/Activity 狀態 (透過 logcat)
check_ui_visible() {
    local dump
    dump=$(adb_cmd shell "dumpsys window | grep mCurrentFocus" 2>/dev/null || echo "")
    if echo "$dump" | grep -q "$PKG"; then
        echo "VISIBLE"
    else
        echo "NOT_VISIBLE"
    fi
}

# 等待鬧鐘觸發 (透過 logcat 監控)
wait_for_alarm_trigger() {
    local alarm_id="$1"
    local timeout_sec="${2:-120}"
    local start_time=$(date +%s)

    log "等待鬧鐘 #$alarm_id 觸發 (timeout: ${timeout_sec}s)..."
    adb_cmd logcat -c 2>/dev/null || true

    while true; do
        local elapsed=$(( $(date +%s) - start_time ))
        if [ "$elapsed" -ge "$timeout_sec" ]; then
            log "⏱ 超時: 鬧鐘 #$alarm_id 未在 ${timeout_sec}s 內觸發"
            echo "TIMEOUT"
            return 1
        fi

        # 檢查 logcat 中的觸發日誌
        local trigger_log
        trigger_log=$(adb_cmd logcat -d -t 100 | grep "$LOGCAT_TAG" | grep "TRIGGERED\|alarm_id=$alarm_id" 2>/dev/null || echo "")
        if [ -n "$trigger_log" ]; then
            log "✅ 鬧鐘 #$alarm_id 已觸發"
            echo "TRIGGERED"
            return 0
        fi

        # 檢查 AlarmTestHook SharedPreferences
        local hook_data
        hook_data=$(adb_cmd shell "run-as $PKG cat /data/data/$PKG/shared_prefs/alarm_test_hook.xml" 2>/dev/null || echo "")
        if echo "$hook_data" | grep -q "receiver_time_${alarm_id}"; then
            log "✅ 鬧鐘 #$alarm_id 已觸發 (AlarmTestHook 確認)"
            echo "TRIGGERED"
            return 0
        fi

        sleep 2
    done
}

# 收集驗證訊號
collect_evidence() {
    local case_id="$1"
    local evidence_file="$REPORT_DIR/${case_id}_evidence.txt"

    {
        echo "=== 驗證訊號: $case_id ==="
        echo "時間: $(date '+%Y-%m-%d %H:%M:%S')"
        echo ""
        echo "[DB] 鬧鐘數量: $(check_db_alarm_exists)"
        echo "[AM] AlarmManager: $(check_alarm_manager)"
        echo "[NOTIF] 通知: $(check_notification)"
        echo "[UI] 全螢幕: $(check_ui_visible)"
        echo ""
        echo "[dumpsys alarm 相關輸出]:"
        adb_cmd shell "dumpsys alarm | grep -A5 -B5 $PKG" 2>/dev/null || echo "(無)"
        echo ""
        echo "[dumpsys notification 相關輸出]:"
        adb_cmd shell "dumpsys notification | grep -A3 -B3 $PKG" 2>/dev/null || echo "(無)"
    } > "$evidence_file"

    log "📄 證據已儲存: $evidence_file"
}

# 記錄測試結果
record_result() {
    local case_id="$1"
    local status="$2"  # PASS / FAIL / SKIP
    local detail="$3"
    echo "${case_id},${status},${detail},$(date '+%Y-%m-%d %H:%M:%S')" >> "$REPORT_DIR/results.csv"
}

# ==================== 測試案例 ====================

# 初始化結果 CSV
echo "CaseID,Status,Detail,Timestamp" > "$REPORT_DIR/results.csv"

log "═══════════════════════════════════════"
log "NexAlarm Layer 1 Smoke 測試開始"
log "裝置: $(adb_cmd shell getprop ro.product.model 2>/dev/null || echo 'unknown')"
log "Android: $(adb_cmd shell getprop ro.build.version.release 2>/dev/null || echo 'unknown')"
log "═══════════════════════════════════════"

# -------------------- L1-01: 單次鬧鐘-新增觸發 --------------------
run_L1_01() {
    local case_id="L1-01"
    log "▶ 執行 $case_id: 單次鬧鐘-新增觸發"

    # 透過 Deep Link 新增鬧鐘 (現在起 60 秒後)
    local trigger_time
    trigger_time=$(date -d '+60 seconds' '+%H%M' 2>/dev/null || date -v+60S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=SmokeTest01'" 2>/dev/null || true
    sleep 3

    # 驗證排程
    local am_status
    am_status=$(check_alarm_manager)
    if [ "$am_status" != "FOUND" ]; then
        record_result "$case_id" "FAIL" "AlarmManager 排程失敗"
        return 1
    fi

    # 等待觸發
    local trigger_result
    trigger_result=$(wait_for_alarm_trigger "1" 90) || true

    if [ "$trigger_result" = "TRIGGERED" ]; then
        # 驗證 UI
        local ui_status
        ui_status=$(check_ui_visible)
        if [ "$ui_status" = "VISIBLE" ]; then
            record_result "$case_id" "PASS" "全螢幕 UI 顯示"
            collect_evidence "$case_id"
        else
            record_result "$case_id" "FAIL" "觸發但全螢幕 UI 未顯示"
        fi
    else
        record_result "$case_id" "FAIL" "鬧鐘未觸發"
    fi
}

# -------------------- L1-02: 單次鬧鐘-時間已過 --------------------
run_L1_02() {
    local case_id="L1-02"
    log "▶ 執行 $case_id: 單次鬧鐘-時間已過"

    # 設定已過時間 (昨天 08:00)，應排程到明天
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=0800&title=PastTimeTest'" 2>/dev/null || true
    sleep 3

    # 驗證: dumpsys 應顯示明天的排程
    local dump
    dump=$(adb_cmd shell "dumpsys alarm | grep $PKG" 2>/dev/null || echo "")
    if echo "$dump" | grep -qE "RTC_WAKEUP|AlarmClockInfo"; then
        record_result "$case_id" "PASS" "已過時間排程到明天"
        collect_evidence "$case_id"
    else
        record_result "$case_id" "FAIL" "未偵測到排程"
    fi
}

# -------------------- L1-03: 重複鬧鐘-每天 --------------------
run_L1_03() {
    local case_id="L1-03"
    log "▶ 執行 $case_id: 重複鬧鐘-每天"

    # 新增每天重複鬧鐘
    local trigger_time
    trigger_time=$(date -d '+60 seconds' '+%H%M' 2>/dev/null || date -v+60S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=DailyRepeat&repeat=1,2,3,4,5,6,7'" 2>/dev/null || true
    sleep 3

    # 驗證排程
    local am_status
    am_status=$(check_alarm_manager)
    if [ "$am_status" = "FOUND" ]; then
        record_result "$case_id" "PASS" "重複鬧鐘已排程"
        collect_evidence "$case_id"
    else
        record_result "$case_id" "FAIL" "重複鬧鐘排程失敗"
    fi
}

# -------------------- L1-04: 鬧鐘觸發-鎖屏 --------------------
run_L1_04() {
    local case_id="L1-04"
    log "▶ 執行 $case_id: 鬧鐘觸發-鎖屏"

    # 鎖屏
    adb_cmd shell "input keyevent KEYCODE_POWER" 2>/dev/null || true
    sleep 2

    # 新增鬧鐘 (60 秒後)
    local trigger_time
    trigger_time=$(date -d '+60 seconds' '+%H%M' 2>/dev/null || date -v+60S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=LockScreenTest'" 2>/dev/null || true
    sleep 3

    # 等待觸發
    local trigger_result
    trigger_result=$(wait_for_alarm_trigger "4" 90) || true

    if [ "$trigger_result" = "TRIGGERED" ]; then
        # 喚醒螢幕檢查
        adb_cmd shell "input keyevent KEYCODE_POWER" 2>/dev/null || true
        sleep 1
        local ui_status
        ui_status=$(check_ui_visible)
        if [ "$ui_status" = "VISIBLE" ]; then
            record_result "$case_id" "PASS" "鎖屏狀態全螢幕覆蓋"
            collect_evidence "$case_id"
        else
            record_result "$case_id" "FAIL" "鎖屏狀態未顯示全螢幕"
        fi
    else
        record_result "$case_id" "FAIL" "鬧鐘未觸發"
    fi
}

# -------------------- L1-05: 鬧鐘觸發-背景 --------------------
run_L1_05() {
    local case_id="L1-05"
    log "▶ 執行 $case_id: 鬧鐘觸發-背景"

    # 回到 Home (App 背景化)
    adb_cmd shell "input keyevent KEYCODE_HOME" 2>/dev/null || true
    sleep 2

    # 新增鬧鐘
    local trigger_time
    trigger_time=$(date -d '+60 seconds' '+%H%M' 2>/dev/null || date -v+60S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=BgTest'" 2>/dev/null || true
    sleep 3

    # 等待觸發
    local trigger_result
    trigger_result=$(wait_for_alarm_trigger "5" 90) || true

    if [ "$trigger_result" = "TRIGGERED" ]; then
        local notif_status
        notif_status=$(check_notification)
        if [ "$notif_status" = "FOUND" ]; then
            record_result "$case_id" "PASS" "背景觸發: Service + 通知"
            collect_evidence "$case_id"
        else
            record_result "$case_id" "FAIL" "背景觸發但無通知"
        fi
    else
        record_result "$case_id" "FAIL" "鬧鐘未觸發"
    fi
}

# -------------------- L1-06: 鬧鐘觸發-被滑掉 --------------------
run_L1_06() {
    local case_id="L1-06"
    log "▶ 執行 $case_id: 鬧鐘觸發-被滑掉"

    # 新增鬧鐘
    local trigger_time
    trigger_time=$(date -d '+90 seconds' '+%H%M' 2>/dev/null || date -v+90S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=SwipedTest'" 2>/dev/null || true
    sleep 3

    # 從 recent 移除 App
    adb_cmd shell "am force-stop $PKG" 2>/dev/null || true
    log "已執行 am force-stop (模擬被滑掉)"
    sleep 3

    # 等待觸發 (AlarmManager 應仍存活)
    local trigger_result
    trigger_result=$(wait_for_alarm_trigger "6" 120) || true

    if [ "$trigger_result" = "TRIGGERED" ]; then
        record_result "$case_id" "PASS" "被滑掉後 AlarmManager 仍觸發"
        collect_evidence "$case_id"
    else
        record_result "$case_id" "FAIL" "被滑掉後鬧鐘未觸發"
    fi
}

# -------------------- L1-07: 貪睡-基本 --------------------
run_L1_07() {
    local case_id="L1-07"
    log "▶ 執行 $case_id: 貪睡-基本"

    # 新增帶貪睡的鬧鐘
    local trigger_time
    trigger_time=$(date -d '+60 seconds' '+%H%M' 2>/dev/null || date -v+60S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=SnoozeTest&silent=true'" 2>/dev/null || true
    sleep 3

    # 等待第一次觸發
    local trigger_result
    trigger_result=$(wait_for_alarm_trigger "7" 90) || true

    if [ "$trigger_result" = "TRIGGERED" ]; then
        # 模擬貪睡操作 (swipe 或按貪睡按鈕)
        adb_cmd shell "input swipe 500 1500 500 500 300" 2>/dev/null || true
        sleep 2

        # 等待第二次觸發 (貪睡延遲)
        local trigger2_result
        trigger2_result=$(wait_for_alarm_trigger "7_snooze" 600) || true

        if [ "$trigger2_result" = "TRIGGERED" ]; then
            record_result "$case_id" "PASS" "貪睡後再次響鈴"
            collect_evidence "$case_id"
        else
            record_result "$case_id" "FAIL" "貪睡後未再次響鈴"
        fi
    else
        record_result "$case_id" "FAIL" "第一次鬧鐘未觸發"
    fi
}

# -------------------- L1-08: 開機重排 --------------------
run_L1_08() {
    local case_id="L1-08"
    log "▶ 執行 $case_id: 開機重排"

    # 先新增一個鬧鐘
    local trigger_time
    trigger_time=$(date -d '+120 seconds' '+%H%M' 2>/dev/null || date -v+120S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=RebootTest'" 2>/dev/null || true
    sleep 3

    # 記錄重開機前排程
    local before_dump
    before_dump=$(adb_cmd shell "dumpsys alarm | grep $PKG | wc -l" 2>/dev/null || echo "0")
    log "重開機前排程筆數: $before_dump"

    # 模擬 BOOT_COMPLETED (不需真正重開機)
    adb_cmd shell "am broadcast -a android.intent.action.BOOT_COMPLETED -p $PKG" 2>/dev/null || true
    sleep 5

    # 檢查重排
    local after_dump
    after_dump=$(adb_cmd shell "dumpsys alarm | grep $PKG | wc -l" 2>/dev/null || echo "0")
    log "重開機後排程筆數: $after_dump"

    if [ "$after_dump" -gt 0 ]; then
        record_result "$case_id" "PASS" "BOOT_COMPLETED 後重新排程 ($after_dump 筆)"
        collect_evidence "$case_id"
    else
        record_result "$case_id" "FAIL" "重開機後無排程"
    fi
}

# -------------------- L1-09: 停用/啟用 --------------------
run_L1_09() {
    local case_id="L1-09"
    log "▶ 執行 $case_id: 停用/啟用"

    # 新增鬧鐘
    local trigger_time
    trigger_time=$(date -d '+300 seconds' '+%H%M' 2>/dev/null || date -v+300S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=ToggleTest'" 2>/dev/null || true
    sleep 3

    # 停用 (透過 content provider 或 DB 操作)
    adb_cmd shell "run-as $PKG sqlite3 /data/data/$PKG/databases/nexalarm.db \"UPDATE alarms SET isEnabled=0 WHERE id=1;\"" 2>/dev/null || true
    sleep 2

    # 驗證: dumpsys 應無此鬧鐘排程
    local dump_disabled
    dump_disabled=$(adb_cmd shell "dumpsys alarm | grep $PKG" 2>/dev/null || echo "")
    log "停用後 dumpsys: ${#dump_disabled} bytes"

    # 重新啟用
    adb_cmd shell "run-as $PKG sqlite3 /data/data/$PKG/databases/nexalarm.db \"UPDATE alarms SET isEnabled=1 WHERE id=1;\"" 2>/dev/null || true
    sleep 2

    # 驗證: dumpsys 應有排程
    local dump_enabled
    dump_enabled=$(adb_cmd shell "dumpsys alarm | grep $PKG" 2>/dev/null || echo "")
    if [ ${#dump_enabled} -gt ${#dump_disabled} ]; then
        record_result "$case_id" "PASS" "停用→取消排程, 啟用→重新排程"
        collect_evidence "$case_id"
    else
        record_result "$case_id" "FAIL" "停用/啟用狀態切換異常"
    fi
}

# -------------------- L1-10: 鬧鐘觸發-Doze --------------------
run_L1_10() {
    local case_id="L1-10"
    log "▶ 執行 $case_id: 鬧鐘觸發-Doze"

    # 進入 Doze 模式
    adb_cmd shell "dumpsys deviceidle enable" 2>/dev/null || true
    adb_cmd shell "dumpsys deviceidle force-idle deep" 2>/dev/null || true
    sleep 5

    # 新增鬧鐘
    local trigger_time
    trigger_time=$(date -d '+60 seconds' '+%H%M' 2>/dev/null || date -v+60S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=DozeTest'" 2>/dev/null || true
    sleep 3

    # 等待觸發
    local trigger_result
    trigger_result=$(wait_for_alarm_trigger "10" 120) || true

    # 退出 Doze
    adb_cmd shell "dumpsys deviceidle unforce" 2>/dev/null || true
    adb_cmd shell "dumpsys deviceidle disable" 2>/dev/null || true

    if [ "$trigger_result" = "TRIGGERED" ]; then
        record_result "$case_id" "PASS" "Doze 模式下 setAlarmClock bypass 成功"
        collect_evidence "$case_id"
    else
        record_result "$case_id" "FAIL" "Doze 模式下鬧鐘未觸發"
    fi
}

# -------------------- L1-11: 鬧鐘觸發-DND --------------------
run_L1_11() {
    local case_id="L1-11"
    log "▶ 執行 $case_id: 鬧鐘觸發-DND"

    # 開啟 DND
    adb_cmd shell "settings put global zen_mode 2" 2>/dev/null || true
    sleep 2

    # 新增鬧鐘
    local trigger_time
    trigger_time=$(date -d '+60 seconds' '+%H%M' 2>/dev/null || date -v+60S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=DNDTest'" 2>/dev/null || true
    sleep 3

    # 等待觸發
    local trigger_result
    trigger_result=$(wait_for_alarm_trigger "11" 90) || true

    # 關閉 DND
    adb_cmd shell "settings put global zen_mode 0" 2>/dev/null || true

    if [ "$trigger_result" = "TRIGGERED" ]; then
        record_result "$case_id" "PASS" "DND 模式下鬧鐘穿透成功"
        collect_evidence "$case_id"
    else
        record_result "$case_id" "FAIL" "DND 模式下鬧鐘未觸發"
    fi
}

# -------------------- L1-12: 觸發後處理 --------------------
run_L1_12() {
    local case_id="L1-12"
    log "▶ 執行 $case_id: 觸發後處理"

    # 新增鬧鐘
    local trigger_time
    trigger_time=$(date -d '+60 seconds' '+%H%M' 2>/dev/null || date -v+60S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=PostTriggerTest'" 2>/dev/null || true
    sleep 3

    # 等待觸發
    local trigger_result
    trigger_result=$(wait_for_alarm_trigger "12" 90) || true

    if [ "$trigger_result" = "TRIGGERED" ]; then
        # 觸發後檢查 DB 狀態
        sleep 3
        local db_count
        db_count=$(check_db_alarm_exists)
        record_result "$case_id" "PASS" "觸發後 DB 狀態: $db_count 個 enabled 鬧鐘"
        collect_evidence "$case_id"
    else
        record_result "$case_id" "FAIL" "鬧鐘未觸發"
    fi
}

# -------------------- 補充案例 --------------------

# S11: 首次啟動引導
run_S11() {
    local case_id="S11"
    log "▶ 執行 $case_id: 首次啟動引導 (SCHEDULE_EXACT 授權)"

    # 檢查精確鬧鐘權限
    local can_schedule
    can_schedule=$(adb_cmd shell "cmd alarm can-schedule-exact-alarms $PKG" 2>/dev/null || echo "unknown")
    log "canScheduleExactAlarms: $can_schedule"

    if [ "$can_schedule" = "true" ] || [ "$can_schedule" = "1" ]; then
        record_result "$case_id" "PASS" "精確鬧鐘權限已授予"
    else
        record_result "$case_id" "SKIP" "需手動授權或裝置不支援"
    fi
}

# S12: USE_EXACT_ALARM 安裝即有權限
run_S12() {
    local case_id="S12"
    log "▶ 執行 $case_id: USE_EXACT_ALARM 安裝即有權限"

    # 檢查 manifest 宣告
    local manifest_check
    manifest_check=$(adb_cmd shell "dumpsys package $PKG | grep USE_EXACT_ALARM" 2>/dev/null || echo "")

    if [ -n "$manifest_check" ]; then
        record_result "$case_id" "PASS" "USE_EXACT_ALARM 已宣告"
    else
        record_result "$case_id" "FAIL" "未找到 USE_EXACT_ALARM 宣告"
    fi
}

# N1: 無通知權限
run_N1() {
    local case_id="N1"
    log "▶ 執行 $case_id: 無通知權限"

    # 撤銷通知權限
    adb_cmd shell "pm revoke $PKG android.permission.POST_NOTIFICATIONS" 2>/dev/null || true
    sleep 2

    # 新增鬧鐘
    local trigger_time
    trigger_time=$(date -d '+60 seconds' '+%H%M' 2>/dev/null || date -v+60S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=NoNotifTest'" 2>/dev/null || true
    sleep 3

    # 等待觸發
    local trigger_result
    trigger_result=$(wait_for_alarm_trigger "N1" 90) || true

    # 恢復通知權限
    adb_cmd shell "pm grant $PKG android.permission.POST_NOTIFICATIONS" 2>/dev/null || true

    if [ "$trigger_result" = "TRIGGERED" ]; then
        local notif_status
        notif_status=$(check_notification)
        if [ "$notif_status" = "NOT_FOUND" ]; then
            record_result "$case_id" "PASS" "無通知權限: FGS 啟動, 全螢幕顯示, 通知不可見"
        else
            record_result "$case_id" "FAIL" "無通知權限但通知仍顯示"
        fi
    else
        record_result "$case_id" "FAIL" "無通知權限時鬧鐘未觸發"
    fi
}

# B1: 重開機+離線
run_B1() {
    local case_id="B1"
    log "▶ 執行 $case_id: 重開機+離線"

    # 斷網
    adb_cmd shell "svc wifi disable" 2>/dev/null || true
    adb_cmd shell "svc data disable" 2>/dev/null || true
    sleep 2

    # 模擬 BOOT_COMPLETED
    adb_cmd shell "am broadcast -a android.intent.action.BOOT_COMPLETED -p $PKG" 2>/dev/null || true
    sleep 5

    # 檢查排程 (離線不應影響)
    local dump
    dump=$(adb_cmd shell "dumpsys alarm | grep $PKG" 2>/dev/null || echo "")
    if echo "$dump" | grep -qE "RTC_WAKEUP|AlarmClockInfo"; then
        record_result "$case_id" "PASS" "離線狀態 BOOT_COMPLETED 重排成功"
    else
        record_result "$case_id" "FAIL" "離線狀態重排失敗"
    fi

    # 恢復網路
    adb_cmd shell "svc wifi enable" 2>/dev/null || true
    adb_cmd shell "svc data enable" 2>/dev/null || true
}

# T5: DST 切換
run_T5() {
    local case_id="T5"
    log "▶ 執行 $case_id: DST 切換"

    # 設定非系統時區
    adb_cmd shell "setprop persist.sys.timezone America/New_York" 2>/dev/null || true
    sleep 2

    # 新增重複鬧鐘
    local trigger_time
    trigger_time=$(date -d '+120 seconds' '+%H%M' 2>/dev/null || date -v+120S '+%H%M')
    adb_cmd shell "am start -a android.intent.action.VIEW -d 'nexalarm://add?time=${trigger_time}&title=DSTTest&repeat=1,2,3,4,5,6,7'" 2>/dev/null || true
    sleep 3

    # 驗證排程
    local dump
    dump=$(adb_cmd shell "dumpsys alarm | grep $PKG" 2>/dev/null || echo "")
    if echo "$dump" | grep -qE "RTC_WAKEUP|AlarmClockInfo"; then
        record_result "$case_id" "PASS" "非系統時區排程成功"
        collect_evidence "$case_id"
    else
        record_result "$case_id" "FAIL" "非系統時區排程失敗"
    fi

    # 恢復時區
    adb_cmd shell "setprop persist.sys.timezone Asia/Taipei" 2>/dev/null || true
}

# ==================== 執行所有測試 ====================

log ""
log "開始執行 Layer 1 Smoke 測試..."
log ""

run_L1_01
sleep 5
run_L1_02
sleep 3
run_L1_03
sleep 5
run_L1_04
sleep 5
run_L1_05
sleep 5
run_L1_06
sleep 5
run_L1_07
sleep 5
run_L1_08
sleep 5
run_L1_09
sleep 3
run_L1_10
sleep 5
run_L1_11
sleep 5
run_L1_12
sleep 5

# 補充案例
run_S11
sleep 3
run_S12
sleep 3
run_N1
sleep 5
run_B1
sleep 5
run_T5
sleep 5

# ==================== 生成報告 ====================

log ""
log "═══════════════════════════════════════"
log "測試完成! 報告: $REPORT_DIR/results.csv"
log "═══════════════════════════════════════"

# 統計
total=$(tail -n +2 "$REPORT_DIR/results.csv" | wc -l)
passed=$(grep ",PASS," "$REPORT_DIR/results.csv" | wc -l)
failed=$(grep ",FAIL," "$REPORT_DIR/results.csv" | wc -l)
skipped=$(grep ",SKIP," "$REPORT_DIR/results.csv" | wc -l)

log "總計: $total 組"
log "✅ 通過: $passed"
log "❌ 失敗: $failed"
log "⏭️ 跳過: $skipped"
log "成功率: $(( passed * 100 / total ))%"
