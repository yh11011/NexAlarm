# NexAlarm MCP

Premium users can connect Codex and ChatGPT to **https://login.nex11.me/mcp** using OAuth. The Android app must be upgraded to the version with Room schema 9, opened while signed in, and registered on each target phone.

## Connect

```sh
codex mcp add nexalarm --url https://login.nex11.me/mcp
codex mcp login nexalarm
```

For ChatGPT, use the supported custom MCP/Plugins flow with OAuth when Developer mode is available to the account/workspace. Follow the [connection guide](https://login.nex11.me/mcp-connect). This uses a private connection; no GPT Store or public-directory submission is required. The guide provides copyable name and URL fields. OpenAI currently documents no arbitrary-MCP URL that prefills the add-plugin form; do not invent an install URL or promise automatic filling. Never put passwords or access tokens into prompts or plugin configuration.

## Tools and delivery

- `list_alarms`: full `client_id`, alarm data and registered phones' current local clocks.
- `create_alarm`: hour/minute, optional `date` (`YYYY-MM-DD`) OR `repeat_days` (Mon=1…Sun=7), title and snooze/vibration settings. Include a unique `idempotency_key` and reuse it on retries.
- `update_alarm`: `client_id`, `changes` object and idempotency key. Unspecified fields survive; `date:null` clears a date.
- `delete_alarm`: client ID and idempotency key.
- `get_delivery_status`: operation ID.

Mutations snapshot currently registered phones and return their receipts after at most 20 seconds of waiting (plus a bounded push attempt). `complete:true` requires every phone to confirm exact scheduling or cancellation. `fallback` is inexact scheduling; `pending`, `failed` and `superseded` are not success. With no registered phones, the cloud alarm is saved but completion is false.

Times use **each phone's local timezone**. A dated alarm preserves its calendar date; an overdue date is rejected by the phone instead of rolling into tomorrow. DST gaps shift forward and overlaps choose the first occurrence. Relative dates must be resolved explicitly; ask the user if phones' local dates make the request ambiguous.

Push is a hint to fetch authenticated commands, never an alarm payload or account credential. Foreground and periodic synchronization recover missed delivery. Force-stopped/offline phones cannot be guaranteed immediate delivery. Device receipts attest submission to Android's scheduler, not future ringing.

## Backend deployment

The existing `nex11-auth.service` runs the Python source in the sibling `auth` repository. It owns MCP, OAuth, device registration and receipts. Install that repository's pinned-compatible dependencies. Configure `GOOGLE_APPLICATION_CREDENTIALS` to a private Firebase service-account file for the same project as Android, grant FCM sending permissions, and enable the FCM HTTP v1 API. Credentials and Android signing material stay outside Git. Without credentials, operations remain pending until the phone polls.

OAuth discovery uses the canonical login origin and MCP resource. New MCP grants use PKCE S256, one-hour access tokens and rotating 30-day refresh tokens; revocation and Premium checks apply on every request. Existing REST clients and legacy token grants remain compatible. Old sync clients do not receive dated alarms and cannot strip their dates.

## Validation

Backend: isolated `pytest tests` in the auth repository, with FCM mocked.

Android: `./gradlew test assembleDebug`; migration instrumentation: `./gradlew connectedAndroidTest` with an isolated test app on a connected device. Cover foreground/background, Doze, timezone changes, expired dates, fallback permissions, two phones, offline recovery, token rotation and logout.

User verification: connect both Codex and ChatGPT with a test Premium account, create a dated and a weekly alarm, confirm every phone's receipt, update, delete, and revoke access. No production alarm should be created as a health check.

## Current verification limits

No connected Android device, Tabbit browser launcher or Firebase server sending credentials were available during implementation. Real FCM delivery, Doze behavior, UI screenshots and account-level Codex/ChatGPT OAuth interaction require that environment. Protocol/OAuth behavior is tested in isolation; these limits must not be represented as completed end-to-end verification.
