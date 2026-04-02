#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
NexAlarm ADB 可靠性測試腳本  v3.0
====================================
架構：
  - 透過 nexalarm:// Deep Link 批次建立鬧鐘
  - 監聽 NexAlarmTest logcat tag（4 個事件：SCHEDULED / CANCELLED / RECEIVED / SERVICE_START）
  - 每次建鬧鐘後用 dumpsys alarm 確認進入系統排程
  - 測試前做 exact alarm 權限檢查；缺權限則整批標記 INEXACT
  - force-stop 情境獨立輸出，不混入可靠性統計
  - Doze 批次記錄 API 類型與 idle 狀態
  - 輸出 JSON + CSV

v3.0 新功能：
  - 測試開始自動安裝最新 debug APK（--no-install 可跳過）
  - 測試帳號自動登入（TEST_ACCOUNT_EMAIL / TEST_ACCOUNT_PASSWORD）
  - 飛航模式離線測試（--offline）
  - --hours N：設定總測試時長（支援 9 小時以上），自動計算批次窗口
  - 移除鬧鐘數量上限（原本最多 100，現在無上限）
  - 測試結束自動清除所有測試鬧鐘（--no-cleanup 可跳過）

使用：
  python3 alarm_reliability_test.py -n 30
  python3 alarm_reliability_test.py -n 50 --no-doze
  python3 alarm_reliability_test.py -n 100 --hours 3
  python3 alarm_reliability_test.py -n 200 --hours 9 --offline
  python3 alarm_reliability_test.py -n 20 --scenarios normal battery_saver dnd
  python3 alarm_reliability_test.py -n 10 --scenarios screen_off app_stopped --no-install

前置需求：
  adb 在 PATH，裝置已啟用 USB 偵錯，NexAlarm 已安裝
  （Doze 測試需裝置支援 dumpsys deviceidle force-idle）
