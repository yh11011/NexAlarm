# Archived reliability results

These CSV/JSON files are exploratory device-run evidence captured on 2026-03-31. They are preserved because failures are useful engineering evidence, but they are **not** a current release qualification report.

The files identify the device as reported by the harness and record exact-alarm availability, scheduled/observed timestamps, receiver-to-service latency, and classifications. Across the retained runs, summaries report no missed rings but do report early triggers and false rings; therefore the `ring_success_rate` field must not be interpreted as overall reliability or a pass rate.

Limitations:

- The exact app commit/version is not embedded strongly enough to reproduce every run.
- Runs cover subsets of scenarios rather than a clean, single execution of all 14 instrumented methods.
- Negative timing values and false rings indicate harness or product defects that require investigation.
- Results do not represent multiple Android versions or OEMs.

Future reports should include commit SHA, version name/code, device/OEM, Android build, exact-alarm permission, battery optimization state, scenario list, raw logs, and explicit pass/fail thresholds.
