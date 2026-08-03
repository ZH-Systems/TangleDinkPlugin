# Tangle Dink Plugin 

Tangle Dink Plugin combines two workflows in one RuneLite sidebar entry:

1. Player-data synchronization against a remote API
2. Clan chat webhook forwarding to a configurable endpoint

It also includes a modular sidebar with collapsible feature folders so the plugin can grow without turning the main panel into a hard-coded list of special cases.

## Architecture

The plugin is split into:

- `api/` for HTTP clients and shared request execution
- `sync/` for manifest loading, snapshot capture, delta calculation, retry state, and submission
- `clanchat/` for clan message classification, filtering, queueing, and webhook delivery
- `collectionlog/` for the explicit collection-log capture flow and item mapping
- `features/` for sidebar feature modules and their panels
- `ui/` for the main sidebar panel and navigation

The main plugin stays small. It starts and stops the top-level services, wires RuneLite events to the relevant service methods, and adds the single sidebar button.

## Adding a Feature

Add a new feature module under `src/main/java/tccrewplugin/features/...`, implement `PluginFeature`, and register it in `FeatureManager`. The sidebar navigation is data-driven from the registry, so the navigation UI does not need a new branch for each feature.

## Clan Chat Webhooks

The plugin now includes a client-side clan chat webhook sender.

It listens to RuneLite clan chat events, sanitizes RuneLite markup, classifies clan system messages, detects account type badges, and sends a structured multipart request to your webhook endpoint.

### Configuration

These settings live in the `Clan Chat Webhook` config section:

- `Secret Key`
- `Endpoint URL`
- `Clan Name`
- `Send Normal Clan Chat`
- `Send System Broadcasts`
- `Send Unknown Broadcasts`
- `Send Login Guidance`
- `Debug Logging`
- `Request Timeout`
- `Include Client Metadata`

The endpoint should normally be the base server URL, for example:

`https://example.com`

The plugin appends:

`/webhook/{secretKey}`

### Request Format

The plugin sends:

`POST {endpoint}/webhook/{secretKey}`

with `multipart/form-data` containing one field:

- `data` - JSON-serialized clan message payload

### Payload Example

```json
{
  "author": "Example Player",
  "content": "Example Player received a new collection log item: Dragon defender",
  "accountType": "IRON",
  "systemMessageType": "COLLECTION_LOG",
  "timestamp": 1775160000,
  "clanTitle": null
}
```

### Message Coverage

The webhook sender supports:

- normal clan chat
- clan system messages
- drops
- raid drops
- pet drops
- collection-log entries
- personal bests
- quests
- PvP broadcasts
- membership events
- level-ups
- combat achievements
- clue drops
- achievement diaries
- unknown clan broadcasts

### Troubleshooting

- Make sure the secret key is set
- Make sure the endpoint URL is valid
- Prefer `https`
- If you are testing locally, `http://localhost` is allowed
- Check `Debug Logging` for safe diagnostics
- If a clan name filter is set, it must match the active clan exactly after whitespace normalization

### Security Notes

- The secret is never logged
- HTTP requests are asynchronous
- Requests have a timeout
- Retry handling is bounded
- Duplicate clan messages are suppressed for a short window
- The plugin does not send messages when the endpoint is malformed

### Local Testing

1. Start a local webhook receiver.
2. Set `Endpoint URL` to `http://localhost:<port>` or your HTTPS server base URL.
3. Set `Secret Key` to the token your receiver expects.
4. Enable the clan chat options you want to test.
5. Log into RuneLite and send a clan message or trigger a clan broadcast.

## Clog/PB Sync

The plugin also includes a client-side collection-log and personal-best sync path.

It reads locally available collection-log state, reads the same personal-best values RuneLite stores for `!pb`, and sends versioned JSON to a webhook you configure.

### Configuration

These settings live in the `Clog/PB Sync` config section:

- `Enable synchronization`
- `Webhook URL`
- `Webhook token`
- `Signing secret`
- `Enable collection log sync`
- `Enable PB sync`
- `Show queued messages`
- `Show success messages`
- `Show error messages`
- `Auto-upload collection log`
- `Auto-upload PB improvements`
- `Debug logging`

