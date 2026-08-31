# NexAlarm Release Build 構建指南

**文件版本**：1.0  
**最後更新**：2026年4月11日  
**目標版本**：v1.1.0 (versionCode: 2)

---

## 概述

本指南提供 NexAlarm 應用程式 Release APK/AAB 構建的完整流程，包括簽名配置、代碼混淆、資源優化、構建驗證和上架準備。

---

## 前置要求

### 必要工具
- ✅ Android Studio Arctic Fox 或更新版本
- ✅ JDK 17+
- ✅ Android SDK 35
- ✅ Gradle 8.7+
- ✅ Google Play 開發者帳號

### 必要檔案
- ✅ `google-services.json`（Firebase 配置）
- ✅ Keystore 檔案（`.jks` 或 `.keystore`）
- ✅ Keystore 密碼和別名資訊

### 檢查清單
- [ ] 所有測試通過（單元測試 + 儀器測試）
- [ ] Lint 檢查無錯誤
- [ ] 版本號已更新（versionCode 和 versionName）
- [ ] CHANGELOG.md 已更新
- [ ] 隱私政策已上線
- [ ] Firebase 配置正確

---

## 1. 簽名配置（Signing Configuration）

### 1.1 生成 Keystore

如果還沒有 Keystore，請執行以下步驟：

```bash
# 使用 keytool 生成 Keystore
keytool -genkey -v -keystore nexalarm-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias nexalarm-key

# 輸入資訊：
# - Keystore 密碼：[輸入強密碼並妥善保存]
# - Key 密碼：[輸入強密碼並妥善保存]
# - 姓名、組織等資訊
```

**⚠️ 重要提醒**：
- Keystore 檔案和密碼必須安全保存
- 丟失 Keystore 將無法更新應用程式
- 建議將 Keystore 備份到多個安全位置
- 不要將 Keystore 提交到版本控制系統

### 1.2 配置 Keystore

在專案根目錄創建 `keystore.properties` 檔案（**不要提交到 Git**）：

```properties
# Keystore 配置
# 注意：此檔案包含敏感資訊，不要提交到版本控制系統

STORE_FILE=nexalarm-release.jks
STORE_PASSWORD=your_keystore_password
KEY_ALIAS=nexalarm-key
KEY_PASSWORD=your_key_password
```

將 `nexalarm-release.jks` 檔案放在專案根目錄，並添加到 `.gitignore`：

```gitignore
# Keystore files
*.jks
*.keystore
keystore.properties
```

### 1.3 更新 build.gradle.kts

修改 `app/build.gradle.kts`，添加簽名配置：

```kotlin
// 在文件頂部添加
import java.util.Properties

// 讀取 keystore.properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    // ... 現有配置 ...
    
    signingConfigs {
        create("release") {
            if (keystoreProperties["STORE_FILE"] != null) {
                storeFile = file(keystoreProperties["STORE_FILE"] as String)
                storePassword = keystoreProperties["STORE_PASSWORD"] as String
                keyAlias = keystoreProperties["KEY_ALIAS"] as String
                keyPassword = keystoreProperties["KEY_PASSWORD"] as String
            }
        }
    }
    
    buildTypes {
        debug {
            // ... 現有 debug 配置 ...
        }
        release {
            // ... 現有 release 配置 ...
            
            // 添加簽名配置
            signingConfig = signingConfigs.getByName("release")
            
            // Crashlytics mapping file 上傳
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = true
            }
        }
    }
}
```

---

## 2. ProGuard 配置優化

### 2.1 當前 ProGuard 規則

現有的 `proguard-rules.pro` 已經包含基本的規則，但建議進行以下優化：

### 2.2 優化建議

更新 `app/proguard-rules.pro`：

```proguard
# ============ 新增優化規則 ============

# 啟用優化（移除不要的代碼和資源）
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# 移除日誌（Release build）
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# 保持 Gson 使用的類（如果使用 Gson）
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 保持 OkHttp 使用的類（如果使用網路庫）
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# 保持 Firebase 使用的類
-keepattributes *Annotation*
-keepclassmembers class * {
  @com.google.firebase.components.Component <init>();
}
-keep class com.google.firebase.** { *; }

# 保持 WorkManager 使用的類
-keep class androidx.work.** { *; }

# ============ 調試選項 ============

# 生成 mapping 檔案（用於解讀崩潰日誌）
-printmapping release/mapping.txt

# 生成詳細日誌（用於調試）
-printconfiguration release/configuration.txt
-printseeds release/seeds.txt
-printusage release/unused.txt
```

---

## 3. 構建步驟

### 3.1 清理專案

```bash
# 清理所有構建產物
./gradlew clean

# 清理並重新構建
./gradlew clean build
```

### 3.2 執行測試

```bash
# 執行單元測試
./gradlew test

# 執行 Lint 檢查
./gradlew lint

# 如果有連接裝置，執行儀器測試
./gradlew connectedAndroidTest
```

