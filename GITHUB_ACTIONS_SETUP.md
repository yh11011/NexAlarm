# GitHub Actions CI/CD 配置指南

本文檔說明 NexAlarm 專案的 GitHub Actions 自動化流程設定。

## 概述

本專案配置了三個 GitHub Actions workflows：

| Workflow | 觸發時機 | 功能 |
|----------|---------|------|
| **CI/CD** (ci.yml) | Push 和 PR | 自動構建、測試、代碼檢查 |
| **發佈** (release.yml) | 手動或標籤 | 生成 Release APK 並準備發佈 |
| **安全檢查** (security-check.yml) | Push、PR、定期 | 依賴檢查、機密掃描、漏洞檢測 |

## Workflows 詳解

### 1. CI/CD Pipeline (`.github/workflows/ci.yml`)

自動在每次 push 和 pull request 時執行。

#### 執行內容

1. **Lint 檢查** (`lint-and-analysis`)
   - 執行 Android Lint 檢查
   - 上傳 Lint 報告作為 Artifact
   - 檢查代碼風格和潛在問題

2. **構建與測試** (`build`)
   - 執行單元測試
   - 構建 Debug APK
   - 上傳測試報告和 APK

3. **Release 構建** (`release-build`)
   - 僅在 main 分支 push 時執行
   - 構建 Release APK（未簽名）
   - 上傳 Release APK

4. **完整性檢查** (`summary`)
   - 驗證所有構建步驟狀態
   - 失敗時拒絕合併（PR 狀態）

#### 使用方式

- **自動執行**：每次推送到 main/develop 或提交 PR 到 main/develop 時自動運行
- **查看結果**：GitHub Repository → Actions 標籤 → 選擇對應的 workflow 運行

#### 常見問題

**Q: 構建超時了怎麼辦？**

A: 如果 Gradle 構建超過 360 分鐘（GitHub Actions 預設超時），可以：
- 在 `.github/workflows/ci.yml` 中添加 `timeout-minutes: 720`
- 優化依賴或編譯時間

**Q: 如何跳過 CI 檢查？**

A: 在 commit 信息中添加 `[skip ci]` 或 `[ci skip]`：
```bash
git commit -m "文件更新 [skip ci]"
```

### 2. 發佈 Workflow (`.github/workflows/release.yml`)

生成 Release APK 並準備發佈到 Google Play。

#### 觸發方式

**方法 1：手動觸發**
1. 進入 GitHub Repository
2. 點擊 **Actions** 標籤
3. 選擇 **「發佈 Release」** workflow
4. 點擊 **Run workflow**
5. 輸入版本號（例：`1.0.1`）
6. 確認執行

**方法 2：推送標籤**
```bash
# 在本地建立標籤並推送
git tag v1.0.1
git push origin v1.0.1
```

#### 執行內容

1. 提取版本號（從手動輸入或標籤名）
2. 自動更新 `build.gradle.kts` 中的版本號
3. 構建 Release APK（未簽名）
4. 驗證 APK 有效性
5. 上傳 APK 和發佈說明
6. 創建 GitHub Release（如果是推送標籤）

#### 簽署與發佈

Release APK **未簽名**，需要手動簽名才能發佈到 Google Play：

```bash
# 下載 Release APK
# 使用簽署金鑰簽名
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore release.keystore \
  app-release-unsigned.apk nexalarm

# 驗證簽名
jarsigner -verify -verbose -certs app-release-unsigned.apk

# 在 Google Play Console 上傳簽名後的 APK
```

### 3. 安全檢查 Workflow (`.github/workflows/security-check.yml`)

定期檢查依賴安全性和代碼安全問題。

#### 觸發時機

- 修改 `build.gradle.kts` 或 `gradle-wrapper.properties` 時
- 每週一上午 9 點自動執行（UTC）
- PR 中修改依賴時

#### 檢查項目

1. **依賴檢查**
   - 檢測過時的依賴
   - 驗證 Gradle Wrapper 完整性

2. **機密掃描**
   - GitGuardian 掃描（需要設定 API key）
   - 本地硬編碼機密檢測

3. **Lint 安全掃描**
   - Android Lint 安全警告
   - 代碼風格檢查

## 配置與自定義

### 修改觸發條件

編輯 `on` 部分以改變 workflow 觸發時機：

```yaml
on:
  push:
    branches: [ main, develop ]           # 推送到這些分支時執行
    paths: [ 'app/**', 'build.gradle.kts' ] # 只在特定檔案變更時執行
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 9 * * 1'                  # 每週一 9:00 UTC
```

### 配置執行環境

修改 `runs-on` 以使用不同的運行器：

```yaml
runs-on: ubuntu-latest  # 預設：Ubuntu
runs-on: macos-latest   # macOS
runs-on: windows-latest # Windows
```

