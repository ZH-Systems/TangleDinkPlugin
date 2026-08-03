package tccrewplugin.sync.model;

import java.time.Instant;
import java.util.Map;

public final class PlayerSubmission
{
	private final int schemaVersion;
	private final String username;
	private final String profile;
	private final String pluginVersion;
	private final Instant capturedAt;
	private final Data data;

	public PlayerSubmission(
		int schemaVersion,
		String username,
		String profile,
		String pluginVersion,
		Instant capturedAt,
		Data data
	)
	{
		this.schemaVersion = schemaVersion;
		this.username = username;
		this.profile = profile;
		this.pluginVersion = pluginVersion;
		this.capturedAt = capturedAt;
		this.data = data;
	}

	public int getSchemaVersion()
	{
		return schemaVersion;
	}

	public String getUsername()
	{
		return username;
	}

	public String getProfile()
	{
		return profile;
	}

	public String getPluginVersion()
	{
		return pluginVersion;
	}

	public Instant getCapturedAt()
	{
		return capturedAt;
	}

	public Data getData()
	{
		return data;
	}

	public static final class Data
	{
		private final Map<Integer, Integer> varbits;
		private final Map<Integer, Integer> varps;
		private final Map<String, Integer> levels;
		private final CollectionLogPayload collectionLog;

		public Data(
			Map<Integer, Integer> varbits,
			Map<Integer, Integer> varps,
			Map<String, Integer> levels,
			CollectionLogPayload collectionLog
		)
		{
			this.varbits = varbits;
			this.varps = varps;
			this.levels = levels;
			this.collectionLog = collectionLog;
		}

		public Map<Integer, Integer> getVarbits()
		{
			return varbits;
		}

		public Map<Integer, Integer> getVarps()
		{
			return varps;
		}

		public Map<String, Integer> getLevels()
		{
			return levels;
		}

		public CollectionLogPayload getCollectionLog()
		{
			return collectionLog;
		}
	}
}
