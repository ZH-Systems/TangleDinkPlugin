package tccrewplugin.sync.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerDelta
{
	private final Map<Integer, Integer> varbits;
	private final Map<Integer, Integer> varps;
	private final Map<String, Integer> realLevels;
	private final CollectionLogPayload collectionLog;
	private final Instant capturedAt;

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

	public Map<Integer, Integer> getVarbits()
	{
		return varbits;
	}

	public Map<Integer, Integer> getVarps()
	{
		return varps;
	}

	public Map<String, Integer> getRealLevels()
	{
		return realLevels;
	}

	public CollectionLogPayload getCollectionLog()
	{
		return collectionLog;
	}

	public Instant getCapturedAt()
	{
		return capturedAt;
	}

	public boolean isEmpty()
	{
		return varbits.isEmpty() && varps.isEmpty() && realLevels.isEmpty() && collectionLog == null;
	}
}
