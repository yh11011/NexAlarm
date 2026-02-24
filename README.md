# Nex Alarm

A fully functional Android alarm application built with modern Android architecture and Material Design 3.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.1.0 |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM (ViewModel + Repository + Room) |
| Database | Room 2.6.1 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Package | `com.nexalarm.app` |

## Features

### ✅ Alarm Management
- Create, edit, delete alarms with 24-hour time picker
- Toggle enable/disable per alarm
- Recurring alarms with repeat-day selection (Mon–Sun)
- Configurable snooze delay (5–10 min) and max snooze count
- Volume control (0–100%) and vibrate-only mode
- One-time alarms auto-delete after ringing (configurable)

### ✅ Folder (Scenario) Management
- Create, edit, delete folders with color picker
- System folders ("Single Alarm", "Recurring Alarm") are protected
- Filter alarms by folder
- Free tier: up to 10 user folders (premium flag unlocks unlimited)

### ✅ Alarm Ringing
- Full-screen `AlarmRingingActivity` with wake lock and lock screen display
- Pulsing animation, Dismiss and Snooze buttons
- Notification with Dismiss/Snooze actions for in-app usage
- Uses `MediaPlayer` for sound and `Vibrator` for vibration
- Bypasses DND/silent mode (alarm audio stream)

### ✅ Meeting Mode Quick Settings Tile
- Toggle all today's alarms to vibrate-only
- Restores original settings when disabled
- Does not modify system silent/DND settings

### ✅ Deep Link / URI Integration
- Custom URI scheme: `nexalarm://`
- Actions: `add`, `delete`, `toggle_folder`
- Deduplication: matching time + title + folder + repeat overwrites existing alarm

### ✅ Boot Persistence
- All enabled alarms are rescheduled after device reboot

### ✅ Exact Alarm Scheduling
- Uses `AlarmManager.setExactAndAllowWhileIdle()`
- Handles `SCHEDULE_EXACT_ALARM` permission
- Correct next-trigger calculation with timezone and repeat rules

## Project Structure

```
app/src/main/java/com/nexalarm/app/
├── data/
│   ├── model/          AlarmEntity, FolderEntity
│   ├── database/       AlarmDao, FolderDao, NexAlarmDatabase
│   └── repository/     AlarmRepository, FolderRepository
├── ui/
│   ├── screens/        HomeScreen, AlarmEditScreen, FolderManageScreen
│   └── theme/          Color, Theme (Material 3 dark theme)
├── viewmodel/          AlarmViewModel, FolderViewModel
├── service/            AlarmService, MeetingModeTileService
├── receiver/           AlarmReceiver, BootReceiver
├── util/               AlarmScheduler, FeatureFlags
├── AlarmRingingActivity.kt
├── MainActivity.kt
└── NexAlarmApp.kt
```

## Build

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

## ADB Test Commands

### Add alarm via URI
```bash
adb shell am start -a android.intent.action.VIEW -d "nexalarm://add?time=0730&title=WakeUp&folder=Study&repeat=1,2,3,4,5&silent=true"
```

### Add simple alarm
```bash
adb shell am start -a android.intent.action.VIEW -d "nexalarm://add?time=0700&title=Morning"
```

### Delete alarm by ID
```bash
adb shell am start -a android.intent.action.VIEW -d "nexalarm://delete?id=1"
```

### Toggle folder
```bash
adb shell am start -a android.intent.action.VIEW -d "nexalarm://toggle_folder?name=Study"
```

## Permissions

| Permission | Purpose |
|-----------|---------|
| `SCHEDULE_EXACT_ALARM` | Exact alarm scheduling |
| `USE_EXACT_ALARM` | Exact alarm (API 33+) |
| `POST_NOTIFICATIONS` | Alarm notifications |
| `USE_FULL_SCREEN_INTENT` | Full-screen alarm display |
| `VIBRATE` | Alarm vibration |
| `WAKE_LOCK` | Keep screen on during alarm |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarms after reboot |
| `FOREGROUND_SERVICE` | Alarm ringing service |

## Premium Simulation

Toggle `FeatureFlags.isPremium = true` in code to unlock unlimited folders (free tier: 10).