Both automatic upload toggles are off by default.

### Commands

- `!clogsync` uploads the cached collection log snapshot
- `!clogstatus` prints the current sync/cache state locally
- `!pball` uploads all locally known personal bests
- `!syncall` uploads a combined collection-log and PB snapshot

The native RuneLite `!pb <boss>` command remains unchanged.

### Webhook Contract

The sync endpoint must be HTTPS and should point at your webhook receiver base URL.

If you want to send directly to Discord, set `Webhook URL` to the Discord webhook URL itself and leave `Webhook token` blank.

The exact request and payload schema are documented in [docs/clog-pb-webhook.md](docs/clog-pb-webhook.md).

The plugin sends JSON with:

- `Authorization: Bearer <token>`
- `User-Agent`
- `X-Event-Id`
- `X-Event-Type`
- `X-Captured-At`
- `X-Timestamp`
- `X-Nonce`
- `X-Content-SHA256`
- optional `X-Signature` when a signing secret is configured

### Payload Example

```json
{
  "schemaVersion": 1,
  "eventType": "personal_bests.snapshot",
  "eventId": "40f83204-ef2e-4ed3-83fc-2427001aa2e1",
  "capturedAt": "2026-08-03T03:15:00Z",
  "command": "!pball",
  "player": {
    "displayName": "Example Player",
    "accountType": "STANDARD"
  },
  "client": {
    "runeliteVersion": "current-version",
    "pluginVersion": "current-plugin-version"
  },
  "summary": {
    "known": 18,
    "notLoaded": 7,
    "malformed": 0,
    "unsupported": 0
  },
  "personalBests": [
    {
      "activityKey": "zulrah",
      "activityName": "Zulrah",
      "variant": null,
      "teamSize": null,
      "durationMilliseconds": 58200,
      "source": "runelite-local-config"
    }
  ]
}
```

### Troubleshooting

- Make sure `Enable synchronization` is on.
- Make sure the webhook URL is valid and HTTPS.
- If you are using a receiver, make sure the webhook token is set.
- If you are using Discord directly, leave the webhook token blank and use the Discord webhook URL.
- Use `!clogstatus` to see whether RuneLite has a cached snapshot yet.
- If auto upload is enabled, the plugin still waits for relevant game-state changes instead of polling.

### Security Notes

- Secrets are not logged.
- Requests are asynchronous.
- Requests are bounded and retried only a small number of times.
- The plugin does not poll for collection-log or PB state.
- The plugin does not send any RuneLite state other than the sync payload.

### How to Test

Use these commands inside RuneLite:

- `!clogsync`
  - Uploads the cached collection log snapshot
- `!clogstatus`
  - Shows the current cache and capture state locally
- `!pball`
  - Uploads all locally known personal bests from RuneLite's `personalbest` profile data
- `!syncall`
  - Uploads both collection-log and PB data in one payload

Suggested test flow:

1. Open RuneLite and confirm the plugin is enabled.
2. Set `Enable synchronization` on.
3. Set `Webhook URL`, `Webhook token`, and, if needed, `Signing secret`.
4. Use `!clogstatus` first to confirm the plugin sees the current state.
5. Run `!pball` and confirm the webhook receives a JSON payload with `eventType = personal_bests.snapshot`.
6. Open the Collection Log in-game, then run `!clogsync` and confirm the webhook receives `eventType = collection_log.snapshot`.
7. Run `!syncall` and confirm the webhook receives `eventType = player_data.snapshot`.
8. If you want automatic uploads, turn on the `Auto-upload collection log` and `Auto-upload PB improvements` toggles, then trigger a relevant in-game state change.

What to verify:

- `!pb <boss>` still works normally through RuneLite
- `!clogstatus` reports the sync cache state without sending a request
- `!clogsync` sends only collection-log data
- `!pball` sends only local PB data
- `!syncall` sends both sections together
- the plugin does not upload anything when synchronization is disabled

