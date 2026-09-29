# NexAlarm — 專案地圖

路徑相對於本檔目錄。先按功能讀第一站，只有涉及跨模組才擴大；程式與地圖不符時以程式為準並更新。

| 功能 | 第一站 | 需要時再看 | 驗證入口 |
| --- | --- | --- | --- |
| 導航、權限與 Deep Link | `app/src/main/java/com/nexalarm/app/AppNavigation.kt` | `app/src/main/java/com/nexalarm/app/MainActivity.kt` | Gradle 與 Deep Link 測試 |
| 鬧鐘編輯與狀態 | `app/src/main/java/com/nexalarm/app/viewmodel/AlarmViewModel.kt` | `app/src/main/java/com/nexalarm/app/data/repository/AlarmRepository.kt`、`app/src/main/java/com/nexalarm/app/ui/screens/AlarmEditScreen.kt` | 單元與裝置測試 |
| 排程、喚醒與響鈴 | `app/src/main/java/com/nexalarm/app/util/AlarmScheduler.kt` | `app/src/main/java/com/nexalarm/app/receiver/AlarmReceiver.kt`、`app/src/main/java/com/nexalarm/app/receiver/BootReceiver.kt`、`app/src/main/java/com/nexalarm/app/service/AlarmService.kt` | TESTING.md |
| Room 與遷移 | `app/src/main/java/com/nexalarm/app/data/database/NexAlarmDatabase.kt` | `app/src/main/java/com/nexalarm/app/data/model/AlarmEntity.kt`、`app/schemas` | migration 與保留資料測試 |
| 語言、主題與設定 | `app/src/main/java/com/nexalarm/app/util/AppSettingsProvider.kt` | `app/src/main/java/com/nexalarm/app/ui/theme/Strings.kt`、`app/src/main/java/com/nexalarm/app/data/SettingsManager.kt` | Compose 與背景同步 |
| 帳號、同步及付費 | `app/src/main/java/com/nexalarm/app/data/AuthRepository.kt` | `app/src/main/java/com/nexalarm/app/data/AlarmSyncRepository.kt`、`app/src/main/java/com/nexalarm/app/util/BillingManager.kt`、`app/src/main/java/com/nexalarm/app/util/FeatureFlags.kt` | mock API／Billing |
| 建置與發佈 | `app/build.gradle.kts` | `.github/workflows/ci.yml`、`.github/workflows/release.yml`、`TESTING.md` | ./gradlew test assembleDebug |
| 介紹網站 | `website` | `README.md` | 公開頁面與素材 |

## 部署與交付入口

Android 交付 APK 與測試結果；release workflow 見 .github/workflows/release.yml，不因文件提交建立 release tag。介紹頁目前由 /var/www/alarm.nex11.me 提供，網站修改需先核對 website/ 與該發布目錄的對照。

## 最近更新

每輪修改後核對並新增一筆，最多 20 筆，最新在前；純討論與唯讀查詢不新增。

- 2026-09-29：核對功能入口、驗證與部署方式，建立精簡導航並統一 Codex 工作規則。
