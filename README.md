# Tangle Crew Plugin

## What Changed From The Original

This repository is not the original Dink plugin. It is a fork tailored for the Tangle Crew workflow.

The main project-level changes in this fork are:

- Rebranding and packaging changes from the original Dink identity to `Tangle Crew Plugin`
- A clan-event-first workflow instead of a generic webhook-only workflow
- Support for a sister site that can coordinate clan events outside the game client
- Remote clan-event enable/disable commands through `::DinkEvent`
- Remote event config polling and migration settings for pulling active event data into the plugin
- Clan-event webhook routing that can temporarily override normal notifier webhook destinations
- Overlay support for showing the current clan-event state in-client

In short: the original notifier foundation is still here, but this fork adds a layer focused on organizing clan events, importing event settings, and routing notifications around those events.

Tangle Crew Plugin sends webhook messages for noteworthy Old School RuneScape events.
It supports Discord webhooks, custom webhook handlers, rich embeds, screenshots, metadata, and a clan-event workflow that can temporarily override your normal webhook routing.

This plugin is primarily built to work with a sister site built to make Clan Events easier

Examples of the metadata payloads sent by the plugin are available in [docs/json-examples.md](docs/json-examples.md).

If you find a bug or want a new notifier added, open an issue in this repository.

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

The plugin currently covers:

- Deaths, including PK deaths and special safe-death handling
- Collection log notifications
- Level-up notifications
- Loot notifications
- Slayer task notifications
- Quest completion notifications
- Clue scroll notifications
- Boss kill count notifications
- Combat achievement notifications
- Achievement diary notifications
- Pet notifications
- Quest speedrun notifications
- Barbarian Assault gambles
- Player kill notifications
- Group storage notifications
- Grand Exchange notifications
- Player trade notifications
- Leagues notifications
- Chat pattern notifications
- External plugin-triggered notifications
- Metadata login/logout summaries
- Clan event activation and remote clan event migrations

## Installation

Install the plugin through RuneLite the same way you would install any other hub plugin.
Once installed, open the plugin panel and configure your webhook URL(s).

## Basic Setup

To use the plugin, you need at least one webhook URL.

1. In Discord, open the server settings for the target server.
2. Open `Integrations`.
3. Create a webhook or open an existing one.
4. Copy the webhook URL.
5. Paste the URL into `Primary Webhook URLs` in the Tangle Crew Plugin settings.

If you want certain notifier types to use different destinations, place URLs in the relevant `Webhook Overrides` fields instead of only using the primary URL.

## Event Drop Detection Workflow

The plugin now uses one `Event Drop Detection` settings section for the full config.

That section contains the webhook URLs, event state, remote event polling, notifier toggles, and all related notifier options. While a clan event is active, the clan-event webhook overrides all other webhook URLs.

The overlay can display the current clan-event state on screen.

### Remote Event Commands

The remote-event workflow uses the `DinkEvent` chat command family.

- Enable a remote clan event:
  `::DinkEvent event enable <eventId>`
- Disable a remote clan event:
  `::DinkEvent event disable <eventId>`

Both commands:

- Fetch the current remote event config
- Validate the active event ID
- Fetch the matching migration payload
- Apply the imported settings

The disable command then forces the clan-event settings into a disabled state before saving:

- `clanEventEnabled = false`
- `clanEventWebhook = "No Event happening right now"`
- `clanEventEndTime = ""`
- `clanEventSecretCode = ""`
- `killCountEnabled = false`
- `minLootValue = 5000000`

If the plugin settings panel was already open, close and reopen it after applying a remote event so the UI reflects the latest values.

### Remote Event Settings

The default remote event config URL points at the repository-hosted JSON used to announce active clan events.

## Chat Commands

Commands are case-insensitive.

### `::DinkExport`

Export your current configuration to the clipboard.

- `::DinkExport`
  - Exports normal notifier settings and omits webhook URLs
