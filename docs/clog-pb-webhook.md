# Clog/PB Sync Webhook Contract

This plugin sends one versioned JSON envelope for collection-log and personal-best syncs.

## Transport Modes

### Receiver mode

If `Webhook URL` points to your own HTTPS receiver, the plugin sends:

- `POST <Webhook URL>`
- `Content-Type: application/json; charset=utf-8`
- `Authorization: Bearer <Webhook token>`
- `User-Agent: Tangle Crew Plugin/<version>`
- `X-Event-Id: <uuid>`
- `X-Event-Type: <eventType>`
- `X-Captured-At: <ISO-8601 timestamp>`
- `X-Timestamp: <unix millis>`
- `X-Nonce: <uuid>`
- `X-Content-SHA256: <sha256 hex of request body>`
- `X-Signature: <hmac-sha256 hex>` when `Signing secret` is set

The request body is the JSON payload described below.

### Discord mode

If `Webhook URL` is a Discord webhook URL:

- no bearer token is used
- no signing headers are used
- the plugin posts a Discord webhook payload

If the serialized JSON is short enough, it is sent inline in a code block. Otherwise the raw JSON is attached as `clog-pb-sync.json`.

## Root Payload Shape

Every sync request uses this envelope:

```json5
{
  "schemaVersion": 1,
  "eventType": "collection_log.snapshot | personal_bests.snapshot | player_data.snapshot",
  "eventId": "uuid",
  "capturedAt": "2026-08-03T18:06:47.508863300Z",
  "command": "!clogsync | !pball | !syncall",
  "player": {
    "displayName": "Zatch-Ary",
    "accountType": "STANDARD | IRONMAN | HARDCORE_IRONMAN | ULTIMATE_IRONMAN | ... "
  },
  "client": {
    "runeliteVersion": "1.12.33",
    "pluginVersion": "plugin version string or null"
  },
  "collectionLog": { },
  "personalBestSummary": { },
  "personalBests": [ ]
}
```

Only the sections relevant to the event are populated.

## Collection Log Payload

`eventType: "collection_log.snapshot"`

```json5
{
  "schemaVersion": 1,
  "eventType": "collection_log.snapshot",
  "eventId": "uuid",
  "capturedAt": "2026-08-03T18:06:47.508863300Z",
  "command": "!clogsync",
  "player": {
    "displayName": "Zatch-Ary",
    "accountType": "STANDARD"
  },
  "client": {
    "runeliteVersion": "1.12.33",
    "pluginVersion": "1.0.0"
  },
  "collectionLog": {
    "state": "COMPLETE | PARTIAL | NOT_LOADED",
    "capturedAt": "2026-08-03T18:06:47.506357100Z",
    "obtainedSlots": 312,
    "observedSlots": 1716,
    "knownTotalSlots": 1716,
    "observedCategoryCount": 5,
    "expectedCategoryCount": 5,
    "items": [
      {
        "itemId": 4151,
        "itemName": "Abyssal whip",
        "quantity": 1,
        "obtained": true,
        "category": "Bosses",
        "subcategory": "Abyssal Sire"
      }
    ]
  },
  "personalBestSummary": null,
  "personalBests": []
}
```

## Personal Best Payload

`eventType: "personal_bests.snapshot"`

```json5
{
  "schemaVersion": 1,
  "eventType": "personal_bests.snapshot",
  "eventId": "uuid",
  "capturedAt": "2026-08-03T18:06:47.508863300Z",
  "command": "!pball",
  "player": {
    "displayName": "Zatch-Ary",
    "accountType": "STANDARD"
  },
  "client": {
    "runeliteVersion": "1.12.33",
    "pluginVersion": "1.0.0"
  },
  "collectionLog": null,
  "personalBestSummary": {
    "known": 12,
    "notLoaded": 0,
    "malformed": 0,
    "unsupported": 0
  },
  "personalBests": [
    {
      "activityKey": "zulrah",
      "activityName": "Zulrah",
      "variant": null,
      "teamSize": null,
      "durationMilliseconds": 104500,
      "source": "runelite-local-config"
    }
  ]
}
```

## Combined Payload

`eventType: "player_data.snapshot"`

This contains both `collectionLog` and `personalBests` sections.

## Collection Log Field Notes

- `obtainedSlots` is the count of obtained collection-log slots.
- `observedSlots` is the number of unique items represented in the outgoing snapshot.
- `knownTotalSlots` is the full cache-derived collection-log index size.
- `observedCategoryCount` is the count of distinct top-level categories represented in the snapshot.
- `expectedCategoryCount` is the count of top-level categories in the cache index.
- `items[].obtained` is derived from the captured collection-log item IDs, not from quantity.

## Receiver Expectations

Your receiver should:

- accept JSON `POST`s over HTTPS
- validate `Authorization: Bearer <token>` when present
- reject non-2xx responses on invalid payloads
- treat `eventId` as the idempotency key
- route on `eventType`
- tolerate `collectionLog`, `personalBestSummary`, or `personalBests` being `null` depending on the event type

## Receiver Builder Prompt

Use this prompt to build a receiver for the plugin:

> Build an HTTPS webhook receiver for the Tangle Crew Plugin Clog/PB Sync feature.
>
> The receiver must accept `POST` requests containing JSON with this envelope:
>
> - `schemaVersion` integer
> - `eventType` string: `collection_log.snapshot`, `personal_bests.snapshot`, or `player_data.snapshot`
> - `eventId` UUID string used for idempotency
> - `capturedAt` ISO-8601 timestamp
> - `command` string, typically `!clogsync`, `!pball`, or `!syncall`
> - `player.displayName` string
> - `player.accountType` string
> - `client.runeliteVersion` string
> - `client.pluginVersion` string or null
> - `collectionLog` object or null
> - `personalBestSummary` object or null
> - `personalBests` array or null
>
> For collection-log events, `collectionLog.items` is an array of objects with:
>
> - `itemId` integer
> - `itemName` string
> - `quantity` integer
> - `obtained` boolean
> - `category` string
> - `subcategory` string or null
>
> For personal-best events, `personalBests` is an array of objects with:
>
> - `activityKey` string
> - `activityName` string
> - `variant` string or null
> - `teamSize` string or null
> - `durationMilliseconds` integer
> - `source` string
>
> The receiver should:
>
> - require `Authorization: Bearer <token>` when configured
> - optionally verify `X-Content-SHA256` and `X-Signature` if present
> - use `eventId` as an idempotency key so duplicates are ignored
> - route by `eventType`
> - return `2xx` only after the payload is accepted
> - store the exact JSON body for later forwarding or processing
> - never mutate the schema silently; if fields are missing, reject with a clear 4xx error
>
> Support Discord forwarding only if the receiver itself wants to fan out to Discord. The RuneLite plugin does not fan out to both destinations at once.
