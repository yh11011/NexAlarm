#!/bin/bash
# ============================================================
# NexAlarm 測試矩陣 v2 — 批次執行管理器
#
# 用途: 依層級/裝置/條件批次執行測試
# 用法: bash run_test_batch.sh [options]
#
# Options:
#   --layer LAYER        測試層級 (1, 2, 3, all)
#   --device SERIAL      裝置 serial (可省略，使用預設)
#   --repeat N           每案例重複次數 (預設: 1)
#   --dry-run            僅顯示執行計畫，不實際執行
#   --help               顯示說明
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_BASE="$PROJECT_DIR/reports/batch_${TIMESTAMP}"

# 預設值
LAYER="all"
DEVICE=""
REPEAT=1
DRY_RUN=false

# 裝置清單 (可依需求修改)
declare -A DEVICES=(
    ["htc_desire"]="HTC_Desire-20-Pro"    # API 29, 舊版主力
    ["pixel6_emu"]="emulator-5554"        # API 31, SCHEDULE_EXACT 權限
    ["pixel7_emu"]="emulator-5556"        # API 33+, POST_NOTIFICATIONS
)

# ==================== 參數解析 ====================

while [[ $# -gt 0 ]]; do
    case $1 in
        --layer) LAYER="$2"; shift 2 ;;
        --device) DEVICE="$2"; shift 2 ;;
        --repeat) REPEAT="$2"; shift 2 ;;
        --dry-run) DRY_RUN=true; shift ;;
        --help)
            echo "用法: $0 [--layer 1|2|3|all] [--device SERIAL] [--repeat N] [--dry-run]"
            echo ""
            echo "層級說明:"
            echo "  1 - Layer 1 Smoke (18 組, ~30min)"
            echo "  2 - Layer 2 Regression (20 組, ~45min)"
            echo "  3 - Layer 3 Reliability (14 組, ~3-4hr)"
            echo "  all - 全部執行"
            exit 0
            ;;
        *) echo "未知參數: $1"; exit 1 ;;
    esac
done

mkdir -p "$REPORT_BASE"

# ==================== 工具函式 ====================

log() {
    echo "[$(date '+%H:%M:%S')] $1" | tee -a "$REPORT_BASE/batch.log"
}

check_device_connected() {
    local serial="$1"
    if [ -z "$serial" ]; then
        adb devices | grep -q "device$" && return 0 || return 1
    else
        adb -s "$serial" shell echo ok >/dev/null 2>&1 && return 0 || return 1
    fi
}

get_device_info() {
    local serial="$1"
    local model api
    if [ -z "$serial" ]; then
        model=$(adb shell getprop ro.product.model 2>/dev/null || echo "unknown")
        api=$(adb shell getprop ro.build.version.sdk 2>/dev/null || echo "unknown")
    else
        model=$(adb -s "$serial" shell getprop ro.product.model 2>/dev/null || echo "unknown")
        api=$(adb -s "$serial" shell getprop ro.build.version.sdk 2>/dev/null || echo "unknown")
    fi
    echo "$model (API $api)"
}

# ==================== 執行計畫生成 ====================

generate_plan() {
    local plan_file="$REPORT_BASE/plan.md"

    {
        echo "# NexAlarm 測試執行計畫"
        echo ""
        echo "**生成時間:** $(date '+%Y-%m-%d %H:%M:%S')"
        echo "**目標層級:** $LAYER"
        echo "**目標裝置:** ${DEVICE:-自動偵測}"
        echo "**重複次數:** $REPEAT"
        echo ""
        echo "## 裝置環境"
        echo ""
        echo "| 裝置 | 型號 | API | 用途 |"
        echo "|------|------|-----|------|"

        if [ -n "$DEVICE" ]; then
            if check_device_connected "$DEVICE"; then
                local info
                info=$(get_device_info "$DEVICE")
                echo "| $DEVICE | $info | 指定裝置 |"
            else
                echo "| $DEVICE | ❌ 未連線 | - |"
            fi
        else
            for name in "${!DEVICES[@]}"; do
                local serial="${DEVICES[$name]}"
                if check_device_connected "$serial"; then
                    local info
                    info=$(get_device_info "$serial")
                    echo "| $name | $info | 自動偵測 |"
                else
                    echo "| $name | $serial | ❌ 未連線 |"
                fi
            done
        fi

        echo ""
        echo "## 測試案例"
        echo ""

        case $LAYER in
            1)
                echo "### Layer 1 — Smoke (18 組)"
                echo ""
                echo "| Case ID | Feature | Priority | 預估時間 | 自動化 |"
                echo "|---------|---------|----------|----------|--------|"
                for i in $(seq -w 1 12); do
                    echo "| L1-$i | Smoke 測試 | P0 | ~2min | ADB |"
                done
                for id in S11 S12 S13 N1 B1 T5; do
                    echo "| $id | 補充測試 | P0-P2 | ~2min | ADB/Instr |"
                done
                ;;
            2)
                echo "### Layer 2 — Regression (20 組)"
                echo ""
                echo "| Case ID | Feature | Priority | 預估時間 | 自動化 |"
                echo "|---------|---------|----------|----------|--------|"
                for i in $(seq -w 1 20); do
                    echo "| L2-$i | Regression | P1 | ~2min | ADB/Instr |"
                done
                ;;
            3)
                echo "### Layer 3 — Reliability (14 組)"
                echo ""
                echo "| Case ID | Feature | Priority | 預估時間 | 自動化 |"
                echo "|---------|---------|----------|----------|--------|"
                for i in $(seq -w 1 14); do
                    echo "| L3-$i | Reliability | P2 | ~15min | Instr |"
                done
                ;;
            all)
                echo "### 全部測試 (52 組)"
                echo ""
                echo "| 層級 | 案例數 | 預估時間 | 自動化程度 |"
                echo "|------|--------|----------|-----------|"
                echo "| Layer 1 | 18 | ~30min | 90% ADB |"
                echo "| Layer 2 | 20 | ~45min | 70% ADB + 30% Instr |"
                echo "| Layer 3 | 14 | ~3-4hr | 50% Instr + 50% Manual |"
                echo "| **總計** | **52** | **~4-5hr** | **~70%** |"
                ;;
        esac

        echo ""
        echo "## 執行順序"
        echo ""
        echo "1. 檢查裝置連線"
        echo "2. 安裝/更新 App"
        echo "3. 執行測試"
        echo "4. 收集證據"
        echo "5. 生成報告"
    } > "$plan_file"

    log "📋 執行計畫已生成: $plan_file"
    cat "$plan_file"
}