"""

import argparse
import csv
import glob as _glob
import json
import os
import random
import re
import subprocess
import sys
import threading
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple

# ── 測試帳號 ──────────────────────────────────────────────────────────────────
TEST_ACCOUNT_EMAIL    = "nexalarm.test@gmail.com"
TEST_ACCOUNT_PASSWORD = "NexAlarm@Test2026"

# ── APK 路徑（相對於腳本目錄）────────────────────────────────────────────────
_SCRIPT_DIR  = os.path.dirname(os.path.abspath(__file__))
APK_SEARCH_DIR = os.path.join(_SCRIPT_DIR, "..", "app", "build", "outputs", "apk", "debug")

# ── 常數 ──────────────────────────────────────────────────────────────────────

PACKAGE = "com.nexalarm.app"

TOLERANCE_PERFECT  = 3.0    # ≤ 3 s  → 完全準確
TOLERANCE_MILD     = 60.0   # 3–60 s → 輕微延遲；> 60 s → 嚴重延遲

# 批次時間設計（秒）——當未指定 --hours 時使用
BATCH_START_OFFSET  = 75    # 批次開始後，第一個鬧鐘最早幾秒後觸發
BATCH_WINDOW        = 60    # 批次觸發窗口寬度（所有鬧鐘分散在此範圍內）
BATCH_TAIL_WAIT     = 25    # 最後一個鬧鐘觸發後的緩衝等待
ADD_DELAY           = 1.3   # 每個鬧鐘新增後的等待秒數（等 DB 寫入 + AlarmScheduler log）

# ── 資料結構 ──────────────────────────────────────────────────────────────────

@dataclass
class NexEvent:
    """NexAlarmTest logcat 事件"""
    kind: str                       # SCHEDULED / CANCELLED / RECEIVED / SERVICE_START
    alarm_id: int
    title: str
    device_ts_ms: int               # 裝置端 System.currentTimeMillis()
    host_time: datetime             # 主機端捕捉時間
    api: Optional[str] = None       # SCHEDULED: setAlarmClock | setAndAllowWhileIdle
    trigger_ms: Optional[int] = None  # SCHEDULED: 預計觸發的 epoch ms


@dataclass
class AlarmTestCase:
    test_id: str
    scenario: str
    host_scheduled_time: datetime   # 主機側預期觸發時間（用於計算誤差）
    should_ring: bool
    alarm_db_id: Optional[int] = None

    # NexAlarmTest 事件
    ev_scheduled:     Optional[NexEvent] = None
    ev_cancelled:     Optional[NexEvent] = None
    ev_received:      Optional[NexEvent] = None
    ev_service_start: Optional[NexEvent] = None

    # dumpsys 確認結果
    dumpsys_found:  Optional[bool] = None   # True/False/None(未檢查)
    dumpsys_api:    Optional[str]  = None   # 從 dumpsys 讀到的 alarm 類型

    # Doze 批次附加資訊
    doze_idle_state: Optional[str] = None   # e.g. "IDLE", "IDLE_PENDING", "ACTIVE"

    # ── 計算屬性 ──────────────────────────────────────────────────────────────

    @property
    def actual_ring_time(self) -> Optional[datetime]:
        return self.ev_received.host_time if self.ev_received else None

    @property
    def delay_seconds(self) -> Optional[float]:
        """RECEIVED.host_time - host_scheduled_time（正 = 延遲，負 = 提早）"""
        if self.ev_received:
            return (self.ev_received.host_time - self.host_scheduled_time).total_seconds()
        return None

    @property
    def service_latency_ms(self) -> Optional[float]:
        """SERVICE_START.device_ts - RECEIVED.device_ts（ms）"""
        if self.ev_received and self.ev_service_start:
            return float(self.ev_service_start.device_ts_ms - self.ev_received.device_ts_ms)
        return None

    @property
    def alarm_api(self) -> Optional[str]:
        if self.ev_scheduled:
            return self.ev_scheduled.api
        return self.dumpsys_api

    @property
    def result_label(self) -> str:
        d = self.delay_seconds
        if self.should_ring:
            if d is None:        return "missed"
            if d < -TOLERANCE_PERFECT: return "early"
            if abs(d) <= TOLERANCE_PERFECT: return "perfect"
            if d <= TOLERANCE_MILD: return "mild_delay"
            return "severe_delay"
        else:
            return "false_ring" if self.ev_received else "correct_silence"


@dataclass
class ScenarioStats:
    key:   str
    name:  str
    perfect:       int = 0
    mild_delay:    int = 0
    severe_delay:  int = 0
    early:         int = 0
    missed:        int = 0
    false_ring:    int = 0
    delays:        List[float] = field(default_factory=list)
    svc_latencies: List[float] = field(default_factory=list)  # ms

    @property
    def total_expected(self) -> int:
        return self.perfect + self.mild_delay + self.severe_delay + self.early + self.missed

    @property
    def total_not_expected(self) -> int:
        return self.false_ring


# ── 情境定義 ──────────────────────────────────────────────────────────────────

ALL_SCENARIOS: Dict[str, str] = {
    "normal":        "正常環境",
    "battery_saver": "省電模式 (Battery Saver)",
    "doze":          "Doze 深度閒置",
    "dnd":           "勿擾模式 (DND)",
    "screen_off":    "螢幕關閉 (Screen Off)",
    "app_stopped":   "App 強制停止（獨立統計）",
}

# force_stop 不計入可靠性統計
RELIABILITY_SCENARIOS = [s for s in ALL_SCENARIOS if s != "app_stopped"]

# ── ADB 工具 ──────────────────────────────────────────────────────────────────

def adb(*args, timeout: int = 10) -> Tuple[str, int]:
    try:
        r = subprocess.run(
            ["adb"] + list(args),
            capture_output=True, text=True, timeout=timeout
        )
        return r.stdout.strip(), r.returncode
    except subprocess.TimeoutExpired:
        return "", -1
    except FileNotFoundError:
        print("[FATAL] adb 不在 PATH，請安裝 Android SDK Platform Tools")
        sys.exit(1)


def adb_shell(*args, timeout: int = 10) -> str:
    out, _ = adb("shell", *args, timeout=timeout)
    return out


def check_device() -> bool:
    out, _ = adb("devices")
    return bool(re.search(r'\t(device|unauthorized)', out))


def get_device_info() -> str:
    model = adb_shell("getprop", "ro.product.model")
    api   = adb_shell("getprop", "ro.build.version.sdk")
    return f"{model} (API {api})"


def get_screen_size() -> Tuple[int, int]:
    out = adb_shell("wm", "size")
    m = re.search(r'(\d+)x(\d+)', out)
    if m:
        return int(m.group(1)), int(m.group(2))
    return 1080, 1920


def open_deep_link(url: str):
    adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", url)
    time.sleep(0.5)


def clear_logcat():
    adb("logcat", "-c")


def dump_logcat_tag(tag: str) -> str:
    out, _ = adb("logcat", "-d", "-s", tag, timeout=8)
    return out


# ── APK 安裝 ──────────────────────────────────────────────────────────────────

def find_latest_apk() -> Optional[str]:
    """找 debug APK 目錄，回傳最新修改時間的那個"""
    pattern = os.path.join(APK_SEARCH_DIR, "*.apk")
    apks = _glob.glob(pattern)
    if not apks:
        return None
    return max(apks, key=os.path.getmtime)


def install_latest_apk() -> bool:
    """安裝最新 debug APK 到已連接裝置，回傳是否成功"""
    apk = find_latest_apk()
    if not apk:
        _log("⚠️  找不到 APK（路徑: " + APK_SEARCH_DIR + "）")
        _log("    請先執行: ./gradlew assembleDebug")
        return False

    _log(f"📦 安裝 APK: {os.path.basename(apk)}")
    mtime = datetime.fromtimestamp(os.path.getmtime(apk)).strftime("%Y-%m-%d %H:%M:%S")
    _log(f"   編譯時間: {mtime}")

    out, code = adb("install", "-r", "-t", apk, timeout=180)
    if code == 0 and "Success" in out:
        _log("✅ 安裝成功")
        time.sleep(2)
        return True
    else:
        _log(f"❌ 安裝失敗（code={code}）: {out[:300]}")
        return False


# ── 飛航模式 ──────────────────────────────────────────────────────────────────

def set_airplane_mode(on: bool):
    """切換飛航模式（離線/上線）"""
    val = "1" if on else "0"
    adb_shell("settings", "put", "global", "airplane_mode_on", val)
    # 發送廣播讓系統確實生效
    adb_shell("am", "broadcast",
              "-a", "android.intent.action.AIRPLANE_MODE",
              "--ez", "state", "true" if on else "false")
    time.sleep(2)
    _log(f"飛航模式: {'ON ✈️  (離線測試中)' if on else 'OFF (已恢復網路)'}")


# ── 事先檢查：精確鬧鐘權限 ───────────────────────────────────────────────────

def check_exact_alarm_permission() -> Tuple[bool, str]:
    """
    回傳 (has_permission, detail_string)
    API < 31 → 不需要動態權限，視為 True
    """
    api_str = adb_shell("getprop", "ro.build.version.sdk")
    try:
        api_level = int(api_str.strip())
    except ValueError:
        return True, "無法取得 API level，跳過檢查"

    if api_level < 31:
        return True, f"API {api_level} 不需要 SCHEDULE_EXACT_ALARM 動態權限"

    # AppOps 方式（API 31+）
    ops_out = adb_shell("appops", "get", PACKAGE, "SCHEDULE_EXACT_ALARM")
    if "allow" in ops_out.lower():
        return True, f"SCHEDULE_EXACT_ALARM = allow  (API {api_level})"

    # 第二道確認：透過 AlarmManager 能力位元
    perm_out = adb_shell("cmd", "alarm", "get-exact-alarm-permission", PACKAGE)
    if "true" in perm_out.lower() or "granted" in perm_out.lower():
        return True, f"cmd alarm 回報已授權  (API {api_level})"

    return False, f"SCHEDULE_EXACT_ALARM 未授權 (API {api_level})。" \
                  "請至「設定 > 應用程式 > 特殊應用程式存取 > 鬧鐘和提醒」手動允許。"


# ── dumpsys alarm 驗證 ────────────────────────────────────────────────────────

def verify_in_dumpsys_alarm(alarm_id: int) -> Tuple[bool, str]:
    """
    確認鬧鐘是否真的在系統 AlarmManager 排程中。
    回傳 (found, api_hint)
    """
    time.sleep(0.4)
    out = adb_shell("dumpsys", "alarm", timeout=12)
    lines = out.splitlines()

    # ① 先找 Alarm clocks 區段（setAlarmClock 會列在此）
    in_clocks = False
    for line in lines:
        if re.match(r'\s*Alarm clocks:', line):
            in_clocks = True
        if in_clocks and PACKAGE in line:
            # 同一行或相鄰行含 alarm_id requestCode
            if str(alarm_id) in line:
                return True, "setAlarmClock"
            return True, "setAlarmClock(id_unconfirmed)"
        if in_clocks and line.strip() and not line.startswith(" "):
            in_clocks = False  # 離開 Alarm clocks 區段

    # ② 一般排程區：找含 PACKAGE 且 requestCode=alarm_id
    pkg_block_lines = [l for l in lines if PACKAGE in l]
    for line in pkg_block_lines:
        if f"requestCode={alarm_id}" in line:
            return True, "inexact_or_exact"
    if pkg_block_lines:
        return True, "present(requestCode_mismatch)"

    return False, "not_found"


# ── 裝置情境控制 ──────────────────────────────────────────────────────────────

def _log(msg: str):
    ts = datetime.now().strftime("%H:%M:%S")
    print(f"[{ts}] {msg}", flush=True)


def set_battery_saver(on: bool):
    adb_shell("settings", "put", "global", "low_power", "1" if on else "0")
    if on:
        adb_shell("cmd", "battery", "unplug")
    else:
        adb_shell("cmd", "battery", "reset")
    time.sleep(1)
    _log(f"省電模式: {'ON ⚡' if on else 'OFF'}")


def set_doze(on: bool) -> str:
    """回傳套用後的 idle 狀態字串"""
    if on:
        adb_shell("cmd", "battery", "unplug")
        out = adb_shell("dumpsys", "deviceidle", "force-idle", "deep")
        _log(f"Doze force-idle: {out.strip()[:80]}")
    else:
        adb_shell("dumpsys", "deviceidle", "disable")
        adb_shell("cmd", "battery", "reset")
        _log("Doze 已停用")
    time.sleep(1.5)
    return get_doze_state()


def get_doze_state() -> str:
    out = adb_shell("dumpsys", "deviceidle")
    m = re.search(r'mState=(\S+)', out)
    return m.group(1) if m else "UNKNOWN"


def set_dnd(on: bool):
    adb_shell("cmd", "notification", "set_zen", "1" if on else "0")
    _log(f"勿擾模式: {'ON' if on else 'OFF'}")


def set_screen(on: bool):
    if on:
        adb_shell("input", "keyevent", "224")  # WAKEUP
        time.sleep(0.5)
        adb_shell("input", "keyevent", "82")   # MENU / unlock swipe
    else:
        power = adb_shell("dumpsys", "power")
        if "mWakefulness=Awake" in power:
            adb_shell("input", "keyevent", "26")  # POWER
    time.sleep(0.5)
    _log(f"螢幕: {'ON' if on else 'OFF'}")


def force_stop_app():
    adb_shell("am", "force-stop", PACKAGE)
    _log("App 已強制停止")
    time.sleep(1)


def start_app():
    adb_shell("monkey", "-p", PACKAGE, "-c",
              "android.intent.category.LAUNCHER", "1")
    time.sleep(2)
    _log("App 已啟動")


def dismiss_ringing():
    adb_shell("am", "broadcast",
              "-a", "com.nexalarm.app.ALARM_DISMISS", "-p", PACKAGE)
    time.sleep(0.3)


def reset_all(restore_network: bool = True):
    """重置所有情境，可選擇是否恢復網路"""
    set_battery_saver(False)
    set_doze(False)
    set_dnd(False)
    set_screen(True)
    if restore_network:
        set_airplane_mode(False)
    start_app()
    time.sleep(1)


def setup_scenario(key: str):
    _log(f"\n=== 設定情境: {ALL_SCENARIOS[key]} ===")
    set_battery_saver(False)
    set_doze(False)
    set_dnd(False)
    set_screen(True)
    if key == "normal":
        start_app()
    elif key == "battery_saver":
        start_app()
        set_battery_saver(True)
    elif key == "doze":
        start_app()
        set_doze(True)
    elif key == "dnd":
        start_app()
        set_dnd(True)
    elif key == "screen_off":
        start_app()
        time.sleep(1)
        set_screen(False)
    elif key == "app_stopped":
        start_app()
        time.sleep(1)
        force_stop_app()


def teardown_scenario(key: str):
    if key == "battery_saver":
        set_battery_saver(False)
    elif key == "doze":
        set_doze(False)
    elif key == "dnd":
        set_dnd(False)
    elif key == "screen_off":
        set_screen(True)
    elif key == "app_stopped":
        start_app()
    time.sleep(0.5)


# ── 測試帳號登入 ──────────────────────────────────────────────────────────────

def _get_ui_dump() -> str:
    """取得 UIAutomator UI 結構 XML"""
    adb_shell("uiautomator", "dump", "/sdcard/ui_nexalarm_dump.xml", timeout=10)
    out, _ = adb("shell", "cat", "/sdcard/ui_nexalarm_dump.xml", timeout=8)
    return out


def _ui_find_by_text(xml_str: str, *texts: str) -> Optional[Tuple[int, int]]:
    """在 UIAutomator XML 中找到含有特定 text 或 content-desc 的元素中心座標"""
    try:
        root = ET.fromstring(xml_str)
    except ET.ParseError:
        return None
    for node in root.iter("node"):
        node_text = node.get("text", "")
        node_desc = node.get("content-desc", "")
        for t in texts:
            if node_text == t or node_desc == t:
                bounds = node.get("bounds", "")
                m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
                if m:
                    cx = (int(m.group(1)) + int(m.group(3))) // 2
                    cy = (int(m.group(2)) + int(m.group(4))) // 2
                    return cx, cy
    return None


def _ui_find_edittexts(xml_str: str) -> List[Tuple[int, int]]:
    """找出所有 EditText 元素的中心座標（按 y 軸排序）"""
    results = []
    try:
        root = ET.fromstring(xml_str)
    except ET.ParseError:
        return results
    for node in root.iter("node"):
        if "EditText" in node.get("class", ""):
            bounds = node.get("bounds", "")
            m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
            if m:
                cx = (int(m.group(1)) + int(m.group(3))) // 2
                cy = (int(m.group(2)) + int(m.group(4))) // 2
                results.append((cx, cy))
    results.sort(key=lambda p: p[1])  # 按 y 軸排序（email 在上，password 在下）
    return results


def _adb_input_text_safe(text: str):
    """安全輸入含特殊字元（@ . !）的文字"""
    # adb shell input text 對 @ 等字元有問題，逐字元用 keyevent 輸入
    # 使用 am broadcast 傳入 clipboard 再貼上（相容性最好）
    escaped = text.replace("\\", "\\\\").replace("'", "\\'").replace(" ", "%s")
    adb_shell("input", "text", escaped, timeout=5)


def login_test_account() -> bool:
    """
    使用 UIAutomator 自動登入測試帳號。
    回傳 True = 登入成功（或已是登入狀態），False = 跳過。
    """
    _log(f"🔑 登入測試帳號: {TEST_ACCOUNT_EMAIL}")

    start_app()
    time.sleep(2)

    # ── Step 1: 檢查是否已登入 ──────────────────────────────────────────────
    xml = _get_ui_dump()
    if _ui_find_by_text(xml, "登出", "Logout", "Sign Out"):
        _log("✅ 已是登入狀態，跳過登入流程")
        return True

    # ── Step 2: 開啟側邊抽屜 ────────────────────────────────────────────────
    w, h = get_screen_size()
    adb_shell("input", "swipe",
              "0", str(h // 2),
              str(w // 3), str(h // 2),
              "400")
    time.sleep(1.2)

    # ── Step 3: 點選「帳號」 ────────────────────────────────────────────────
    xml = _get_ui_dump()
    account_pos = _ui_find_by_text(xml, "帳號", "Account")
    if not account_pos:
        _log("⚠️  找不到「帳號」選項，請手動登入後再執行測試")
        return False

    adb_shell("input", "tap", str(account_pos[0]), str(account_pos[1]))
    time.sleep(2)

    # ── Step 4: 再次確認是否已登入 ──────────────────────────────────────────
    xml = _get_ui_dump()
    if _ui_find_by_text(xml, "登出", "Logout", "Sign Out"):
        _log("✅ 已是登入狀態")
        return True

    # ── Step 5: 找 EditText 欄位（email 在前，password 在後）───────────────
    fields = _ui_find_edittexts(xml)
    if len(fields) < 2:
        _log(f"⚠️  只找到 {len(fields)} 個輸入欄位（需要 2 個），嘗試備用方式")
        # 備用：點擊 email hint 文字
        email_pos = _ui_find_by_text(xml, "電子郵件", "Email", "帳號/電子郵件",
                                     "email", "Username")
        if email_pos:
            fields_fallback = [email_pos]
            adb_shell("input", "tap", str(email_pos[0]), str(email_pos[1]))
            time.sleep(0.5)
            xml2 = _get_ui_dump()
            fields = _ui_find_edittexts(xml2)

        if len(fields) < 2:
            _log("⚠️  無法找到登入欄位，請手動登入")
            return False

    email_field, password_field = fields[0], fields[1]

    # ── Step 6: 輸入 Email ──────────────────────────────────────────────────
    adb_shell("input", "tap", str(email_field[0]), str(email_field[1]))
    time.sleep(0.5)
    adb_shell("input", "keyevent", "123")  # KEYCODE_MOVE_END（確保游標在最後）
    # 清除現有內容
    adb_shell("input", "keyevent", "KEYCODE_CTRL_A")
    time.sleep(0.2)
    _adb_input_text_safe(TEST_ACCOUNT_EMAIL)
    time.sleep(0.5)

    # ── Step 7: 輸入 Password ───────────────────────────────────────────────
    adb_shell("input", "tap", str(password_field[0]), str(password_field[1]))
    time.sleep(0.5)
    adb_shell("input", "keyevent", "KEYCODE_CTRL_A")
    time.sleep(0.2)
    _adb_input_text_safe(TEST_ACCOUNT_PASSWORD)
    time.sleep(0.5)

    # ── Step 8: 隱藏鍵盤並點擊登入按鈕 ─────────────────────────────────────
    adb_shell("input", "keyevent", "4")  # BACK（收鍵盤）
    time.sleep(0.5)

    xml = _get_ui_dump()
    login_pos = _ui_find_by_text(xml, "登入", "Login", "Sign In", "登錄")
    if not login_pos:
        _log("⚠️  找不到登入按鈕，嘗試按 Enter")
        adb_shell("input", "keyevent", "66")  # ENTER
    else:
        adb_shell("input", "tap", str(login_pos[0]), str(login_pos[1]))

    _log("   登入請求已送出，等待回應...")
    time.sleep(4)

    # ── Step 9: 確認登入結果 ────────────────────────────────────────────────
    xml = _get_ui_dump()
    if _ui_find_by_text(xml, "登出", "Logout", "Sign Out"):
        _log("✅ 登入成功！")
        return True
    else:
        _log("⚠️  無法確認登入狀態（可能需要手動操作）")
        return False


# ── 測試結束清除 ──────────────────────────────────────────────────────────────

def cleanup_test_alarms(test_cases: List[AlarmTestCase]):
    """刪除所有測試產生的鬧鐘（透過 Deep Link nexalarm://delete?id=N）"""
    _log("\n🧹 清除測試鬧鐘...")
    ids_to_delete = [tc.alarm_db_id for tc in test_cases
                     if tc.alarm_db_id is not None]
    if not ids_to_delete:
        _log("   沒有需要清除的鬧鐘")
        return

    deleted = 0
    failed  = 0
    for alarm_id in ids_to_delete:
        open_deep_link(f"nexalarm://delete?id={alarm_id}")
        deleted += 1
        if deleted % 10 == 0:
            _log(f"   已刪除 {deleted}/{len(ids_to_delete)}...")
        time.sleep(0.15)  # 稍微放慢，避免 DB 寫入競爭

    _log(f"✅ 清除完成：{deleted} 個鬧鐘已刪除")


