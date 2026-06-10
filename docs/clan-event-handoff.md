# Clan Event Feature Handoff

This repository is a fork/customization of the Dink RuneLite plugin. The requested clan event work has been started and the central implementation is now in place. This document captures the architecture, implementation status, and remaining verification needs for another agent to take over.

## Requested Changes

1. Add a checkbox/config toggle for whether a clan event is happening.
2. When the event is enabled, prompt/configure a new webhook URL that overrides all existing webhook routing until the event completes.
3. Allow creation of a secret code entered by the user in the Dink/RuneLite config panel.
4. Show the secret code plus the date on screen at all times and in all screenshots.
5. Once the event timer ends, webhook routing reverts to the previous behavior.

## Current Repo State

- Main plugin class: `src/main/java/TcCrewPlugin/tccrewplugin.java`
- Config interface: `src/main/java/TcCrewPlugin/DinkPluginConfig.java`
- Runtime config/import manager: `src/main/java/TcCrewPlugin/SettingsManager.java`
- Webhook routing for most notifiers: `src/main/java/TcCrewPlugin/notifiers/BaseNotifier.java`
- Webhook HTTP + screenshot processing: `src/main/java/TcCrewPlugin/message/DiscordMessageHandler.java`
- Screenshot capture utility: `src/main/java/TcCrewPlugin/util/Utils.java`
- Tests live under `src/test/java/TcCrewPlugin`.
- Build uses Gradle, Java target 11, JUnit 5.
- Current modified file before this handoff was created: `settings.gradle.kts`, with `rootProject.name = "ClanEventDinkPlugin"`.
- Implemented clan event files: `src/main/java/TcCrewPlugin/ClanEventManager.java` and `src/main/java/TcCrewPlugin/ClanEventOverlay.java`.

There is no custom Dink panel class in the repo. The plugin uses RuneLite's generated config UI from `DinkPluginConfig` annotations, so "Dink panel" likely means the plugin config panel, not a custom Swing panel.

## Important Existing Behavior

Webhook selection for normal notifiers is centralized in `BaseNotifier#createMessage(String overrideUrl, boolean sendImage, NotificationBody<?> body)`:

- Seasonal worlds can redirect to `config.leaguesWebhook()` when `SeasonalPolicy.FORWARD_TO_LEAGUES` applies.
- Otherwise each notifier can provide a per-notifier override URL.
- If no override is set, it falls back to `config.primaryWebhook()`.
- The selected URL is passed to `DiscordMessageHandler#createMessage`.

External plugin notifications are a partial exception:

- `ExternalPluginNotifier#handleNotify` calls `input.getUrls(this::getWebhookUrl)` and then passes those URLs into `BaseNotifier#createMessage`.
- If external requests provide custom URLs, the clan event override should still win if the requirement is literally "override all existing webhooks."

Screenshot capture is centralized in `DiscordMessageHandler#captureScreenshot`, which delegates to `Utils.captureScreenshot`.

## Implementation Status

### 1. Clan Event Config

Implemented in `DinkPluginConfig` with a new `Clan Event` section:

- `clanEventEnabled` boolean, name like `Clan Event Active`.
- `clanEventWebhook` string, name like `Clan Event Webhook URLs`.
- `clanEventEndTime` string, name like `Clan Event End Time`.
- `clanEventSecretCode` string, name like `Clan Event Secret Code`.

The end time accepts ISO-8601 instants, offset date-times, and local date-times.

### 2. Event State Helper

Implemented as:

`src/main/java/TcCrewPlugin/ClanEventManager.java`

Responsibilities:

- Read config values.
- Decide whether the clan event is active now.
- Expose `String getActiveWebhookOverride()`.
- Expose `String getOverlayText()` or separate `getSecretCode()`/`getDateText()`.
- On each tick, if `clanEventEnabled` is true and the configured end time has passed, set `clanEventEnabled` to false through `DinkPluginConfig#setClanEventEnabled`.
- Add a chat warning/success through `TcCrewPlugin` when an event auto-ends.

It is injected into `TcCrewPlugin`, initialized on startup, notified on config changes, and ticked from `TcCrewPlugin#onGameTick`.

### 3. Override All Webhooks

Implemented in `BaseNotifier#createMessage` so the clan event webhook wins before seasonal/per-notifier/primary routing.

Current priority:

