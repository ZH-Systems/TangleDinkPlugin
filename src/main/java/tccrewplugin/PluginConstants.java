package tccrewplugin;

public final class PluginConstants
{
	public static final String PLUGIN_NAME = "Tangle Dink Plugin";
	public static final String CONFIG_GROUP = "tangle-dink";
	public static final String VERSION = "1.0.0";

	public static final String DEFAULT_API_BASE_URL = "https://dev.example.invalid";
	public static final String DEFAULT_CLAN_WEBHOOK_ENDPOINT = "https://dev.example.invalid/api/clan/webhook";
	public static final int DEFAULT_SYNC_INTERVAL_SECONDS = 10;
	public static final int MIN_SYNC_INTERVAL_SECONDS = 5;
	public static final int DEFAULT_WEBHOOK_QUEUE_CAPACITY = 256;
	public static final int MIN_WEBHOOK_QUEUE_CAPACITY = 1;
	public static final int DEFAULT_WEBHOOK_MINIMUM_INTERVAL_MS = 750;
	public static final int MIN_WEBHOOK_MINIMUM_INTERVAL_MS = 0;
	public static final int MAX_RETRY_ATTEMPTS = 6;
	public static final int DEFAULT_HTTP_TIMEOUT_SECONDS = 15;
	public static final int DEFAULT_MANIFEST_REFRESH_SECONDS = 60;
	public static final int DEFAULT_HISTORY_LIMIT = 64;
	public static final int DEFAULT_COLLECTION_LOG_MAPPING_VERSION = 1;
	public static final int DEFAULT_COLLECTION_LOG_ITEM_COUNT = 0;
	public static final int DEFAULT_PLAYER_SUBMISSION_SCHEMA_VERSION = 1;
	public static final int DEFAULT_CLAN_WEBHOOK_SCHEMA_VERSION = 1;

	private PluginConstants()
	{
	}
}