# ── NexAlarmTest logcat 監聽 ──────────────────────────────────────────────────
# 格式：
#   SCHEDULED   |id=X|title=Y|triggerMs=Z|api=A|ts=T
#   CANCELLED   |id=X|ts=T
#   RECEIVED    |id=X|title=Y|ts=T
#   SERVICE_START|id=X|title=Y|ts=T

_NEX_RE = re.compile(
    r'NexAlarmTest\s*:\s*'
    r'(?P<kind>SCHEDULED|CANCELLED|RECEIVED|SERVICE_START)'
    r'\|id=(?P<alarm_id>\d+)'
    r'(?:\|title=(?P<title>[^|]*))?'
    r'(?:\|triggerMs=(?P<trigger_ms>\d+))?'
    r'(?:\|api=(?P<api>\w+))?'
    r'\|ts=(?P<ts>\d+)'
)


class LogcatMonitor:
    def __init__(self):
        self._proc:   Optional[subprocess.Popen] = None
        self._thread: Optional[threading.Thread] = None
        self._running = False
        self._lock = threading.Lock()
        # title → list of NexEvent（同一 title 可能被觸發多次，取最新）
        self.events_by_title: Dict[str, List[NexEvent]] = {}
        # alarm_id → list of NexEvent
        self.events_by_id: Dict[int, List[NexEvent]] = {}

    def start(self):
        clear_logcat()
        self._running = True
        self._thread = threading.Thread(target=self._loop, daemon=True)
        self._thread.start()

    def stop(self):
        self._running = False
        if self._proc:
            try:
                self._proc.terminate()
            except Exception:
                pass

    def _loop(self):
        self._proc = subprocess.Popen(
            ["adb", "logcat", "-s", "NexAlarmTest:I"],
            stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
            text=True, bufsize=1
        )
        for raw in self._proc.stdout:
            if not self._running:
                break
            m = _NEX_RE.search(raw.strip())
            if not m:
                continue
            ev = NexEvent(
                kind=m.group("kind"),
                alarm_id=int(m.group("alarm_id")),
                title=(m.group("title") or "").strip(),
                device_ts_ms=int(m.group("ts")),
                host_time=datetime.now(),
                api=m.group("api"),
                trigger_ms=int(m.group("trigger_ms")) if m.group("trigger_ms") else None,
            )
            with self._lock:
                self.events_by_title.setdefault(ev.title, []).append(ev)
                self.events_by_id.setdefault(ev.alarm_id, []).append(ev)

            icon = {"SCHEDULED": "📅", "CANCELLED": "🗑",
                    "RECEIVED": "🔔", "SERVICE_START": "🔊"}.get(ev.kind, "?")
            if ev.kind in ("RECEIVED", "SERVICE_START"):
                _log(f"  {icon} {ev.kind}  {ev.title} (id={ev.alarm_id}) "
                     f"@ {ev.host_time.strftime('%H:%M:%S.%f')[:12]}")
                if ev.kind == "RECEIVED":
                    dismiss_ringing()

    def latest(self, title: str, kind: str) -> Optional[NexEvent]:
        with self._lock:
            evs = [e for e in self.events_by_title.get(title, []) if e.kind == kind]
            return evs[-1] if evs else None

    def latest_by_id(self, alarm_id: int, kind: str) -> Optional[NexEvent]:
        with self._lock:
            evs = [e for e in self.events_by_id.get(alarm_id, []) if e.kind == kind]
            return evs[-1] if evs else None


