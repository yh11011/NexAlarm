# NexAlarm 專案評估與 30 天整改清單

日期：2026-04-08

## 結論

總分：40/100。

這不是可投資的商用產品，最多算是功能已成形的 side project。更直白地說：它能 demo，但離可上架、可放量、可收費的產品還差一段實打實的工程與產品距離。

實際檢查結果：

- `./gradlew testDebugUnitTest`：通過
- `./gradlew lintDebug`：失敗
- Lint 結果：3 個 error、93 個 warning

這和專案內部文件宣稱「完全準備就緒」是矛盾的，見 [LAUNCH_READINESS_CHECKLIST.md](C:/Users/user/desktop/work/project/NexAlarm/LAUNCH_READINESS_CHECKLIST.md:4)。

## 一、技術品質：11/30

- 架構不算乾淨。表面上是 MVVM，但 `MainActivity` 直接建立 DB 與 repository 處理 deep link，見 [MainActivity.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/MainActivity.kt:103)。
- `AlarmViewModel` 直接握 DAO、scheduler、sync 邏輯，見 [AlarmViewModel.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/viewmodel/AlarmViewModel.kt:26)。
- `AppNavigation` 也塞了大量帳號與流程控制，見 [AppNavigation.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/AppNavigation.kt:68)。
- 這不是 Clean Architecture，沒有 DI、沒有 use case layer、邊界很鬆。

明顯安全問題：

- 登入 token 被直接放進 URL query string 打開網頁，見 [SettingsScreen.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/ui/screens/SettingsScreen.kt:482)。
- 這會進瀏覽器歷史、server access log、代理層與第三方觀測資料。正式產品不應這樣做。

明顯 Android 風險：

- `AlarmRingingActivity` 直接讀 wallpaper，Lint 已報權限問題，見 [AlarmRingingActivity.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/ui/screens/AlarmRingingActivity.kt:188)。
- `CrashHandler` 與 `CrashReportingManager` 在 `minSdk 26` 下直接用 `longVersionCode`，Lint 已報 `NewApi` error，見 [CrashHandler.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/util/CrashHandler.kt:88)、[CrashReportingManager.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/util/CrashReportingManager.kt:153)。

邏輯缺口：

- Manifest 註冊了 `QUICKBOOT_POWERON`，見 [AndroidManifest.xml](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/AndroidManifest.xml:133)。
- 但 `BootReceiver` 只處理 `BOOT_COMPLETED`，見 [BootReceiver.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/receiver/BootReceiver.kt:19)。

依賴問題：

- AGP、Firebase BOM、Room、Billing、WorkManager、Browser 等版本都落後現行 stable。
- `EncryptedSharedPreferences`、`MasterKeys` 所在的 `androidx.security.crypto` 路徑已被官方標記 deprecated。

測試覆蓋率：

- `app/src/main/java` 約 53 個檔、8363 行。
- JVM test 只有 2 個檔、81 行。
- 內容基本上只是 `FeatureFlags` 與 `RepeatDaysConverter`，見 [FeatureFlagsTest.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/test/java/com/nexalarm/app/FeatureFlagsTest.kt)、[RepeatDaysConverterTest.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/test/java/com/nexalarm/app/data/model/RepeatDaysConverterTest.kt)。
- `androidTest` 雖然不少，但沒有本次裝置驗證結果可佐證，而且 CI 沒有實跑 connected tests。

CI 問題：

- Lint 目前會直接紅燈，見 [ci.yml](C:/Users/user/desktop/work/project/NexAlarm/.github/workflows/ci.yml:37)。
- 單元測試被設成 `continue-on-error: true`，見 [ci.yml](C:/Users/user/desktop/work/project/NexAlarm/.github/workflows/ci.yml:69)。
- 這等於測試失敗也能往下走。

維護難度：中高。

## 二、產品完成度：15/25

- 核心功能基本齊。單次/重複鬧鐘、計時器、碼錶、資料夾分類、登入、付費開關、會議模式 tile 都有。
- 但完成度不到可商用。

主要缺口：