### 3.3 構建 Release APK

```bash
# 構建 Release APK
./gradlew assembleRelease

# APK 輸出位置：app/build/outputs/apk/release/app-release.apk
```

### 3.4 構建 Release AAB（推薦用於 Google Play）

```bash
# 構建 Release AAB
./gradlew bundleRelease

# AAB 輸出位置：app/build/outputs/bundle/release/app-release.aab
```

---

## 4. 構建驗證

### 4.1 檢查 APK/AAB 屬性

```bash
# 查看 APK 詳細資訊
aapt dump badging app/build/outputs/apk/release/app-release.apk

# 查看 AAB 詳細資訊
bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app-release.apks
```

### 4.2 驗證簽名

```bash
# 驗證 APK 簽名
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# 查看簽名資訊
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

### 4.3 檢查 APK 大小

```bash
# 查看 APK 大小
ls -lh app/build/outputs/apk/release/app-release.apk

# 分析 APK 結構
./gradlew analyzeReleaseBundle
```

### 4.4 安裝測試

```bash
# 安裝到連接的裝置
adb install app/build/outputs/apk/release/app-release.apk

# 啟動應用程式
adb shell am start -n com.nexalarm.app/.MainActivity

# 查看日誌
adb logcat | grep "com.nexalarm.app"
```

---

## 5. Firebase 配置驗證

### 5.1 檢查 google-services.json

確保 `app/google-services.json` 存在且配置正確：

```bash
# 檢查檔案是否存在
ls -la app/google-services.json

# 驗證 JSON 格式
cat app/google-services.json | jq .
```

### 5.2 驗證 Firebase 連接

```bash
# 構建並檢查 Firebase 配置
./gradlew assembleRelease

# 查看 Firebase 相關日誌
adb logcat | grep "Firebase"
```

### 5.3 上傳 Mapping File

Release 構建完成後，Firebase Crashlytics 會自動上傳 mapping file。可以檢查：

```bash
# 查看 mapping file 是否生成
ls -la app/build/outputs/mapping/release/mapping.txt

# 手動上傳（如果自動上傳失敗）
firebase crashlytics:symbols:upload --app=com.nexalarm.app \
  app/build/outputs/mapping/release/mapping.txt
```

---

## 6. 版本管理

### 6.1 更新版本號

在 `app/build.gradle.kts` 中更新：

```kotlin
defaultConfig {
    // Version management: Update both versionCode and versionName together
    versionCode = 3  // 每次發布必須遞增
    versionName = "1.2.0"  // 遵循 Semantic Versioning
}
```

### 6.2 更新 CHANGELOG.md

在 `CHANGELOG.md` 中添加版本更新說明：

```markdown
## [1.2.0] - 2026-04-XX

### Added
- 新功能描述
- 另一個新功能

### Changed
- 改進的功能描述
- 優化的性能

### Fixed
- 修復的錯誤描述
- 解決的問題

### Security
- 安全相關的更新
```

---

## 7. 上架準備

### 7.1 準備檔案清單

- [ ] Release APK 或 AAB 檔案
- [ ] 應用程式圖標（512x512）
- [ ] 功能圖形（1024x500）
- [ ] 螢幕截圖（至少 2 張）
- [ ] 應用程式描述（中文/英文）
- [ ] 隱私政策 URL
- [ ] 應用程式分類
- [ ] 內容評級

### 7.2 Google Play Console 上架步驟

1. **登入 Google Play Console**
   - 訪問：https://play.google.com/console
   - 選擇或創建應用程式

2. **填寫應用程式資訊**
   - 應用程式名稱
   - 簡短描述
   - 完整描述
   - 應用程式圖標
   - 功能圖形
   - 螢幕截圖

3. **設定商店列表**
   - 上傳所有必要素材
   - 填寫描述和關鍵字
   - 設定分類

4. **內容評級**
   - 填寫內容評級問卷
   - 確認評級結果

5. **定價與分發**
   - 設定價格（免費或付費）
   - 選擇分發國家/地區
   - 設定內容指南

6. **隱私政策**
   - 提供隱私政策 URL
   - 確保政策符合當地法律

7. **上傳 APK/AAB**
   - 上傳 Release APK 或 AAB
   - 等待 Google 自動測試完成
   - 確認無重大問題

8. **發布設定**
   - 選擇發布軌道（內部測試、封閉測試、開放測試、生產環境）
   - 設定發布時間
   - 提交審核

### 7.3 發布軌道建議

#### 階段 1：內部測試（1-2 天）
- 邀請內部成員測試
- 驗證基本功能
- 修復發現的問題

#### 階段 2：封閉測試（3-7 天）
- 邀請外部測試者（10-50 人）
- 收集使用者反饋
- 修復發現的問題

#### 階段 3：開放測試（選擇性）
- 向公開用戶開放測試
- 收集更多反饋
- 準備正式發布

#### 階段 4：生產環境
- 正式上架
- 開始推廣
- 監控使用者反饋

---

## 8. 常見問題排除

### 8.1 簽名失敗

**問題**：`Signing failed` 或 `Keystore file not found`

**解決方案**：
```bash
# 檢查 keystore.properties 是否存在
ls -la keystore.properties

