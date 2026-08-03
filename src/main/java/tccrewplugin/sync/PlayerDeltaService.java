package tccrewplugin.sync;

import tccrewplugin.sync.model.CollectionLogPayload;
import tccrewplugin.sync.model.PlayerDelta;
import tccrewplugin.sync.model.PlayerSnapshot;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PlayerDeltaService
{
	public PlayerDelta diff(PlayerSnapshot previous, PlayerSnapshot current)
	{
		if (current == null)
		{
			return new PlayerDelta(Map.of(), Map.of(), Map.of(), null, Instant.now());
		}

		if (previous == null)
		{
			return new PlayerDelta(current.getVarbits(), current.getVarps(), current.getRealLevels(), current.getCollectionLog(), current.getCapturedAt());
		}

		Map<Integer, Integer> varbits = diffMap(previous.getVarbits(), current.getVarbits());
		Map<Integer, Integer> varps = diffMap(previous.getVarps(), current.getVarps());
		Map<String, Integer> realLevels = diffMap(previous.getRealLevels(), current.getRealLevels());
		CollectionLogPayload collectionLog = Objects.equals(previous.getCollectionLog(), current.getCollectionLog()) ? null : current.getCollectionLog();
		return new PlayerDelta(varbits, varps, realLevels, collectionLog, current.getCapturedAt());
	}

	private static <K, V> Map<K, V> diffMap(Map<K, V> previous, Map<K, V> current)
	{
		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : current.entrySet())
		{
			V previousValue = previous.get(entry.getKey());
			if (!Objects.equals(previousValue, entry.getValue()))
			{
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}
}