### 設定 Secrets（敏感信息）

1. 進入 Repository **Settings** → **Secrets and variables** → **Actions**
2. 點擊 **New repository secret**
3. 添加敏感信息（如 API keys）
4. 在 workflow 中使用：`${{ secrets.SECRET_NAME }}`

#### 常用 Secrets

```yaml
secrets.GITHUB_TOKEN          # GitHub 自動提供的 token
secrets.GITGUARDIAN_API_KEY   # GitGuardian 漏洞掃描 API key
secrets.SIGNING_KEYSTORE      # APK 簽署金鑰（Base64 編碼）
secrets.SIGNING_KEY_ALIAS     # 簽署金鑰別名
secrets.SIGNING_KEY_PASSWORD  # 簽署金鑰密碼
```

### 上傳簽署金鑰（用於自動簽名）

如果要在 GitHub Actions 中自動簽署 APK：

```bash
# 1. 將簽署金鑰編碼為 Base64
base64 release.keystore | xclip

# 2. 在 GitHub Settings 中添加 secret：
# Name: SIGNING_KEYSTORE
# Value: 複製 Base64 字符串

# 3. 在 build.gradle.kts 中添加簽署配置
signingConfigs {
    release {
        storeFile = file(System.getenv("SIGNING_KEYSTORE_PATH") ?: "release.keystore")
        storePassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""
        keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: ""
        keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""
    }
}
```

## GitHub Actions 限制與配額

免費帳戶每月享受：
- **2000 分鐘** GitHub Actions 執行時間（Ubuntu）
- **無限制** 存儲 Artifacts（保留 90 天）
- **5 個並發任務**

組織和商業帳戶：
- **Enterprise Cloud** 提供更高配額

## 監控與調試

### 查看 Workflow 執行日誌

1. Repository → **Actions** 標籤
2. 選擇對應的 workflow 運行
3. 點擊任務查看詳細日誌
4. 搜索錯誤信息或 `Error:` 關鍵字

### 常見錯誤與解決方案

| 錯誤 | 原因 | 解決 |
|-----|------|------|
| `Gradle build failed` | 編譯錯誤 | 檢查本地編譯，修復代碼問題 |
| `Lint failed` | 代碼檢查不通過 | 修復 Lint 警告 |
| `No matching APK found` | APK 構建失敗 | 檢查 build.gradle.kts 配置 |
| `Timeout` | 構建超時 | 優化依賴或增加超時時間 |

### 本地測試 Workflow

使用 `act` 工具在本地測試 workflow：

```bash
# 安裝 act
brew install act  # macOS
# 或其他平台的安裝方式

# 列出所有 workflows
act --list

# 本地執行特定 workflow
act -j build

# 本地執行並顯示詳細日誌
act -j build --verbose
```

## 最佳實踐

### 1. 使用保護分支

在 Repository Settings → **Branches** 中配置：
- ✅ 要求 CI 檢查通過才能合併 PR
- ✅ 要求至少 1 個代碼審查
- ✅ 禁止強制推送

### 2. 定期更新依賴

```bash
# 定期執行依賴更新檢查
./gradlew dependencyUpdates

# 或使用 Renovate 自動化 PR
```

### 3. 監控構建失敗

- 配置 Email 通知（GitHub Settings）
- 使用 Slack 集成接收通知
- 定期檢查 Actions 執行歷史

### 4. 安全實踐

- ❌ 不要將簽署金鑰提交到版本控制
- ✅ 使用 GitHub Secrets 存儲敏感信息
- ✅ 定期掃描依賴漏洞
- ✅ 審核 GitHub Actions 外掛的安全性

## 後續改進建議

1. **集成代碼覆蓋率檢查** (Codecov)
   ```yaml
   - uses: codecov/codecov-action@v3
     with:
       files: ./app/build/reports/coverage.xml
   ```

2. **自動發佈到 Google Play**
   ```yaml
   - uses: r0adkll/upload-google-play@v1
     with:
       serviceAccountJsonPlainText: ${{ secrets.PLAY_STORE_JSON }}
       packageName: com.nexalarm.app
       releaseFiles: 'app/build/outputs/apk/release/**/*.apk'
       track: internal  # internal -> alpha -> beta -> production
   ```

3. **自動版本 bump**
   ```yaml
   - uses: PaulHatch/semantic-version-action@v5
     with:
       tag_prefix: "v"
       major_pattern: "BREAKING CHANGE:"
   ```

## 相關資源

- [GitHub Actions 文檔](https://docs.github.com/en/actions)
- [Gradle 最佳實踐](https://gradle.org/guides/performance/)
- [Android 構建系統](https://developer.android.com/build)
- [本專案 Workflows](../.github/workflows/)

---

**最後更新：** 2026-03-27 | **貢獻者：** AI 助手