# ── 鬧鐘新增 ─────────────────────────────────────────────────────────────────

def _get_scheduled_id_from_logcat(test_id: str,
                                   monitor: LogcatMonitor) -> Optional[int]:
    """等待 SCHEDULED 事件並回傳 alarm_id（最多等 3 秒）"""
    deadline = time.time() + 3.0
    while time.time() < deadline:
        ev = monitor.latest(test_id, "SCHEDULED")
        if ev:
            return ev.alarm_id
        time.sleep(0.1)
    # fallback：從 AlarmScheduler 傳統 log 擷取
    out = dump_logcat_tag("AlarmScheduler:D")
    matches = re.findall(r'Scheduled alarm (\d+) at ', out)
    return int(matches[-1]) if matches else None


def add_alarm(test_id: str, alarm_time: datetime, scenario: str,
              monitor: LogcatMonitor,
              keep_prob: float = 0.7,
              check_dumpsys: bool = True) -> AlarmTestCase:
    """
    新增鬧鐘 → 等待 SCHEDULED log → 確認 dumpsys → 隨機決定是否刪除
    """
    time_str = alarm_time.strftime("%H%M")
    url = f"nexalarm://add?time={time_str}&title={test_id}"

    open_deep_link(url)
    time.sleep(ADD_DELAY)

    # 從 NexAlarmTest log 取得 alarm_id
    alarm_id = _get_scheduled_id_from_logcat(test_id, monitor)

    tc = AlarmTestCase(
        test_id=test_id,
        scenario=scenario,
        host_scheduled_time=alarm_time,
        should_ring=random.random() < keep_prob,
        alarm_db_id=alarm_id,
    )

    # 填入 SCHEDULED 事件
    if alarm_id is not None:
        tc.ev_scheduled = monitor.latest(test_id, "SCHEDULED")

    # dumpsys 驗證
    if check_dumpsys and alarm_id is not None:
        tc.dumpsys_found, tc.dumpsys_api = verify_in_dumpsys_alarm(alarm_id)
        dsym = "✔dumpsys" if tc.dumpsys_found else "✘dumpsys"
    else:
        dsym = "(skip)"

    # 刪除或保留
    if not tc.should_ring:
        if alarm_id is not None:
            open_deep_link(f"nexalarm://delete?id={alarm_id}")
            time.sleep(0.3)
        _log(f"  {test_id} [{time_str}] 刪除 {dsym} (id={alarm_id})")
    else:
        _log(f"  {test_id} [{time_str}] 保留 {dsym} (id={alarm_id}, api={tc.alarm_api})")

    return tc