# ==================== 測試執行 ====================

run_layer1() {
    local device_serial="${1:-}"
    log "▶ 開始 Layer 1 Smoke 測試..."

    if [ "$DRY_RUN" = true ]; then
        log "[DRY-RUN] 將執行: bash $SCRIPT_DIR/run_layer1_smoke.sh $device_serial"
        return 0
    fi

    bash "$SCRIPT_DIR/run_layer1_smoke.sh" "$device_serial" 2>&1 | tee -a "$REPORT_BASE/layer1.log"
}

run_layer2() {
    local device_serial="${1:-}"
    log "▶ 開始 Layer 2 Regression 測試..."

    if [ "$DRY_RUN" = true ]; then
        log "[DRY-RUN] 將執行: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nexalarm.app.test.Layer2RegressionTest"
        return 0
    fi

    cd "$PROJECT_DIR"
    ./gradlew connectedAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.class=com.nexalarm.app.test.Layer2RegressionTest \
        2>&1 | tee -a "$REPORT_BASE/layer2.log"
}

run_layer3() {
    local device_serial="${1:-}"
    log "▶ 開始 Layer 3 Reliability 測試..."

    if [ "$DRY_RUN" = true ]; then
        log "[DRY-RUN] 將執行: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nexalarm.app.test.AlarmReliabilityTest"
        return 0
    fi

    cd "$PROJECT_DIR"
    ./gradlew connectedAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.class=com.nexalarm.app.test.AlarmReliabilityTest \
        2>&1 | tee -a "$REPORT_BASE/layer3.log"
}

# ==================== 報告生成 ====================

generate_report() {
    log "▶ 生成批次測試報告..."

    local report_file="$REPORT_BASE/summary.md"
    {
        echo "# NexAlarm 測試批次報告"
        echo ""
        echo "**執行時間:** $(date '+%Y-%m-%d %H:%M:%S')"
        echo "**層級:** $LAYER"
        echo "**裝置:** ${DEVICE:-自動偵測}"
        echo ""

        # 統計各層結果
        if [ -f "$REPORT_BASE/layer1_results.csv" ]; then
            local total passed failed
            total=$(tail -n +2 "$REPORT_BASE/layer1_results.csv" 2>/dev/null | wc -l || echo "0")
            passed=$(grep ",PASS," "$REPORT_BASE/layer1_results.csv" 2>/dev/null | wc -l || echo "0")
            failed=$(grep ",FAIL," "$REPORT_BASE/layer1_results.csv" 2>/dev/null | wc -l || echo "0")
            echo "## Layer 1 Smoke"
            echo "- 總計: $total"
            echo "- 通過: $passed"
            echo "- 失敗: $failed"
            echo "- 成功率: $(( total > 0 ? passed * 100 / total : 0 ))%"
            echo ""
        fi

        echo "## 詳細報告"
        echo ""
        echo "請查看以下目錄獲取詳細證據:"
        echo "- CSV 原始數據: \`$REPORT_BASE/*.csv\`"
        echo "- 證據檔案: \`$REPORT_BASE/*_evidence.txt\`"
        echo "- 執行日誌: \`$REPORT_BASE/*.log\`"
    } > "$report_file"

    log "📊 報告已生成: $report_file"
}

# ==================== 主流程 ====================

main() {
    log "═══════════════════════════════════════"
    log "NexAlarm 測試批次管理器"
    log "═══════════════════════════════════════"

    # 生成執行計畫
    generate_plan

    # 檢查裝置
    local target_device="$DEVICE"
    if [ -z "$target_device" ]; then
        # 自動偵測第一台連線裝置
        target_device=$(adb devices | grep "device$" | head -1 | awk '{print $1}')
        if [ -z "$target_device" ]; then
            log "❌ 無可用裝置，請連接裝置或指定 --device"
            exit 1
        fi
        log "📱 自動偵測裝置: $target_device ($(get_device_info "$target_device"))"
    else
        if ! check_device_connected "$target_device"; then
            log "❌ 裝置 $target_device 未連線"
            exit 1
        fi
        log "📱 使用裝置: $target_device ($(get_device_info "$target_device"))"
    fi

    # 安裝 App
    log "📦 安裝/更新 App..."
    if [ "$DRY_RUN" = false ]; then
        cd "$PROJECT_DIR"
        ./gradlew installDebug 2>&1 | tail -5
    fi

    # 執行測試
    case $LAYER in
        1) run_layer1 "$target_device" ;;
        2) run_layer2 "$target_device" ;;
        3) run_layer3 "$target_device" ;;
        all)
            run_layer1 "$target_device"
            run_layer2 "$target_device"
            run_layer3 "$target_device"
            ;;
    esac

    # 生成報告
    generate_report

    log "═══════════════════════════════════════"
    log "批次測試完成! 報告: $REPORT_BASE"
    log "═══════════════════════════════════════"
}

main "$@"
