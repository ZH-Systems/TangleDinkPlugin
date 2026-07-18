package tccrewplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(PluginConstants.CONFIG_GROUP)
public interface TangleDinkConfig extends Config
{
	@ConfigSection(
		name = "Player Sync",
		description = "Manifest and player-data synchronization settings",
		position = 0,
		closedByDefault = false
	)
	String playerSyncSection = "Player Sync";

	@ConfigSection(
		name = "Clan Webhooks",
		description = "Clan chat webhook forwarding settings",
		position = 10,
		closedByDefault = false
	)
	String clanWebhookSection = "Clan Webhooks";

	@ConfigSection(
		name = "Features",
		description = "Enable or disable feature categories",
		position = 20,
		closedByDefault = false
	)
	String featuresSection = "Features";

	@ConfigSection(
		name = "Advanced",
		description = "Advanced and diagnostic settings",
		position = 30,
		closedByDefault = true
	)
	String advancedSection = "Advanced";

	@ConfigItem(
		keyName = "apiBaseUrl",
		name = "API Base URL",
		description = "Base URL for the player sync API",
		position = 0,
		section = playerSyncSection
	)
	default String apiBaseUrl()
	{
		return PluginConstants.DEFAULT_API_BASE_URL;
	}

	@ConfigItem(
		keyName = "apiToken",
		name = "API Token",
		description = "Bearer token used for player sync submissions",
		position = 1,
		section = playerSyncSection,
		secret = true
	)
	default String apiToken()
	{
		return "";
	}