- `::DinkExport all`
  - Exports the full config, including webhook URLs
- `::DinkExport webhooks`
  - Exports only webhook-related settings
- `::DinkExport <section>`
  - Exports the named config section, which is now `eventdropdetection`

### `::DinkImport`

Imports a JSON config from the clipboard.

This command merges some multi-line values, such as webhook URL lists and filtered names, rather than blindly replacing every existing value.

### `::DinkMigrate`

Imports settings from other webhook plugins on a best-effort basis.

- `::DinkMigrate all`
  - Migrates all supported plugins
- `::DinkMigrate <pluginName>`
  - Migrates a single supported plugin

Supported migrations include:

- BetterDiscordLootLogger
- DiscordCollectionLogger
- DiscordDeathNotifications
- DiscordLevelNotifications
- DiscordLootLogger
- DiscordRareDropNotifier
- GIMBankDiscord
- RaidShamer
- UniversalDiscordNotifications

### `::DinkHash`

Copies your Dink hash to the clipboard.

### `::DinkRegion`

Prints your current region ID to chat.

### `::DinkEvent`

Controls remote clan events.

- `::DinkEvent event enable <eventId>`
- `::DinkEvent event disable <eventId>`

## Notifiers

### Death

Sends a webhook message upon dying, including PK death support and configurable safe-death handling.

### Collection

Sends a webhook message when you obtain a collection log item.

### Level

Sends a webhook message when you level a skill, including virtual level and XP milestone support.

### Loot

Sends a webhook message for valuable loot and supports item value thresholds, item rarity, and screenshots.

### Slayer

Sends a webhook message when you complete a slayer task.

### Quests

Sends a webhook message when you complete a quest.

### Clue Scrolls

Sends a webhook message when you complete a clue scroll, with configurable tier and value thresholds.

### Kill Count

Sends a webhook message when you defeat a boss.

### Combat Achievements

Sends a webhook message when you complete a combat task.

### Achievement Diaries

Sends a webhook message when you complete an achievement diary.

### Pet

Sends a webhook message when you receive a pet.

### Speedrunning

Sends a webhook message when you complete a quest speedrun.

### BA Gambles

Sends a webhook message when you receive a high-level Barbarian Assault gamble.

### Player Kills

Sends a webhook message when you kill another player.

### Group Storage

Sends a webhook message for Group Ironman shared bank deposits and withdrawals.

### Grand Exchange

Sends a webhook message when you buy or sell items on the Grand Exchange.

### Trades

Sends a webhook message when you complete a player trade.

### Leagues

Sends a webhook message for Leagues region unlocks, relic unlocks, and task completions.

### Chat

Sends a webhook message when a chat message matches a user-defined pattern.

### External Plugins

Allows other hub plugins to request Dink notifications.

## Other Setup Notes

Some notifiers depend on RuneLite chat settings so the plugin can detect the underlying event.

- Collection notifier requires `Collection log - New addition notification`
- Pet notifier works best with `Untradeable loot notifications` enabled
- Kill Count notifier expects `Filter out boss kill-count with spam-filter` to remain disabled

## Advanced Features

- Multiple webhook URLs are supported; put one per line
- Each notifier can use its own webhook override
- Screenshots can be enabled per notifier
- Screenshots are compressed when needed to fit Discord limits
- Chat can be hidden from screenshots
- RSN filtering is supported
- Rich embeds are optional
- Rich embed footers can be customized
- The plugin retries webhook delivery when network errors occur
- Discord Forum Channel URLs are supported via `?forum` and `?thread_id=...`
- Metadata can be sent to custom handlers on login

## Message Placeholders

### All Messages

`%USERNAME%` is replaced with the username of the player.

### Death

`%VALUELOST%` is replaced with the value of the lost items.
`%PKER%` is replaced with the killer name for PvP deaths.

### Collection