## What It Does

Player sync uses:

- `GET {apiBaseUrl}/api/sync/manifest`
- `POST {apiBaseUrl}/api/sync/submit`

The API token is sent as a bearer token. Keep the default `apiBaseUrl` as a development placeholder until you point it at a real service.

Example manifest:

```json
{
  "version": 1,
  "varbits": [4101, 4102],
  "varps": [1, 2],
  "collectionLogItems": [11832, 11834]
}
```

Example submission:

```json
{
  "schemaVersion": 1,
  "username": "Player Name",
  "profile": "STANDARD",
  "pluginVersion": "1.0.0",
  "capturedAt": "2026-07-17T22:30:00Z",
  "data": {
    "varbits": {
      "4101": 1
    },
    "varps": {},
    "levels": {
      "Attack": 99
    },
    "collectionLog": {
      "mappingVersion": 1,
      "itemCount": 1572,
      "ownedCount": 400,
      "slots": "AAQIECBA..."
    }
  }
}
```

The plugin keeps the last successful snapshot per username and profile type, and only submits deltas.

## Configuring Clan Webhooks

Clan chat forwarding uses:

```http
POST {clanWebhookEndpoint}
Content-Type: application/json
Authorization: Bearer {clanWebhookSecret}
```

## Event Drop Detection Workflow

The plugin now uses one `Event Drop Detection` settings section for the full config.

That section contains the webhook URLs, event state, remote event polling, notifier toggles, and all related notifier options. While a clan event is active, the clan-event webhook overrides all other webhook URLs.

- Clan webhooks are enabled
- Endpoint and secret are present
- The player is logged in
- The message type is enabled
- The clan matches `requiredClanName` when that filter is set
- Guest broadcasts are allowed when the message is guest-related
- The message is not a duplicate
- The message is not plugin-generated loopback text

### Remote Event Commands

## Guest Broadcasts

Guest support is disabled by default. A guest broadcast is only treated as guest-related when the message itself is classified as a guest event. If `approvedGuestUsernames` is set, only normalized names in that allowlist are accepted.

## Webhook Payload

Webhook payloads are typed and versioned. A simplified example:

```json
{
  "schemaVersion": 1,
  "eventId": "04bfed56-85fd-4b03-bb63-d3e5129cd88c",
  "eventType": "CLAN_CHAT_MESSAGE",
  "occurredAt": "2026-07-17T22:45:00Z",
  "pluginVersion": "1.0.0",
  "player": {
    "username": "Local Player",
    "profile": "STANDARD",
    "world": 420
  },
  "clan": {
    "name": "Example Clan"
  },
  "message": {
    "type": "CHAT",
    "sender": "Clan Member",
    "senderRank": "GENERAL",
    "text": "Example clan message",
    "guest": false
  }
}
```

Test webhooks are marked as `TEST` and never impersonate a real clan member.

## Privacy and Security

Enabled clan messages are transmitted to the configured external endpoint and may become visible in downstream systems such as Discord. Player sync submissions are username-based and are not cryptographic proof of account ownership.

The default remote event config URL points at the repository-hosted JSON used to announce active clan events.

## Local Development

The test tree includes a lightweight development-only mock server with these endpoints:

- `GET /api/sync/manifest`
- `POST /api/sync/submit`
- `POST /api/clan/webhook`

It sanitizes captured JSON, validates a development token/secret, and can be configured to return retryable status codes such as `429` and `500`.

- `::DinkExport`
  - Exports normal notifier settings and omits webhook URLs
- `::DinkExport all`
  - Exports the full config, including webhook URLs
- `::DinkExport webhooks`
  - Exports only webhook-related settings
- `::DinkExport <section>`
  - Exports the named config section, which is now `eventdropdetection`

From the plugin root:

```bash
./gradlew run
```

Then follow the Jagex account development-client instructions:

https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts

## Notes on Attribution

This plugin reimplements the behavior of WikiSync and clan-chat-webhook rather than copying their source code wholesale. The reference projects were inspected for behavior and structure, but no substantial source code was imported into this repository.
