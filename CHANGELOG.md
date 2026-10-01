# Changelog

Notable user-facing and engineering changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Version labels are intended to use Semantic Versioning, but the project remains in beta and historical artifacts were not produced by the current signed-release process.

## [Unreleased]

### Added

- GitHub Actions workflows for lint, unit tests, debug builds, dependency review, secret scanning, Gradle Wrapper validation, and gated signed beta releases.
- Firebase Crashlytics integration and debug-only LeakCanary dependency.
- Room schema version 8 and migrations through `MIGRATION_7_8`.
- Account and cloud alarm-sync code paths.
- Open-source contribution, security, issue, pull-request, architecture, release, roadmap, testing-evidence, and engineering-journey documentation.

### Changed

- Public documentation now distinguishes implemented code, CI checks, historical device results, and unverified behavior.
- Beta releases require signing secrets, APK signature verification, and a SHA-256 checksum before GitHub Release publication.

### Removed

- Tracked debug APK and temporary SQLite databases from the distributable repository tree.

## [1.0.0-beta] — historical pre-release

### Added

- One-time and weekday-recurring alarms.
- Folder-based schedule organization.
- Configurable snooze, volume, and vibration-only settings.
- Meeting Mode Quick Settings tile.
- `nexalarm://` deep-link actions for add, delete, and folder toggle.
- Full-screen ringing flow, notification actions, and reboot rescheduling.
- Traditional Chinese/English UI and light/dark themes.
- Fourteen instrumented reliability scenario definitions.

### Changed

- The normal exact-alarm path uses `setAlarmClock()`; Android 12+ devices without exact-alarm access use the inexact `setAndAllowWhileIdle()` fallback.

### Notes

- The GitHub pre-release contains a debug APK. It predates the signed beta workflow and is not treated as a current verified distribution.
- No release date is asserted here because repository history and release metadata remain the source of truth.

[Unreleased]: https://github.com/yh11011/NexAlarm/compare/v1.0.0-beta...HEAD
[1.0.0-beta]: https://github.com/yh11011/NexAlarm/releases/tag/v1.0.0-beta
