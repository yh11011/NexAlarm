# NexAlarm reliability testing

NexAlarm contains 14 instrumented scenario methods in `AlarmReliabilityTest.kt`. They require Android system behavior and are not part of the standard JVM-only CI run. Their existence does not mean they currently pass.

## Scenario inventory

| # | Scenario | Main observation |
|---:|---|---|
| 1 | Screen on | Baseline delivery |
| 2 | Screen off | Delivery while display sleeps |
| 3 | Device locked | Locked-screen/full-screen behavior |
| 4 | Synthetic Doze | Idle-mode behavior; physical device preferred |
| 5 | Rapid successive alarms | PendingIntent collisions/overlap |
| 6 | Process killed | AlarmManager delivery after process removal |
| 7 | kill-9 | Abnormal process termination |
| 8 | Alarm queue | Scheduled entry visible through system inspection |
| 9 | DND | Alarm audio behavior under Do Not Disturb |
| 10 | Battery Saver | Delivery timing under power saving |
| 11 | Vibration only | No unintended audio |
| 12 | Simultaneous alarms | Concurrent trigger behavior |
| 13 | Long standby | Delivery after extended idle |
| 14 | Silent mode | Alarm stream behavior under silent ringer mode |

## Observation levels and failure classes

- **Level 0:** receiver observed.
- **Level 1:** foreground service and sound/vibration path observed.
- **Level 2:** delay ≤ 3 seconds, service alive ≥ 5 seconds, no crash, usable volume, and notification/full-screen evidence.
- **F1:** no receiver trigger; **F2:** delay > 10 seconds; **F3:** trigger without expected sound; **F4:** immediate crash; **F5:** 3–10 second system delay.

Some scenarios change screen, DND, ringer, or power state. Review the test code and use a dedicated device; verify cleanup manually.

## Commands

Local checks that do not need a device:

```bash
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

All instrumented tests on a connected device/emulator:

```bash
adb devices
./gradlew connectedDebugAndroidTest
```

One scenario:

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nexalarm.app.test.AlarmReliabilityTest#test09_DoNotDisturbMode
```

Doze and OEM battery behavior should be validated on physical hardware. Record commit SHA, app version, Android build, device/OEM, exact-alarm access, battery optimization, scenario, raw timestamps, and pass/fail classification.

## Existing evidence

[Archived March 2026 results](docs/testing/results/README.md) include early triggers and false rings. They are useful regression evidence but are not a clean current-suite pass. Do not quote their `ring_success_rate` field as overall reliability.
