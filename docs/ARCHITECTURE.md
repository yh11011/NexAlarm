# Architecture

NexAlarm is a single Android application written in Kotlin with a Jetpack Compose UI.

## Alarm lifecycle

```text
Compose screen / deep link
        ↓
ViewModel → Repository → Room
        ↓
AlarmScheduler → AlarmManager
        ↓
AlarmReceiver → foreground AlarmService
        ↓
notification / full-screen AlarmRingingActivity
```

`AlarmScheduler` selects `setAlarmClock()` when exact alarms are available. Android 12+ devices without exact-alarm access use `setAndAllowWhileIdle()`, which is an explicit reliability degradation. `BootReceiver` reloads enabled alarms from Room after supported boot broadcasts.

Room currently uses schema version 8 with explicit migrations from versions 1 through 8. UI state is exposed by ViewModels and repositories; periodic authenticated alarm sync uses WorkManager.

## Boundaries

- Device scheduling and ringing are local Android responsibilities.
- Account, sync, billing, Firebase, and website behavior depend on external services and require separate end-to-end verification.
- Instrumented reliability tests exercise system behavior but are not run by the standard hosted CI because they require a suitable Android device/emulator.