- 你賣「Cloud backup & restore」與 premium 功能，但實作上看到的是 alarm sync，不是一套完整的備份/還原產品流程，見 [FeatureFlags.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/util/FeatureFlags.kt:9)、[AccountScreen.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/ui/screens/AccountScreen.kt:315)。
- 自訂鈴聲看起來是半成品。資料模型有 `ringtoneUri`，見 [AlarmEntity.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/data/model/AlarmEntity.kt:22)，但 `AlarmService` 仍固定使用系統預設鈴聲，見 [AlarmService.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/service/AlarmService.kt:162)。
- 首次體驗過重。第一個進入點就可能先看到登入 onboarding，見 [AppNavigation.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/AppNavigation.kt:267)，然後再被要求通知、精確鬧鐘、電池白名單等權限，見 [MainActivity.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/MainActivity.kt:63)。
- UI/UX 可用，但不到上架精品水準。Lint 對 icon、monochrome icon、資源規範都在報警，代表 polish 不夠。

## 三、市場競爭力：8/25

- 競爭環境非常差。Google 自家 `Clock` 在 Google Play 是 1B+ downloads；`Alarm Clock Xtreme` 是 50M+；`Alarmy`、`Sleep as Android` 都是 10M+；`AMdroid` 也有 5M+。
- 這不是藍海，是高度飽和的工具類市場。

目前可見差異化：

- 資料夾/情境管理
- 會議模式 quick settings tile
- AI deep link 建立鬧鐘

判斷：

- 前兩個有一些價值，但不足以形成 moat。
- 第三個更像 gimmick，不是強需求，還伴隨 token 外洩風險。
- 目標用戶群不夠清楚，像是把學生、上班族、重度排程 users、AI 使用者全部混在一起。

市場規模：

- Android 裝置總量當然很大，但鬧鐘是預裝工具，Google Clock 先天佔入口。
- 第三方鬧鐘只有在強差異化的 niche 才有機會。

商業化可能性：低，但不是零。

## 四、商用投資報酬率：6/20

- 以商用最低標來看，還需要 320-480 小時純工程工時。
- 如果把 backend、QA、上架素材、隱私政策、商業化驗證一起算，現實總投入更接近 450-650 小時。

投資評分：3/10。

理由：

- 市場太擠
- 產品沒有明確 moat
- 商業化 wedge 太弱
- 工程與合規債務還沒清完

唯一讓它不是 1/10 的原因，是它至少不是假 demo，核心 alarm app 確實有做出來。

## 30 天整改清單

目標很明確：先把會擋上架、會出事故、會讓使用者不信任的問題清掉。

這 30 天不要分心做新功能。你現在最需要的是把專案從「能 demo」拉到「能上架且不太丟臉」。

## 第 1 階段：先救命，Day 1-10

1. 修掉所有會讓 `lint` 失敗的 error。
   - [AlarmRingingActivity.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/ui/screens/AlarmRingingActivity.kt:188)
   - [CrashHandler.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/util/CrashHandler.kt:88)
   - [CrashReportingManager.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/util/CrashReportingManager.kt:153)

2. 移除 token 放在 URL query string 的做法，改成短期授權碼或 server-side session exchange。
   - [SettingsScreen.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/ui/screens/SettingsScreen.kt:482)
   - 這一條是安全紅線，優先級最高。