# ── 主測試類別 ────────────────────────────────────────────────────────────────

class AlarmReliabilityTester:

    def __init__(self, count: int, scenarios: List[str], keep_prob: float,
                 hours: Optional[float] = None,
                 offline: bool = False,
                 do_install: bool = True,
                 do_login: bool = True,
                 do_cleanup: bool = True):
        self.count      = count
        self.scenarios  = scenarios
        self.keep_prob  = keep_prob
        self.hours      = hours          # None = 使用預設窗口；float = 分配到 N 小時
        self.offline    = offline        # 是否啟用飛航模式
        self.do_install = do_install
        self.do_login   = do_login
        self.do_cleanup = do_cleanup

        self.test_cases: List[AlarmTestCase] = []
        self.monitor = LogcatMonitor()
        self.stats: Dict[str, ScenarioStats] = {
            k: ScenarioStats(k, ALL_SCENARIOS[k]) for k in scenarios
        }
        self.has_exact_alarm: bool = True
        self.doze_idle_at_batch_start: Optional[str] = None

    def _calc_batch_window(self) -> int:
        """計算本次測試的批次窗口寬度（秒）"""
        if self.hours:
            total_sec   = int(self.hours * 3600)
            per_scenario = total_sec // len(self.scenarios)
            window = per_scenario - BATCH_START_OFFSET - BATCH_TAIL_WAIT
            return max(60, window)
        return BATCH_WINDOW

    # ── 執行 ──────────────────────────────────────────────────────────────────

    def run(self):
        batch_window = self._calc_batch_window()

        _log("=" * 65)
        _log(f"NexAlarm 可靠性測試 v3.0  ({self.count} 個鬧鐘)")
        _log(f"情境: {', '.join(self.scenarios)}")
        if self.hours:
            _log(f"測試時長: {self.hours:.1f} 小時  |  批次窗口: {batch_window}s")
        _log(f"離線模式: {'是 ✈️' if self.offline else '否'}")
        _log("=" * 65)

        if not check_device():
            _log("[FATAL] ADB 裝置未連接")
            sys.exit(1)
        _log(f"裝置: {get_device_info()}")

        # ── 安裝 APK ──────────────────────────────────────────────────────────
        if self.do_install:
            install_latest_apk()
        else:
            _log("⏩ 跳過 APK 安裝（--no-install）")

        # ── 事前權限檢查 ───────────────────────────────────────────────────────
        self.has_exact_alarm, perm_detail = check_exact_alarm_permission()
        status = "✔" if self.has_exact_alarm else "✘ 無精確權限"
        _log(f"SCHEDULE_EXACT_ALARM: {status}")
        _log(f"  {perm_detail}")
        if not self.has_exact_alarm:
            _log("  ⚠️  所有鬧鐘將使用 setAndAllowWhileIdle（精度較低），"
                 "結果僅供參考")

        # ── 重置裝置狀態（不重置網路，稍後在登入後再切換飛航）────────────────
        reset_all(restore_network=False)

        # ── 測試帳號登入（在切換飛航模式前）──────────────────────────────────
        if self.do_login:
            login_test_account()
            time.sleep(1)
        else:
            _log("⏩ 跳過登入（--no-login）")

        # ── 切換飛航模式 ───────────────────────────────────────────────────────
        if self.offline:
            set_airplane_mode(True)
            time.sleep(1)

        self.monitor.start()

        # ── 分批執行 ──────────────────────────────────────────────────────────
        batches = self._distribute()
        now = datetime.now()
        alarm_idx   = 0
        time_offset = BATCH_START_OFFSET  # 第一批從現在 + offset 秒開始觸發

        for scenario, batch_size in batches:
            _log(f"\n{'─'*55}")
            _log(f"情境: {ALL_SCENARIOS[scenario]}  ({batch_size} 個)")
            _log(f"{'─'*55}")

            # 計算觸發時間（在批次窗口內隨機分散）
            alarm_times = sorted(
                now + timedelta(seconds=time_offset + random.uniform(0, batch_window))
                for _ in range(batch_size)
            )
            last_time = alarm_times[-1]

            # 套用情境
            setup_scenario(scenario)

            # Doze：記錄 idle 狀態
            if scenario == "doze":
                self.doze_idle_at_batch_start = get_doze_state()
                _log(f"  Doze idle state: {self.doze_idle_at_batch_start}")

            # 新增鬧鐘
            _log(f"\n  新增 {batch_size} 個鬧鐘（窗口 "
                 f"{(now+timedelta(seconds=time_offset)).strftime('%H:%M:%S')}~"
                 f"{last_time.strftime('%H:%M:%S')}）")
            for t in alarm_times:
                alarm_idx += 1
                tc = add_alarm(
                    f"TEST_{alarm_idx:03d}", t, scenario,
                    self.monitor, self.keep_prob
                )
                # Doze 批次：把 idle 狀態附到每個 tc
                if scenario == "doze":
                    tc.doze_idle_state = self.doze_idle_at_batch_start
                self.test_cases.append(tc)

            # 等待響鈴
            wait_sec = (last_time + timedelta(seconds=BATCH_TAIL_WAIT)
                        - datetime.now()).total_seconds()
            if wait_sec > 0:
                _log(f"\n  等待批次響鈴（最多 {wait_sec:.0f}s = "
                     f"{wait_sec/60:.1f} 分鐘）...")
                self._wait_with_progress(wait_sec, ALL_SCENARIOS[scenario])

            # Doze：結束時再記錄一次 idle 狀態（看是否脫出）
            if scenario == "doze":
                idle_end = get_doze_state()
                _log(f"  Doze idle state（批次結束）: {idle_end}")

            teardown_scenario(scenario)
            time_offset += batch_window + BATCH_START_OFFSET

        time.sleep(2)
        self.monitor.stop()

        # ── 恢復網路 + 重置裝置 ───────────────────────────────────────────────
        reset_all(restore_network=True)

        # ── 清除測試鬧鐘 ──────────────────────────────────────────────────────
        if self.do_cleanup:
            cleanup_test_alarms(self.test_cases)
        else:
            _log("⏩ 跳過清除（--no-cleanup）")

        # ── 比對 + 報告 ───────────────────────────────────────────────────────
        self._match_events()
        self._compute_stats()
        self._print_report()
        self._save_output()

    # ── 時間分配 ──────────────────────────────────────────────────────────────

    def _distribute(self) -> List[Tuple[str, int]]:
        n = len(self.scenarios)
        base, extra = divmod(self.count, n)
        return [(s, base + (1 if i < extra else 0))
                for i, s in enumerate(self.scenarios)]

    # ── 進度等待 ──────────────────────────────────────────────────────────────

    @staticmethod
    def _wait_with_progress(seconds: float, desc: str):
        start = time.time()
        last_print = -11
        while True:
            elapsed = time.time() - start
            if elapsed >= seconds:
                break
            remaining = seconds - elapsed
            if elapsed - last_print >= 10:
                # 長時間測試顯示小時/分鐘格式
                if remaining >= 3600:
                    remain_str = f"{remaining/3600:.1f}h"
                elif remaining >= 60:
                    remain_str = f"{remaining/60:.1f}m"
                else:
                    remain_str = f"{remaining:.0f}s"
                _log(f"  [{desc}] 剩餘 {remain_str}")
                last_print = elapsed
            time.sleep(1)

    # ── 事件匹配 ──────────────────────────────────────────────────────────────

    def _match_events(self):
        """把 monitor 收到的 NexEvent 填回各 TestCase"""
        for tc in self.test_cases:
            tid = tc.test_id
            # SCHEDULED / CANCELLED 在新增時已填（透過 add_alarm），補充保險
            if tc.ev_scheduled is None:
                tc.ev_scheduled = self.monitor.latest(tid, "SCHEDULED")
            if not tc.should_ring and tc.ev_cancelled is None:
                ev = self.monitor.latest(tid, "CANCELLED")
                if ev is None and tc.alarm_db_id:
                    ev = self.monitor.latest_by_id(tc.alarm_db_id, "CANCELLED")
                tc.ev_cancelled = ev
            # RECEIVED / SERVICE_START
            tc.ev_received      = self.monitor.latest(tid, "RECEIVED")
            tc.ev_service_start = self.monitor.latest(tid, "SERVICE_START")
            # 如果 title 未能直接匹配，用 alarm_db_id 再找一次
            if tc.alarm_db_id and tc.ev_received is None:
                tc.ev_received = self.monitor.latest_by_id(
                    tc.alarm_db_id, "RECEIVED")
            if tc.alarm_db_id and tc.ev_service_start is None:
                tc.ev_service_start = self.monitor.latest_by_id(
                    tc.alarm_db_id, "SERVICE_START")

    # ── 統計彙整 ──────────────────────────────────────────────────────────────

    def _compute_stats(self):
        for tc in self.test_cases:
            s = self.stats[tc.scenario]
            label = tc.result_label
            if label == "perfect":       s.perfect += 1
            elif label == "mild_delay":  s.mild_delay += 1
            elif label == "severe_delay":s.severe_delay += 1
            elif label == "early":       s.early += 1
            elif label == "missed":      s.missed += 1
            elif label == "false_ring":  s.false_ring += 1

            if tc.delay_seconds is not None:
                s.delays.append(tc.delay_seconds)
            if tc.service_latency_ms is not None:
                s.svc_latencies.append(tc.service_latency_ms)

    # ── 報告輸出 ──────────────────────────────────────────────────────────────

    def _print_report(self):
        print()
        print("=" * 65)
        print("測試報告")
        print("=" * 65)

        if not self.has_exact_alarm:
            print("⚠️  SCHEDULE_EXACT_ALARM 未授權 → 所有鬧鐘使用 inexact API")
        print()

        # ── 各情境（排除 force_stop）────────────────────────────────────────
        rel_scenarios = [k for k in self.scenarios if k != "app_stopped"]
        tot = dict(perfect=0, mild=0, severe=0, early=0, missed=0, false_ring=0)
        all_delays: List[float] = []

        for key in rel_scenarios:
            s = self.stats[key]
            if s.total_expected == 0 and s.false_ring == 0:
                continue
            delayed = s.mild_delay + s.severe_delay + s.early

            # API 類型統計
            api_counts: Dict[str, int] = {}
            for tc in self.test_cases:
                if tc.scenario == key and tc.alarm_api:
                    api_counts[tc.alarm_api] = api_counts.get(tc.alarm_api, 0) + 1
            api_str = ", ".join(f"{k}×{v}" for k, v in api_counts.items()) or "—"

            # Doze idle state
            doze_str = ""
            if key == "doze":
                idle_states = {tc.doze_idle_state for tc in self.test_cases
                               if tc.scenario == "doze" and tc.doze_idle_state}
                doze_str = f"\n│  Doze idle state  : {', '.join(idle_states) or '—'}"

            print(f"┌─ {s.name}")
            print(f"│  Alarm API        : {api_str}{doze_str}")
            print(f"│  ✅ 完全準確 (≤{TOLERANCE_PERFECT:.0f}s)      : {s.perfect}")
            print(f"│  ⏱  輕微延遲 ({TOLERANCE_PERFECT:.0f}~{TOLERANCE_MILD:.0f}s)  : {s.mild_delay}")
            print(f"│  🔴 嚴重延遲 (>{TOLERANCE_MILD:.0f}s)      : {s.severe_delay}")
            print(f"│  ⏩ 提早觸發                  : {s.early}")
            print(f"│  ❌ 應響未響                  : {s.missed}")
            print(f"│  ❗ 誤響（不應響）            : {s.false_ring}")
            if s.delays:
                avg_d  = sum(s.delays) / len(s.delays)
                max_d  = max(s.delays)
                print(f"│  📊 平均延遲: {avg_d:+.2f}s  最大: {max_d:+.2f}s")
            if s.svc_latencies:
                avg_l = sum(s.svc_latencies) / len(s.svc_latencies)
                print(f"│  ⚡ 平均 svc_latency: {avg_l:.0f}ms")
            print()

            tot["perfect"]    += s.perfect
            tot["mild"]       += s.mild_delay
            tot["severe"]     += s.severe_delay
            tot["early"]      += s.early
            tot["missed"]     += s.missed
            tot["false_ring"] += s.false_ring
            all_delays.extend(s.delays)

        # ── force-stop 獨立區塊 ───────────────────────────────────────────────
        if "app_stopped" in self.scenarios:
            fs = self.stats["app_stopped"]
            print("─" * 65)
            print("【App 強制停止 — 獨立統計，不計入整體可靠性】")
            print(f"  完全準確: {fs.perfect}  |  "
                  f"延遲: {fs.mild_delay + fs.severe_delay}  |  "
                  f"應響未響: {fs.missed}  |  誤響: {fs.false_ring}")
            if fs.delays:
                avg_fs = sum(fs.delays) / len(fs.delays)
                print(f"  平均延遲: {avg_fs:+.2f}s")
            print()

        # ── 總計（不含 force-stop）───────────────────────────────────────────
        delayed_total = tot["mild"] + tot["severe"] + tot["early"]
        total_expected = (tot["perfect"] + delayed_total + tot["missed"])

        print("─" * 65)
        print("【總計摘要（排除 force-stop）】")
        print(f"  ✅ 完全無誤                : {tot['perfect']}")
        print(f"  ⚠️  延遲或提早              : {delayed_total}")
        print(f"      └ 輕微延遲              : {tot['mild']}")
        print(f"      └ 嚴重延遲              : {tot['severe']}")
        print(f"      └ 提早觸發              : {tot['early']}")
        print(f"  ❌ 該響但沒響鈴            : {tot['missed']}")
        print(f"  ❌ 不該響卻響鈴            : {tot['false_ring']}")

        if total_expected > 0:
            rate = (total_expected - tot["missed"]) / total_expected * 100
            print(f"\n  響鈴成功率: {rate:.1f}%  "
                  f"({total_expected - tot['missed']}/{total_expected})")
        if all_delays:
            avg = sum(all_delays) / len(all_delays)
            mx  = max(all_delays)
            mn  = min(all_delays)
            print(f"  整體平均延遲: {avg:+.2f}s  "
                  f"最大: {mx:+.2f}s  最小: {mn:+.2f}s")

        # ── 逐條明細 ──────────────────────────────────────────────────────────
        print()
        print("─" * 65)
        print("【逐條明細】")
        hdr = f"{'ID':>10}  {'預期時間':>8}  {'應響':>5}  {'實際':>8}  {'結果':<18}  API"
        print(hdr)
        print("─" * len(hdr))
        for tc in sorted(self.test_cases, key=lambda x: x.host_scheduled_time):
            exp_s = tc.host_scheduled_time.strftime("%H:%M:%S")
            should = "是" if tc.should_ring else "否(刪)"
            if tc.actual_ring_time:
                act_s = tc.actual_ring_time.strftime("%H:%M:%S")
            else:
                act_s = "—"

            lbl_map = {
                "perfect":        "✅ 準確",
                "mild_delay":     f"⏱ {tc.delay_seconds:+.1f}s",
                "severe_delay":   f"🔴 {tc.delay_seconds:+.1f}s",
                "early":          f"⏩ {tc.delay_seconds:+.1f}s",
                "missed":         "❌ 未響",
                "false_ring":     "❗ 誤響",
                "correct_silence": "✅ 正確未響",
            }
            result = lbl_map.get(tc.result_label, tc.result_label)
            api = tc.alarm_api or "?"
            dsym = ("✔" if tc.dumpsys_found
                    else "✘" if tc.dumpsys_found is False else "?")
            print(f"{tc.test_id:>10}  {exp_s:>8}  {should:>5}  "
                  f"{act_s:>8}  {result:<18}  {api}({dsym})")

    # ── 儲存 JSON + CSV ───────────────────────────────────────────────────────

    def _save_output(self):
        ts_str   = datetime.now().strftime("%Y%m%d_%H%M%S")
        json_f   = f"alarm_test_{ts_str}.json"
        csv_f    = f"alarm_test_{ts_str}.csv"

        # ── JSON ──────────────────────────────────────────────────────────────
        rel_keys = [k for k in self.scenarios if k != "app_stopped"]
        all_rel_delays = [d for k in rel_keys for d in self.stats[k].delays]
        tot_p   = sum(self.stats[k].perfect    for k in rel_keys)
        tot_d   = sum(self.stats[k].mild_delay + self.stats[k].severe_delay
                      + self.stats[k].early    for k in rel_keys)
        tot_m   = sum(self.stats[k].missed     for k in rel_keys)
        tot_fr  = sum(self.stats[k].false_ring for k in rel_keys)

        report = {
            "generated_at":    datetime.now().isoformat(),
            "device":          get_device_info(),
            "has_exact_alarm": self.has_exact_alarm,
            "total_alarms":    self.count,
            "keep_prob":       self.keep_prob,
            "scenarios":       self.scenarios,
            "offline_mode":    self.offline,
            "hours":           self.hours,
            "thresholds": {
                "perfect_s": TOLERANCE_PERFECT,
                "mild_s":    TOLERANCE_MILD,
            },
            "summary": {
                "perfect":          tot_p,
                "delayed_or_early": tot_d,
                "missed":           tot_m,
                "false_ring":       tot_fr,
                "avg_delay_s":      round(sum(all_rel_delays) / len(all_rel_delays), 3)
                                    if all_rel_delays else 0,
                "max_delay_s":      round(max(all_rel_delays), 3)
                                    if all_rel_delays else 0,
                "ring_success_rate": round(
                    (tot_p + tot_d) / (tot_p + tot_d + tot_m) * 100, 1
                ) if (tot_p + tot_d + tot_m) > 0 else None,
            },
            "app_stopped_independent": {
                "perfect":      self.stats.get("app_stopped",
                                ScenarioStats("","")).perfect,
                "mild_delay":   self.stats.get("app_stopped",
                                ScenarioStats("","")).mild_delay,
                "severe_delay": self.stats.get("app_stopped",
                                ScenarioStats("","")).severe_delay,
                "missed":       self.stats.get("app_stopped",
                                ScenarioStats("","")).missed,
                "false_ring":   self.stats.get("app_stopped",
                                ScenarioStats("","")).false_ring,
            } if "app_stopped" in self.scenarios else None,
            "by_scenario": {
                k: {
                    "name":          s.name,
                    "perfect":       s.perfect,
                    "mild_delay":    s.mild_delay,
                    "severe_delay":  s.severe_delay,
                    "early":         s.early,
                    "missed":        s.missed,
                    "false_ring":    s.false_ring,
                    "delays_s":      [round(d, 3) for d in s.delays],
                    "svc_latencies_ms": [round(l, 1) for l in s.svc_latencies],
                    "avg_svc_latency_ms": round(
                        sum(s.svc_latencies) / len(s.svc_latencies), 1
                    ) if s.svc_latencies else None,
                }
                for k, s in self.stats.items()
            },
            "details": self._detail_list(),
        }

        with open(json_f, "w", encoding="utf-8") as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        _log(f"JSON 已儲存: {json_f}")

        # ── CSV ───────────────────────────────────────────────────────────────
        fieldnames = [
            "test_id", "scenario", "scheduled_time", "should_ring",
            "alarm_db_id", "alarm_api", "dumpsys_found", "dumpsys_api",
            "actual_ring_time", "delay_seconds", "service_latency_ms",
            "result", "doze_idle_state",
            "ev_scheduled_ts", "ev_received_ts", "ev_service_ts",
            "has_exact_alarm",
        ]
        with open(csv_f, "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=fieldnames)
            w.writeheader()
            for row in self._detail_list():
                row["has_exact_alarm"] = self.has_exact_alarm
                w.writerow({k: row.get(k, "") for k in fieldnames})
        _log(f"CSV 已儲存: {csv_f}")

    def _detail_list(self) -> List[dict]:
        rows = []
        for tc in self.test_cases:
            rows.append({
                "test_id":            tc.test_id,
                "scenario":           tc.scenario,
                "scheduled_time":     tc.host_scheduled_time.isoformat(),
                "should_ring":        tc.should_ring,
                "alarm_db_id":        tc.alarm_db_id,
                "alarm_api":          tc.alarm_api or "",
                "dumpsys_found":      tc.dumpsys_found,
                "dumpsys_api":        tc.dumpsys_api or "",
                "actual_ring_time":   tc.actual_ring_time.isoformat()
                                      if tc.actual_ring_time else None,
                "delay_seconds":      round(tc.delay_seconds, 3)
                                      if tc.delay_seconds is not None else None,
                "service_latency_ms": round(tc.service_latency_ms, 1)
                                      if tc.service_latency_ms is not None else None,
                "result":             tc.result_label,
                "doze_idle_state":    tc.doze_idle_state or "",
                "ev_scheduled_ts":    tc.ev_scheduled.device_ts_ms
                                      if tc.ev_scheduled else None,
                "ev_received_ts":     tc.ev_received.device_ts_ms
                                      if tc.ev_received else None,
                "ev_service_ts":      tc.ev_service_start.device_ts_ms
                                      if tc.ev_service_start else None,
            })
        return rows


# ── 入口 ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="NexAlarm ADB 可靠性測試 v3.0",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
情境：
  normal        正常環境
  battery_saver 省電模式
  doze          強制 Doze 深度閒置（API 類型與 idle state 會記錄到結果）
  dnd           勿擾模式
  screen_off    螢幕關閉
  app_stopped   App 強制停止（結果獨立輸出，不計入可靠性統計）

範例：
  # 快速測試（預設）
  python3 alarm_reliability_test.py -n 30

  # 3 小時長時測試，200 個鬧鐘
  python3 alarm_reliability_test.py -n 200 --hours 3

  # 9 小時離線壓力測試，500 個鬧鐘，不含 Doze
  python3 alarm_reliability_test.py -n 500 --hours 9 --offline --no-doze

  # 指定情境，跳過安裝
  python3 alarm_reliability_test.py -n 20 --scenarios normal battery_saver dnd --no-install

  # 測試帳號（在腳本頂部修改 TEST_ACCOUNT_EMAIL / TEST_ACCOUNT_PASSWORD）
  python3 alarm_reliability_test.py -n 30 --no-login  # 跳過自動登入
""")
    parser.add_argument("-n", "--count", type=int, default=20,
                        help="鬧鐘總數（預設 20，無上限）")
    parser.add_argument("--hours", type=float, default=None,
                        help="測試總時長（小時）。設定後自動計算批次時間窗口。"
                             "例: --hours 9 可執行約 9 小時的長時測試")
    parser.add_argument("--offline", action="store_true", default=False,
                        help="啟用飛航模式進行離線測試（登入後切換，結束後恢復）")
    parser.add_argument("--no-doze", action="store_true",
                        help="跳過 Doze 測試")
    parser.add_argument("--scenarios", nargs="+",
                        choices=list(ALL_SCENARIOS.keys()),
                        default=None,
                        help="指定情境（預設全部）")
    parser.add_argument("--keep-prob", type=float, default=0.7,
                        help="鬧鐘保留機率 0.0~1.0（預設 0.7）")
    parser.add_argument("--no-dumpsys", action="store_true",
                        help="跳過 dumpsys alarm 驗證（加快新增速度）")
    parser.add_argument("--no-install", action="store_true",
                        help="跳過安裝最新 APK")
    parser.add_argument("--no-login", action="store_true",
                        help="跳過測試帳號自動登入")
    parser.add_argument("--no-cleanup", action="store_true",
                        help="測試結束後不刪除測試鬧鐘")
    args = parser.parse_args()

    count = max(1, args.count)  # 無上限，最少 1 個

    if args.scenarios:
        scenarios = args.scenarios
    else:
        scenarios = list(ALL_SCENARIOS.keys())
        if args.no_doze:
            scenarios = [s for s in scenarios if s != "doze"]

    # 把 --no-dumpsys 旗標傳入 add_alarm
    if args.no_dumpsys:
        _orig = add_alarm
        globals()["add_alarm"] = lambda *a, **kw: _orig(
            *a, **{**kw, "check_dumpsys": False})

    # 計算預估時間
    batch_window_preview = (
        max(60, int(args.hours * 3600 / len(scenarios)) - BATCH_START_OFFSET - BATCH_TAIL_WAIT)
        if args.hours else BATCH_WINDOW
    )
    if args.hours:
        eta_sec = int(args.hours * 3600)
    else:
        eta_sec = len(scenarios) * (BATCH_START_OFFSET + batch_window_preview + BATCH_TAIL_WAIT)

    eta_h = eta_sec // 3600
    eta_m = (eta_sec % 3600) // 60

    print()
    print("╔═══════════════════════════════════════════════════════════╗")
    print("║        NexAlarm ADB 可靠性測試  v3.0                     ║")
    print("╠═══════════════════════════════════════════════════════════╣")
    print(f"║  鬧鐘數量: {count:<5}  保留機率: {args.keep_prob:.0%}"
          f"  情境: {len(scenarios)} 個{' ':18}║")
    offline_tag = "✈️ 離線" if args.offline else "有網路"
    print(f"║  閾值: ≤{TOLERANCE_PERFECT:.0f}s=完全準確  "
          f"離線: {offline_tag}"
          f"  批次窗口: {batch_window_preview}s{' ':6}║")
    if eta_h > 0:
        print(f"║  預估時長: {eta_h}h {eta_m:02d}m{' ':45}║")
    else:
        print(f"║  預估時長: ~{eta_m}m{' ':49}║")
    print("╚═══════════════════════════════════════════════════════════╝")
    print()
    print("注意：")
    print("  1. 測試期間請勿操作裝置")
    print("  2. 確認 NexAlarm 已授予精確鬧鐘權限（設定 > 特殊應用程式存取）")
    print("  3. Doze 測試需實體裝置（模擬器可加 --no-doze）")
    print("  4. force-stop 場景測的是 AlarmManager 能否在 app 不在時喚醒裝置")
    print(f"  5. 測試帳號: {TEST_ACCOUNT_EMAIL}")
    print()

    AlarmReliabilityTester(
        count=count,
        scenarios=scenarios,
        keep_prob=args.keep_prob,
        hours=args.hours,
        offline=args.offline,
        do_install=not args.no_install,
        do_login=not args.no_login,
        do_cleanup=not args.no_cleanup,
    ).run()


if __name__ == "__main__":
    main()
