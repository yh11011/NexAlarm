# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-03-27

### Added
- Full alarm management with 24-hour time picker
- Single and recurring alarms with flexible repeat rules (Mon-Sun selection)
- Folder-based alarm classification with color coding and emoji support
- System folders (Single Alarm, Recurring Alarm) with protection against deletion
- Configurable snooze delay (5-10 minutes) and maximum snooze count
- Volume control (0-100%) and vibrate-only mode
- Meeting Mode quick settings tile for toggling today's alarms to vibrate-only
- Deep Link integration with `nexalarm://` URI scheme supporting `add`, `delete`, `toggle_folder` actions
- Full-screen alarm ringing UI with pulsing animation and wake lock
- Notification with Dismiss/Snooze actions
- Boot persistence: all enabled alarms are rescheduled after device reboot
- Global crash handler that logs uncaught exceptions locally
- Dark mode / Light mode theme toggle
- Bilingual support (Traditional Chinese / English)
- 14 comprehensive automated reliability tests covering Doze mode, process termination, DND mode, etc.
- Premium tier support (unlimited folders; free tier limited to 10)

### Changed
- Timer wheel animation refactored to continuous arc with smooth tracking dot
- AlarmScheduler now uses `setAlarmClock()` for better Doze mode bypass (previously `setExactAndAllowWhileIdle()`)
- UI optimized for Material Design 3

### Fixed
- Timer wheel component stuttering and text occlusion issues
- APK build errors related to resource packaging
- Database migration issues from v1 to v3

### Security
- Proper handling of `SCHEDULE_EXACT_ALARM` permission (API 31-32)
- `USE_EXACT_ALARM` permission support (API 33+)
- No hardcoded secrets or credentials
- Input validation for alarm deep links to prevent injection attacks
- Secure alarm storage in Room database with type converters

### Notes
- Minimum SDK: API 26 (Android 8.0)
- Target SDK: API 35
- Kotlin: 2.1.0
- Jetpack Compose: 2025.01.00

---

## [Unreleased]

### Planned for v1.1.0
- Firebase Crashlytics integration for production monitoring
- CI/CD Pipeline via GitHub Actions
- ProGuard/R8 code obfuscation for release builds
- Database backup and export functionality
- Unit tests for ViewModels and Repository layer
- Memory leak detection with LeakCanary
- Improved Intent size handling for large alarm titles

### Planned for v1.2.0
- AlarmEditScreen refactoring to reduce complexity
- Single Activity refactoring to separate Deep Link handling
- Additional language support (Spanish, French, German)
- API documentation for Deep Link functionality
- User settings backup to cloud