	@ConfigItem(
		keyName = "automaticSyncEnabled",
		name = "Automatic Sync",
		description = "Synchronize player data automatically while logged in",
		position = 2,
		section = playerSyncSection
	)
	default boolean automaticSyncEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncIntervalSeconds",
		name = "Sync Interval",
		description = "How often to attempt player synchronization",
		position = 3,
		section = playerSyncSection
	)
	@Units(Units.SECONDS)
	@Range(min = PluginConstants.MIN_SYNC_INTERVAL_SECONDS, max = 3600)
	default int syncIntervalSeconds()
	{
		return PluginConstants.DEFAULT_SYNC_INTERVAL_SECONDS;
	}

	@ConfigItem(
		keyName = "syncSkills",
		name = "Sync Skills",
		description = "Include real skill levels in player snapshots",
		position = 4,
		section = playerSyncSection
	)
	default boolean syncSkills()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncVarbits",
		name = "Sync Varbits",
		description = "Include manifest-selected varbits in player snapshots",
		position = 5,
		section = playerSyncSection
	)
	default boolean syncVarbits()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncVarps",
		name = "Sync Varps",
		description = "Include manifest-selected varps in player snapshots",
		position = 6,
		section = playerSyncSection
	)
	default boolean syncVarps()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncCollectionLog",
		name = "Sync Collection Log",
		description = "Include collection-log payloads when a capture session is available",
		position = 7,
		section = playerSyncSection
	)
	default boolean syncCollectionLog()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableAccountFeature",
		name = "Enable Account Feature",
		description = "Show the account feature category in the sidebar",
		position = 0,
		section = featuresSection
	)
	default boolean enableAccountFeature()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableSyncFeature",
		name = "Enable Synchronization Feature",
		description = "Show the synchronization feature category in the sidebar",
		position = 1,
		section = featuresSection
	)
	default boolean enableSyncFeature()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableCollectionLogFeature",
		name = "Enable Collection Log Feature",
		description = "Show the collection log feature category in the sidebar",
		position = 2,
		section = featuresSection
	)
	default boolean enableCollectionLogFeature()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableClanChatFeature",
		name = "Enable Clan Feature",
		description = "Show the clan chat webhook feature category in the sidebar",
		position = 3,
		section = featuresSection
	)
	default boolean enableClanChatFeature()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableSettingsFeature",
		name = "Enable Settings Feature",
		description = "Show the settings feature category in the sidebar",
		position = 4,
		section = featuresSection
	)
	default boolean enableSettingsFeature()
	{
		return true;
	}

	@ConfigItem(
		keyName = "debugLogging",
		name = "Debug Logging",
		description = "Enable extra diagnostic logging",
		position = 0,
		section = advancedSection
	)
	default boolean debugLogging()
	{
		return false;
	}

	@ConfigItem(
		keyName = "clanWebhookEnabled",
		name = "Enable Clan Webhooks",
		description = "Forward eligible clan chat messages to the configured endpoint",
		position = 0,
		section = clanWebhookSection,
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean clanWebhookEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "clanWebhookEndpoint",
		name = "Webhook Endpoint",
		description = "HTTP endpoint that receives clan webhook payloads",
		position = 1,
		section = clanWebhookSection
	)
	default String clanWebhookEndpoint()
	{
		return PluginConstants.DEFAULT_CLAN_WEBHOOK_ENDPOINT;
	}

	@ConfigItem(
		keyName = "clanWebhookSecret",
		name = "Webhook Secret",
		description = "Secret used to authenticate webhook requests",
		position = 2,
		section = clanWebhookSection,
		secret = true
	)
	default String clanWebhookSecret()
	{
		return "";
	}

	@ConfigItem(
		keyName = "clanWebhookAuthenticationMode",
		name = "Webhook Authentication Mode",
		description = "How the secret is transmitted to the webhook endpoint",
		position = 3,
		section = clanWebhookSection
	)
	default String clanWebhookAuthenticationMode()
	{
		return "BEARER_TOKEN";
	}

	@ConfigItem(
		keyName = "requiredClanName",
		name = "Required Clan Name",
		description = "Only forward messages when the active clan matches this name",
		position = 4,
		section = clanWebhookSection
	)
	default String requiredClanName()
	{
		return "";
	}

	@ConfigItem(
		keyName = "sendPublicClanMessages",
		name = "Send Clan Chat Messages",
		description = "Forward normal clan chat messages",
		position = 5,
		section = clanWebhookSection
	)
	default boolean sendPublicClanMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendClanBroadcasts",
		name = "Send Clan Broadcasts",
		description = "Forward clan broadcast messages",
		position = 6,
		section = clanWebhookSection
	)
	default boolean sendClanBroadcasts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendGuestBroadcasts",
		name = "Send Guest Broadcasts",
		description = "Forward guest-related clan broadcasts",
		position = 7,
		section = clanWebhookSection
	)
	default boolean sendGuestBroadcasts()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sendSystemClanMessages",
		name = "Send System Messages",
		description = "Forward clan system messages",
		position = 8,
		section = clanWebhookSection
	)
	default boolean sendSystemClanMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeSenderRank",
		name = "Include Sender Rank",
		description = "Include clan rank data when available",
		position = 9,
		section = clanWebhookSection
	)
	default boolean includeSenderRank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeWorldNumber",
		name = "Include World Number",
		description = "Include the current world number in webhook payloads",
		position = 10,
		section = clanWebhookSection
	)
	default boolean includeWorldNumber()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeTimestamp",
		name = "Include Timestamp",
		description = "Include the current time in webhook payloads",
		position = 11,
		section = clanWebhookSection
	)
	default boolean includeTimestamp()
	{
		return true;
	}

	@ConfigItem(
		keyName = "redactUrls",
		name = "Redact URLs",
		description = "Strip raw URLs from captured clan messages",
		position = 12,
		section = clanWebhookSection
	)
	default boolean redactUrls()
	{
		return true;
	}

	@ConfigItem(
		keyName = "queueFailedWebhookMessages",
		name = "Queue Failed Messages",
		description = "Retry failed webhook messages in the local queue",
		position = 13,
		section = clanWebhookSection
	)
	default boolean queueFailedWebhookMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "webhookQueueCapacity",
		name = "Webhook Queue Capacity",
		description = "Maximum number of clan webhook messages to buffer",
		position = 14,
		section = clanWebhookSection
	)
	@Range(min = PluginConstants.MIN_WEBHOOK_QUEUE_CAPACITY, max = 5000)
	default int webhookQueueCapacity()
	{
		return PluginConstants.DEFAULT_WEBHOOK_QUEUE_CAPACITY;
	}

	@ConfigItem(
		keyName = "webhookMinimumIntervalMilliseconds",
		name = "Minimum Delivery Interval",
		description = "Minimum delay between clan webhook deliveries",
		position = 15,
		section = clanWebhookSection
	)
	@Units(Units.MILLISECONDS)
	@Range(min = PluginConstants.MIN_WEBHOOK_MINIMUM_INTERVAL_MS, max = 60000)
	default int webhookMinimumIntervalMilliseconds()
	{
		return PluginConstants.DEFAULT_WEBHOOK_MINIMUM_INTERVAL_MS;
	}

	@ConfigItem(
		keyName = "approvedGuestUsernames",
		name = "Approved Guest Usernames",
		description = "Optional comma-separated guest allowlist for guest-related broadcasts",
		position = 16,
		section = clanWebhookSection
	)
	default String approvedGuestUsernames()
	{
		return "";
	}
}