`%ITEM%` is replaced with the collection log item.
`%COMPLETED%` is replaced with the number of completed entries.
`%TOTAL_POSSIBLE%` is replaced with the number of tracked entries.

### Level

`%SKILL%` is replaced with the skill name and achieved level.
`%TOTAL_LEVEL%` is replaced with total level.
`%TOTAL_XP%` is replaced with total experience.

### Loot

`%LOOT%` is replaced with the loot list.
`%TOTAL_VALUE%` is replaced with the total loot value.
`%SOURCE%` is replaced with the loot source.
`%COUNT%` is replaced with the source kill count or `unknown`.

### Slayer

`%TASK%` is replaced with the slayer task.
`%TASKCOUNT%` is replaced with the number of completed tasks.
`%POINTS%` is replaced with the points earned.

### Quests

`%QUEST%` is replaced with the completed quest name.

### Clue Scrolls

`%CLUE%` is replaced with the clue tier.
`%LOOT%` is replaced with the casket loot.
`%TOTAL_VALUE%` is replaced with the casket value.
`%COUNT%` is replaced with the number of completions for that clue tier.

### Kill Count

`%BOSS%` is replaced with the boss name.
`%COUNT%` is replaced with the kill or completion count.

### Combat Achievements

`%TIER%` is replaced with the achievement tier.
`%TASK%` is replaced with the task name.
`%POINTS%` is replaced with the points earned.
`%TOTAL_POINTS%` is replaced with the total points earned.
`%COMPLETED%` is replaced with the tier that was completed when rewards were unlocked.

### Achievement Diary

`%AREA%` is replaced with the diary area.
`%DIFFICULTY%` is replaced with the diary difficulty.
`%TOTAL%` is replaced with the total diaries completed.
`%TASKS_COMPLETE%` is replaced with the number of tasks completed.
`%TASKS_TOTAL%` is replaced with the total number of tasks possible.
`%AREA_TASKS_COMPLETE%` is replaced with the number of tasks completed in that area.
`%AREA_TASKS_TOTAL%` is replaced with the number of tasks possible in that area.

### Pet

`%GAME_MESSAGE%` is replaced with the game message.
`%PET%` is replaced with the pet name when available.

### Speedrunning

`%QUEST%` is replaced with the quest name.
`%TIME%` is replaced with the latest run time.
`%BEST%` is replaced with the personal best time.

### BA Gambles

`%COUNT%` is replaced with the gamble count.
`%LOOT%` is replaced with the gamble loot.

### Player Kills

`%TARGET%` is replaced with the victim's username.

### Group Storage

`%DEPOSITED%` is replaced with deposited items.
`%WITHDRAWN%` is replaced with withdrawn items.

### Grand Exchange

`%TYPE%` is replaced with the transaction type.
`%ITEM%` is replaced with the item name.
`%STATUS%` is replaced with the offer status.

### Trades

`%COUNTERPARTY%` is replaced with the other player's name.
`%GROSS_VALUE%` is replaced with the total value offered by both players.
`%NET_VALUE%` is replaced with the received value minus the given value.

### Leagues

Leagues notifications cover region unlocks, relic unlocks, mastery unlocks, and task completions.

### Chat

`%MESSAGE%` is replaced with the matched chat message.
`%SENDER%` is replaced with the message sender or message category.

### External Plugins

External plugin notifications are driven by the sender's payload and the notification template.

### Metadata

Login metadata includes a combined character summary spanning multiple notifier systems.

## Projects Using Dink

These projects integrate with Dink or consume Dink-style webhook payloads:

- Watchdog
- Leppunen's Dink Handler
- Zneix's Dink filter
- Swap's Webhook Handler
- Danbot
- stabiliserver
- RuneDiary
- OSRS Loot Tracker
- Cast OSRS

## Credits

This plugin uses code from Universal Discord Notifier.

Item rarity data is sourced from the OSRS Wiki and parsed by Flipping Utilities.
