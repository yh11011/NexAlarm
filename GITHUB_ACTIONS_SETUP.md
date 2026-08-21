# GitHub Actions setup

NexAlarm has three workflows:

- `ci.yml`: wrapper validation, `lintDebug`, `testDebugUnitTest`, `assembleDebug`, reports, and a short-lived debug artifact on pushes/PRs to `main`.
- `security-check.yml`: Gradle Wrapper validation and secret scanning on pushes, PRs, weekly schedule, and manual runs; dependency vulnerability review on PRs; Android lint.
- `release.yml`: beta tags only; requires signing secrets, then builds, signs, verifies, checksums, and publishes a GitHub pre-release.

The workflows do not run physical-device reliability scenarios and must not be described as proof of multi-device alarm reliability.

## Required release secrets

Configure these under **Settings → Secrets and variables → Actions**:

- `ANDROID_SIGNING_KEYSTORE_BASE64`
- `ANDROID_SIGNING_KEY_ALIAS`
- `ANDROID_SIGNING_STORE_PASSWORD`
- `ANDROID_SIGNING_KEY_PASSWORD`

Do not commit a keystore. See [docs/RELEASING.md](docs/RELEASING.md) for the beta checklist.

Recommended repository settings:

- Protect `main` and require the Android CI and security checks.
- Require pull-request review and block force pushes.
- Enable private vulnerability reporting and Dependabot alerts.
- Restrict release-environment access if signing secrets are moved to an environment.