1. Active clan event webhook, if event is active and webhook is non-blank.
2. Existing seasonal redirect logic.
3. Per-notifier override.
4. Primary webhook.

This preserves existing behavior outside active events.

Because `ExternalPluginNotifier` ultimately calls `BaseNotifier#createMessage`, the event webhook should also win over external plugin custom URL lists.

### 4. Display Secret Code and Date On Screen

Implemented as:

`src/main/java/TcCrewPlugin/ClanEventOverlay.java`

Implementation notes:

- Extend `net.runelite.client.ui.overlay.Overlay`.
- Inject `DinkPluginConfig` or `ClanEventManager`.
- Render only when the event is active and a secret code is non-blank.
- Text should include the secret code and current date, for example `Code: ABC123 | 2026-06-06`.
- Register/unregister it in `TcCrewPlugin` using `OverlayManager`.
- Use a top-left or top-right position so screenshots naturally include it.

It is registered and unregistered in `TcCrewPlugin`. It renders the manager-provided text at top left.

### 5. Ensure Secret Code Appears In All Screenshots

Implemented in `DiscordMessageHandler#captureScreenshot` by stamping the clan event code/date directly onto the captured `BufferedImage` before rescaling/compression. This guarantees Dink screenshots include the code even if overlay capture behavior differs by client mode.

## Files To Touch

- `src/main/java/TcCrewPlugin/DinkPluginConfig.java`
  - Add clan event config section and items.
- `src/main/java/TcCrewPlugin/tccrewplugin.java`
  - Inject `ClanEventManager`, `ClanEventOverlay`, and `OverlayManager`.
  - Initialize manager and register overlay on startup.
  - Unregister overlay on shutdown.
  - Call manager on game ticks.
- `src/main/java/TcCrewPlugin/notifiers/BaseNotifier.java`
  - Make event webhook override all normal routing.
- `src/main/java/TcCrewPlugin/notifiers/ExternalPluginNotifier.java`
  - Check whether any URL-list path bypasses the override. Adjust only if needed.
- `src/main/java/TcCrewPlugin/message/DiscordMessageHandler.java`
  - Only touch if overlay is not captured in screenshots or to add guaranteed image stamping.
- New: `src/main/java/TcCrewPlugin/ClanEventManager.java`
- New: `src/main/java/TcCrewPlugin/ClanEventOverlay.java`
- Tests under `src/test/java/TcCrewPlugin` and/or `src/test/java/TcCrewPlugin/notifiers`.

## Testing Plan

Run:

```sh
./gradlew test
```

Current verification blocker: this session could not run Gradle because no Java runtime was available.

Add/update tests for:

- Event disabled: existing webhook routing unchanged.
- Event enabled with webhook: all notifier messages use event webhook. Focused `BaseNotifierTest` added.
- Event enabled with blank webhook: existing routing stays unchanged. Focused `BaseNotifierTest` added.
- Seasonal world plus event: event webhook wins.
- External plugin request with custom URLs plus event: event webhook wins. Focused `ExternalPluginNotifierTest` added.
- End time passed: manager disables `clanEventEnabled` and routing reverts. Focused `ClanEventManagerTest` added.
- Secret code/date overlay renders when active and hides when inactive.

Manual verification in RuneLite:

- Enable event in plugin config.
- Enter event webhook, secret code, and a near-future end time.
- Trigger at least one notification with a screenshot.
- Confirm Discord receives the event webhook during the event.
- Confirm overlay text is visible in game and in uploaded screenshots.
- Wait for end time and confirm notifications return to original webhooks.

## Open Decisions

- Exact timer input UX: string timestamp, epoch millis, or duration minutes. RuneLite config UI is limited, so a string timestamp is simplest.
- Whether event webhook is required when event is enabled. Current behavior: blank event webhook leaves normal routing intact, but the event can still display a code.
- Whether the secret code should display only during active clan events. Current behavior: yes.
- Whether "date" means local calendar date or event date. Current behavior: current local date using `LocalDate.now()`.

## Risks

- RuneLite overlay capture may vary. Test that the overlay appears in Dink screenshots; if not, add direct screenshot stamping.
- Config import/export currently treats webhook config keys specially in `SettingsManager`. If the clan event webhook should participate in imports/exports as a webhook, include it in the webhook key set by placing it in the webhook section or adding it explicitly.
- Do not rename `discordWebhook`; it is intentionally preserved for backwards compatibility.

