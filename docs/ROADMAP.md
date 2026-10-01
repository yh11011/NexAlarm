# Engineering roadmap

These are issue candidates derived from current repository evidence. They are not completed work and are not fake historical issues.

## Public-beta blockers

- Produce a signed beta with protected signing secrets, verified APK signature, SHA-256 checksum, install test, release notes, and known issues.
- Rerun all 14 reliability scenarios on the release candidate and publish an honest pass/fail report.
- Remove false-ring and early-trigger behavior seen in the archived March 2026 exploratory results.
- Confirm that the published Firebase Android API key is restricted to the intended package/signing certificates and that backend endpoints are intended for public clients; rotate anything that is not.

## Beta quality

- Build a device matrix across Android versions and multiple OEM battery-management implementations.
- Capture current physical-device screenshots and a short demo without personal data.
- Complete TalkBack, font scaling, contrast, touch-target, and reduced-motion accessibility checks.
- Verify hosted login, alarm sync, premium entitlement, Crashlytics, and privacy behavior end to end.
- Test release install/upgrade paths from the historical beta and current database schemas.

## Longer-term

- Add deterministic tests around trigger-time calculation and Room migrations.
- Automate a managed-emulator smoke suite while keeping Doze/OEM tests on physical devices.
- Document supported deep-link parameters and security constraints.
- Add a reproducible release provenance/attestation step after the signed beta path is stable.
