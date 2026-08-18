package tccrewplugin.sync.model;

import lombok.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Value
public class PlayerDelta
{
	Map<Integer, Integer> varbits;
	Map<Integer, Integer> varps;
	Map<String, Integer> realLevels;
	CollectionLogPayload collectionLog;
	Instant capturedAt;

	public PlayerDelta(
		Map<Integer, Integer> varbits,
		Map<Integer, Integer> varps,
		Map<String, Integer> realLevels,
		CollectionLogPayload collectionLog,
		Instant capturedAt
	)
	{
		this.varbits = Collections.unmodifiableMap(new LinkedHashMap<>(varbits));
		this.varps = Collections.unmodifiableMap(new LinkedHashMap<>(varps));
		this.realLevels = Collections.unmodifiableMap(new LinkedHashMap<>(realLevels));
		this.collectionLog = collectionLog;
		this.capturedAt = capturedAt;
	}

	public boolean isEmpty()
	{
		return varbits.isEmpty() && varps.isEmpty() && realLevels.isEmpty() && collectionLog == null;
	}
}
