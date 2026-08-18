package tccrewplugin.sync.model;

import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
public class PlayerSubmission
{
	int schemaVersion;
	String username;
	String profile;
	String pluginVersion;
	Instant capturedAt;
	Data data;

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

	@Value
	public static class Data
	{
		Map<Integer, Integer> varbits;
		Map<Integer, Integer> varps;
		Map<String, Integer> levels;
		CollectionLogPayload collectionLog;

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
	}
}
