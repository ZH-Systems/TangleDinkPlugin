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

## Configuring Player Sync

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

Bearer token mode is the default. A secret-header compatibility mode is also supported, using `X-Clan-Webhook-Secret`.

The endpoint and secret are configurable at runtime and do not require a RuneLite restart to take effect. Disabling clan webhooks stops delivery immediately.

## Clan Filtering

The clan service only forwards a message when all of the following are true:

- Clan webhooks are enabled
- Endpoint and secret are present
- The player is logged in
- The message type is enabled
- The clan matches `requiredClanName` when that filter is set
- Guest broadcasts are allowed when the message is guest-related
- The message is not a duplicate
- The message is not plugin-generated loopback text

Matching is case-insensitive and whitespace-normalized.

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

The plugin does not log API tokens or webhook secrets. Collection-log capture is explicit user action, not continuous scraping.

## Collection Log Consent

Collection-log sync only begins when you trigger the capture action in the sidebar. The plugin does not continuously scrape the collection log in the background.

## Local Development

The test tree includes a lightweight development-only mock server with these endpoints:

- `GET /api/sync/manifest`
- `POST /api/sync/submit`
- `POST /api/clan/webhook`

It sanitizes captured JSON, validates a development token/secret, and can be configured to return retryable status codes such as `429` and `500`.

## Run It Locally

From the plugin root:

```bash
./gradlew run
```

Then follow the Jagex account development-client instructions:

https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts

## Notes on Attribution

This plugin reimplements the behavior of WikiSync and clan-chat-webhook rather than copying their source code wholesale. The reference projects were inspected for behavior and structure, but no substantial source code was imported into this repository.
