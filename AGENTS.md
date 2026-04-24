# Repository Guidelines

## Project Structure & Module Organization

NexAlarm is a Kotlin Android app using Jetpack Compose, Material 3, MVVM, Room, WorkManager, Firebase Crashlytics, and Google Play Billing. Main app code lives in `app/src/main/java/com/nexalarm/app/`: `data/` for Room models, DAOs, repositories, API/auth/sync code; `ui/screens/`, `ui/components/`, and `ui/theme/` for Compose UI; `viewmodel/` for presentation state; `service/`, `receiver/`, `worker/`, and `util/` for alarm runtime behavior. JVM tests are in `app/src/test/`; device/emulator tests are in `app/src/androidTest/`. Android resources are under `app/src/main/res/`, Room schemas under `app/schemas/`, release and setup docs under `docs/` and root Markdown files, and web assets under `website/`.

## Build, Test, and Development Commands

Use the Gradle wrapper from the repository root:

```bash
./gradlew assembleDebug              # build a debug APK
./gradlew installDebug               # install on a connected device/emulator
./gradlew testDebugUnitTest          # run local JVM unit tests
./gradlew connectedAndroidTest       # run instrumented tests on a device/emulator
./gradlew lintDebug                  # run Android lint for debug
./gradlew clean assembleDebug        # clean and rebuild
```

For direct alarm deep-link checks, use `adb shell am start -a android.intent.action.VIEW -d "nexalarm://add?time=0700&title=Morning"`.

## Coding Style & Naming Conventions

Use Kotlin with Java 17 targets. Follow existing Compose style: PascalCase for composables, screens, entities, ViewModels, and services; camelCase for functions and properties; `UPPER_SNAKE_CASE` only for constants when already used locally. Keep UI state in ViewModels or existing providers, repositories in `data/repository/`, and alarm scheduling/runtime logic out of composables. Preserve bilingual string resources in `res/values/strings.xml` and `res/values-zh-rTW/strings.xml`; avoid hardcoded user-facing text in Kotlin.

## Testing Guidelines

Place fast logic tests in `app/src/test/java/...` with names ending in `Test.kt`. Put Room, alarm reliability, and UI/device workflows in `app/src/androidTest/java/...`. Run `testDebugUnitTest` before small logic changes and `connectedAndroidTest` for alarm, database, receiver, service, or permission behavior. See `TESTING.md` for the 14 alarm reliability scenarios and report collection commands.

## Commit & Pull Request Guidelines

Recent history uses short imperative summaries such as `reduce lint warnings` and `polish launcher resources`, though it is not fully consistent. Prefer concise, lower-case, action-oriented commit subjects that describe the changed behavior. Pull requests should include a focused summary, test commands and results, linked issues when applicable, and screenshots or recordings for visible Compose UI changes. Note permission, manifest, billing, Firebase, or release-version changes explicitly.

## Agent-Specific Instructions

Respect existing uncommitted work; do not revert unrelated changes. `CLAUDE.md` contains detailed project guidance, including Chinese-language communication expectations for AI assistants. Keep `versionCode` and `versionName` in `app/build.gradle.kts` synchronized for release changes.