# 檢查 Keystore 檔案是否存在
ls -la nexalarm-release.jks

# 驗證 Keystore 密碼
keytool -list -v -keystore nexalarm-release.jks -alias nexalarm-key
```

### 8.2 ProGuard 錯誤

**問題**：`ProGuard failed` 或 `Warning: can't find referenced class`

**解決方案**：
```bash
# 查看 ProGuard 日誌
cat app/build/outputs/mapping/release/dump.txt

# 添加忽略規則到 proguard-rules.pro
-dontwarn com.example.**
-keep class com.example.** { *; }

# 重新構建
./gradlew clean assembleRelease
```

### 8.3 Firebase 配置錯誤

**問題**：`Firebase initialization failed`

**解決方案**：
```bash
# 檢查 google-services.json 是否存在
ls -la app/google-services.json

# 重新下載 google-services.json
# 1. 訪問 Firebase Console
# 2. 選擇專案
# 3. 下載 google-services.json
# 4. 放置在 app/ 目錄

# 重新構建
./gradlew clean assembleRelease
```

### 8.4 APK 大小過大

**問題**：APK 大小超過 50MB

**解決方案**：
```kotlin
// 在 build.gradle.kts 中啟用資源壓縮
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true  // 啟用資源壓縮
        // ...
    }
}

// 使用 App Bundle（AAB）代替 APK
// Google Play 會自動為不同裝置生成優化的 APK
./gradlew bundleRelease
```

---

## 9. 構建後檢查清單

### 技術檢查
- [ ] Release APK/AAB 成功構建
- [ ] 簽名驗證通過
- [ ] APK 大小合理（< 50MB）
- [ ] 應用程式可以正常安裝和啟動
- [ ] 基本功能正常運作
- [ ] Firebase Crashlytics 正常連接
- [ ] ProGuard 混淆正常工作

### 內容檢查
- [ ] 應用程式名稱正確
- [ ] 版本號正確
- [ ] 圖標和素材正確
- [ ] 描述和關鍵字正確
- [ ] 隱私政策 URL 有效
- [ ] 分類和評級正確

### 上架檢查
- [ ] 所有必要素材已準備
- [ ] Google Play Console 資訊已填寫
- [ ] 內容評級已完成
- [ ] 隱私政策已上線
- [ ] 測試軌道已設定

---

## 10. 緊急回滾計劃

### 如果發現嚴重問題

1. **立即從 Google Play 下架**
   - 在 Google Play Console 中暫停應用程式
   - 通知已安裝的使用者（如適用）

2. **修復問題**
   - 快速修復發現的問題
   - 執行完整測試
   - 構建新的 Release 版本

3. **發布更新**
   - 增加 versionCode
   - 構建新的 APK/AAB
   - 提交審核並上架

4. **溝通**
   - 通知受影響的使用者
   - 提供更新說明
   - 提供補償（如適用）

---

## 11. 維護和更新

### 定期維護任務

#### 每週
- [ ] 檢查 Firebase Crashlytics 報告
- [ ] 查看使用者評論和評分
- [ ] 監控應用程式性能

#### 每月
- [ ] 檢查依賴庫更新
- [ ] 評估安全漏洞
- [ ] 規劃下一個版本

#### 每季
- [ ] 進行全面的代碼審查
- [ ] 更新測試覆蓋率
- [ ] 優化應用程式性能

---

## 12. 參考資源

### 官方文檔
- [Android App Bundles](https://developer.android.com/guide/app-bundle/)
- [Sign your app](https://developer.android.com/studio/publish/app-signing)
- [Shrink, obfuscate, and optimize your app](https://developer.android.com/studio/build/shrink-code)
- [Google Play Console](https://play.google.com/console)

### 工具
- [Bundletool](https://developer.android.com/studio/command-line/bundletool)
- [APK Analyzer](https://developer.android.com/studio/build/apk-analyzer)
- [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics)

---

## 結論

本指南提供了 NexAlarm Release Build 的完整流程。遵循這些步驟可以確保：

✅ **安全的簽名配置**：保護應用程式完整性  
✅ **優化的代碼混淆**：保護智慧財產權，減小 APK 大小  
✅ **完整的構建驗證**：確保 Release 品質  
✅ **順暢的上架流程**：減少審核被拒的風險  
✅ **可靠的維護計劃**：確保長期穩定運行  

**重要提醒**：
- 妥善保存 Keystore 和密碼
- 每次發布前執行完整測試
- 定期監控應用程式性能和使用者反饋
- 準備緊急回滾計劃

---

**文件版本**：1.0  
**最後更新**：2026年4月11日  
**負責人**：開發團隊  
**下次審查**：2026年5月11日
