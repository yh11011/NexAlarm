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

## In-app updates

The workflow publishes `update.json` alongside the signed APK and checksum. `scripts/release_metadata.py` extracts the actual package/version/minSdk and signing digest after signature verification. The existing auth service exposes `/api/v1/app/releases/latest?channel=beta`, accepting only complete metadata from this repository's signed beta release naming pattern. Historical Debug APKs are excluded. The app also checks the archive and its installed signing identity before invoking Android's installer. If no verified release exists, it shows that state and the official source without downloading a historical build.

Keep release version codes increasing and preserve the signing key. The local 1.1.0-beta.2 debug build uses a different certificate from the historical GitHub 1.0.0 APK; confirm the device's installed certificate before attempting an upgrade. Missing original signing material blocks compatible release delivery, not development of the update UI. Do not publish a new release until signing secrets and device validation are available.
