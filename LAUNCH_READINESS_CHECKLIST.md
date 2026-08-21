# Launch readiness

**Current status: not verified for a public signed beta or Google Play production release.**

This file is a gate, not a readiness claim. A release candidate is ready only when every checked item links to current evidence.

## Automated gate

- [ ] `lintDebug` passes on the tagged commit.
- [ ] `testDebugUnitTest` passes on the tagged commit.
- [ ] Release assembly completes.
- [ ] APK is signed with the protected release key.
- [ ] `apksigner verify --verbose --print-certs` passes.
- [ ] SHA-256 checksum is attached to the pre-release.

## Device gate

- [ ] All 14 scenarios rerun on the release candidate with raw results retained.
- [ ] No unexplained early trigger, false ring, missed alarm, silent alarm, or crash.
- [ ] Clean install and upgrade install verified.
- [ ] At least one real device verifies screen-off, lock, Doze, reboot, DND, Battery Saver, and exact-alarm fallback.
- [ ] OEM/Android test matrix and limitations are published.

## Product and security gate

- [ ] Permissions, privacy disclosures, hosted account/sync behavior, billing, and data deletion are reviewed end to end.
- [ ] No secret or signing material is tracked or printed by CI.
- [ ] Release notes list supported Android version, verification scope, install instructions, and known issues.
- [ ] Current screenshots are captured on a real device and reviewed for personal data.
- [ ] Google Play statements are added only after actual review/listing status can be linked.

See [docs/ROADMAP.md](docs/ROADMAP.md) for unresolved work.
