# NexAlarm

<p align="center"><img src="website/logo-full.png" alt="NexAlarm" width="360"></p>

<p align="center"><strong>A reliability-focused Android alarm clock for recurring schedules and schedule groups.</strong></p>

<p align="center">
  <a href="https://github.com/yh11011/NexAlarm/actions/workflows/ci.yml"><img alt="Android CI" src="https://github.com/yh11011/NexAlarm/actions/workflows/ci.yml/badge.svg"></a>
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white">
  <a href="LICENSE"><img alt="MIT license" src="https://img.shields.io/badge/License-MIT-blue.svg"></a>
</p>

NexAlarm explores the parts of alarm-clock engineering that ordinary CRUD demos avoid: Android idle modes, process death, exact-alarm access, reboot recovery, recurring schedules, and user-visible fallbacks when the OS cannot guarantee exact delivery.

[Website](http://nex11.me/NexAlarm/) · [Report a reliability bug](https://github.com/yh11011/NexAlarm/issues/new?template=bug_report.yml) · [Build from source](#build-from-source)

> **Beta distribution status:** the existing `v1.0.0-beta` asset is a historical debug build and is not presented here as a verified release. A public download button will return after the signed-release workflow and device verification are completed. See [Release process](docs/RELEASING.md).

## Screenshots and demo

Real device screenshots are intentionally not substituted with mockups.

> **Manual follow-up:** add current, device-captured screenshots after the UI and privacy review. Track this in [the roadmap](docs/ROADMAP.md).

## Why NexAlarm?

Android alarms cross several system boundaries. NexAlarm's main scheduling path uses `AlarmManager.setAlarmClock()` when exact alarms are available. On Android 12+ without exact-alarm access, it deliberately falls back to `setAndAllowWhileIdle()` and therefore cannot promise exact delivery. Enabled alarms are stored in Room and rescheduled by `BootReceiver` after supported boot broadcasts.

The ringing path is `AlarmReceiver` → foreground `AlarmService` → full-screen ringing activity/notification. Schedule groups (called folders in parts of the code), recurring weekdays, snooze, vibration-only alarms, and a Quick Settings meeting-mode tile sit on top of that path.

## Reliability verification

The repository defines 14 instrumented scenarios, including screen-off, locked-device, synthetic Doze, process kill, kill-9, DND, Battery Saver, silent/vibration modes, concurrent alarms, alarm-queue inspection, and long idle. Their observation model is:

- **Level 0:** the broadcast receiver was observed.
- **Level 1:** the foreground service and sound/vibration path were observed.
- **Level 2:** timing, service lifetime, crash, volume, and notification/full-screen checks met the documented threshold.

Failures are classified as missed, late, silent, crashed, or system-delayed. These are **test definitions, not a claim that every scenario passes**. The retained March 2026 exploratory runs contain early triggers and false rings, so they are treated as diagnostic evidence. See [TESTING.md](TESTING.md) and [the archived results](docs/testing/results/README.md).

## Notable features

- One-time and weekday-recurring alarms with configurable snooze, volume, and vibration-only behavior.
- Schedule groups/folders with group enable/disable and protected system groups.
- Exact-alarm-aware scheduling with an explicit inexact fallback.
- Reboot rescheduling for enabled alarms.
- `nexalarm://` deep links for add, delete, and group-toggle automation.
- Meeting Mode Quick Settings tile that changes today's alarms without changing system DND.
- Timer, stopwatch, Traditional Chinese/English UI, light/dark themes, and selectable time zone.
- Optional account, cloud-sync, Crashlytics, and Google Play Billing code paths; service-side and store behavior are not covered by the local test suite.

## Architecture

NexAlarm is a Kotlin/Jetpack Compose app using MVVM-style ViewModels and repositories, Room (schema version 8), `AlarmManager`, broadcast receivers, a foreground service, and WorkManager. See [Architecture](docs/ARCHITECTURE.md) for component boundaries and the alarm lifecycle.

## AI-assisted development

NexAlarm uses AI-assisted tools to accelerate implementation, review, documentation, and debugging. AI output is not assumed correct: changes are checked against Android behavior, reviewed in source, and validated with lint, tests, CI, and physical-device testing when the behavior requires hardware. Known gaps and contradictory results stay documented.

**AI-assisted development, human-verified engineering.**

## Verification boundaries

Verified in this repository:

- CI is configured to require Android lint, JVM unit tests, and debug assembly.
- Fourteen reliability scenario methods exist in the instrumented test source.
- Historical device-run result files are preserved with their failures and limitations.
- The exact/inexact scheduling branches, boot receiver, foreground service, and Room migrations are present in source.

Not yet established:

- A clean rerun of all 14 scenarios on the current commit.
- A multi-device/OEM reliability matrix, including vendor battery-management behavior.
- Google Play review or a production-ready store release.
- A current signed beta APK with published checksum and documented signing verification.
- Complete end-to-end verification of the hosted account, sync, billing, and Crashlytics services.

## Build from source

Requirements: JDK 17 and an Android SDK capable of compiling API 35.

```bash
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Instrumented tests require an emulator or connected device:

```bash
./gradlew connectedDebugAndroidTest
```

Firebase-enabled builds use `app/google-services.json`. Treat local replacements as environment-specific configuration; do not commit private service credentials or signing material.

## Contributing and security

Small, evidence-backed pull requests are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting changes. Report vulnerabilities privately as described in [SECURITY.md](SECURITY.md). Roadmap items are tracked without inventing historical issues in [docs/ROADMAP.md](docs/ROADMAP.md).

## License

Licensed under the [MIT License](LICENSE).