3. 停止首頁直接彈一堆權限與電池白名單請求，改成「建立第一個鬧鐘時」再逐步引導。
   - [MainActivity.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/MainActivity.kt:63)
   - [MainActivity.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/MainActivity.kt:90)
   - `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 很容易踩 Play 政策，不要當預設流程。

4. 修正開機廣播行為一致性。
   - Manifest 宣告了 `QUICKBOOT_POWERON`，但 `BootReceiver` 沒處理。
   - [AndroidManifest.xml](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/AndroidManifest.xml:133)
   - [BootReceiver.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/receiver/BootReceiver.kt:19)

5. 把 CI 改成真的會擋錯誤。
   - 移除 `continue-on-error: true`
   - [ci.yml](C:/Users/user/desktop/work/project/NexAlarm/.github/workflows/ci.yml:69)

6. 刪掉或重寫「完全準備就緒」這種失真文件，避免你自己被假訊號騙。
   - [LAUNCH_READINESS_CHECKLIST.md](C:/Users/user/desktop/work/project/NexAlarm/LAUNCH_READINESS_CHECKLIST.md:4)

## 第 2 階段：能上架，Day 11-20

1. 先做一次依賴升級，但只升必要且低風險的。
   - 優先：Firebase plugin / BOM、Billing、Security Crypto、WorkManager、Browser。
   - 不要第一輪就大升 Compose、AGP、Room 到最新大版。

2. 補最小可接受測試集。
   - 現在 JVM test 幾乎等於沒有。
   - 至少補這幾類：
   - `AlarmScheduler` 時區/跨日/重複日計算
   - `AlarmViewModel` 新增/更新/刪除/貪睡/衝突檢查
   - `AlarmReceiver` dismiss/snooze 後狀態轉換
   - billing 與 premium gate 基本流程

3. 做 1 輪真機可靠性驗證，不要只看測試腳本存在。
   - 至少手動確認：
   - 螢幕鎖定時鬧鐘會響
   - 重開機後鬧鐘仍存在
   - 勿擾/省電模式下實際表現
   - 單次鬧鐘、重複鬧鐘、貪睡都正常

4. 修上架外觀與資源警告。
   - icon、monochrome、round icon、未使用資源都要整理。

5. 收斂 premium 文案，刪掉沒做好的承諾。
   - 現在寫了 `Cloud backup & restore`，但看到的是 sync，不是完整 restore 產品。
   - [FeatureFlags.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/util/FeatureFlags.kt:9)
   - [AccountScreen.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/ui/screens/AccountScreen.kt:315)

6. 補隱私政策與商店敘述。
   - 要明確說明：
   - 會收哪些帳號資料
   - 是否有 crash reporting
   - 是否有 analytics
   - 雲端同步範圍是什麼
   - AI 整合會導去哪裡

## 第 3 階段：上架後再談商業化，Day 21-30

1. 先凍結架構大改，只做低風險整理。
   - 先把最肥的三個檔拆薄：
   - [MainActivity.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/MainActivity.kt)
   - [AppNavigation.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/AppNavigation.kt)
   - [AlarmViewModel.kt](C:/Users/user/desktop/work/project/NexAlarm/app/src/main/java/com/nexalarm/app/viewmodel/AlarmViewModel.kt)

2. 定義真正的付費切入點，只留一個。
   - 不要再同時講 AI、資料夾、同步、客服。
   - 建議只選一條：
   - 重度鬧鐘管理
   - 情境/資料夾排程
   - 雲端同步

3. 補齊真正會影響轉換的功能缺口。
   - 優先順序：
   - 自訂鈴聲真的接上 `ringtoneUri`
   - 備份/還原真的可用
   - premium 狀態跨裝置一致
   - onboarding 不強迫登入

4. 加最小分析事件，不要先加一堆。
   - 只追這幾個：
   - 安裝後建立第一個鬧鐘
   - 第一次響鈴成功
   - 開啟 premium paywall
   - 購買成功
   - 7 日留存

5. 上架後一週只看兩件事。
   - crash rate
   - alarm reliability 投訴
   - 如果這兩個不穩，商業化全部延後。

## 交付標準

Day 10 前：

- `./gradlew testDebugUnitTest` 綠燈
- `./gradlew lintDebug` 綠燈
- 不再透過 URL 傳 token
- 不再在首次啟動就亂彈電池白名單
- CI 會真的擋錯

Day 20 前：

- 真機測過核心鬧鐘流程
- premium 文案與實作一致
- 上架素材與隱私政策齊備
- 可以送審，不會一眼看出是半成品

Day 30 前：

- 上架版本穩定
- 收集第一批真實使用資料
- 決定唯一商業化主軸

## 這 30 天不要做的事

- 不要重做整套 UI
- 不要先追最新所有依賴
- 不要先做更多 AI 整合
- 不要同時搞 website、backend、app 大重構
- 不要再寫「已完全就緒」這類文件

## 來源

- Google Play `Clock`: https://play.google.com/store/apps/details?hl=en&id=com.google.android.deskclock
- Google Play `Alarm Clock Xtreme & Timer`: https://play.google.com/store/apps/details?hl=en_US&id=com.alarmclock.xtreme.free
- Google Play `Alarmy - Alarm Clock & Sleep`: https://play.google.com/store/apps/details?hl=en-US&id=droom.sleepIfUCan
- Google Play `Sleep as Android: Smart alarm`: https://play.google.com/store/apps/details?id=com.urbandroid.sleep
- Google Play `AMdroid / Alarm Clock for Heavy Sleepers`: https://play.google.com/store/apps/details/?hl=en-US&id=com.amdroidalarmclock.amdroid
- Statcounter mobile OS market share worldwide, March 2026: https://gs.statcounter.com/os-market-share/mobile/worldwide-
- Android Developers `EncryptedSharedPreferences` deprecation: https://developer.android.com/reference/kotlin/androidx/security/crypto/EncryptedSharedPreferences
- Android Developers `MasterKeys` deprecation: https://developer.android.com/reference/kotlin/androidx/security/crypto/MasterKeys
