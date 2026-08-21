# Engineering journey

This log records engineering decisions and verification gaps rather than presenting a polished success story.

## Exact-alarm API selection

**Problem:** documentation described `setExactAndAllowWhileIdle()` although the implementation had moved. **Approach:** trace the scheduler's permission branches. **Verification:** source inspection shows `setAlarmClock()` on the exact path and `setAndAllowWhileIdle()` without access. **Lesson:** API names in marketing copy drift quickly; document the fallback and its weaker guarantee.

## Reliability evidence versus test inventory

**Problem:** the project claimed 14 automated reliability tests as though that meant 14 passing scenarios. **Approach:** compare test methods with retained CSV/JSON runs. **Verification:** all 14 methods exist, while historical results include early and false-ring classifications. **Lesson:** test existence, execution, and passing evidence are three different claims.

## Release readiness assumptions

**Problem:** an unsigned/debug artifact and an optimistic workflow made download availability look stronger than it was. **Approach:** require protected signing inputs, signature verification, checksums, and pre-release notes. **Verification:** the workflow can be statically reviewed now; an actual signed run remains a manual boundary. **Lesson:** artifact generation is not release readiness.

## Security workflow naming

**Problem:** an `echo` step was labeled vulnerability scanning and a file-type check was labeled cryptographic wrapper verification. **Approach:** replace them with dependency review, secret scanning, and Gradle's wrapper validation action. **Verification:** action execution still needs the pull request run. **Lesson:** security labels should describe controls that actually execute.

## AI-assisted maintenance

**Problem:** AI can rapidly repeat stale claims across README, website, and release notes. **Approach:** use AI for repository-wide discovery, then anchor edits to code, manifests, tests, history, and command output. **Verification:** unsupported claims were removed and remaining boundaries are explicit. **Lesson:** AI increases review throughput, not the truth value of its output.
