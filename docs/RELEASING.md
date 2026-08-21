# Beta release process

NexAlarm uses pre-release identifiers such as `v1.1.0-beta.1`. A beta tag does not mean production-ready or Google Play approved.

## Required repository secrets

- `ANDROID_SIGNING_KEYSTORE_BASE64`: base64-encoded release keystore
- `ANDROID_SIGNING_KEY_ALIAS`
- `ANDROID_SIGNING_STORE_PASSWORD`
- `ANDROID_SIGNING_KEY_PASSWORD`

Never commit the keystore or print these values. Keep an offline backup and document key ownership outside the repository.

## Release checklist

1. Update `versionCode`, `versionName`, changelog, known issues, supported Android version, and verification scope in a reviewed PR.
2. Run lint, unit tests, debug build, and the relevant physical-device reliability matrix.
3. Tag the reviewed main commit with `vMAJOR.MINOR.PATCH-beta.N` and push the tag.
4. The release workflow rebuilds, signs, verifies with `apksigner`, creates a SHA-256 checksum, and opens a GitHub pre-release.
5. Install the attached APK on a clean device and an upgrade-path device before promoting the release from draft if manual approval is configured.

If signing secrets are absent or verification fails, the workflow must fail; an unsigned APK is only a CI artifact and must not be attached as a public download.
