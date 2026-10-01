# Contributing to NexAlarm

Thanks for helping improve NexAlarm. Prefer a focused issue or pull request with evidence over a broad rewrite.

## Before opening a pull request

1. Describe the Android version, device/OEM, and exact-alarm/battery state for reliability changes.
2. Keep public claims tied to source, CI output, or an attached device result.
3. Never commit keystores, passwords, API tokens, private service credentials, or user alarm data.
4. Run:

   ```bash
   ./gradlew lintDebug testDebugUnitTest assembleDebug
   ```

5. For alarm delivery changes, run the relevant instrumented scenario on a device when possible and state what was not run.

Use imperative commit subjects and explain behavioral tradeoffs in the PR. AI-assisted contributions are welcome, but the contributor remains responsible for reviewing and verifying the result.
