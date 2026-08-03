package tccrewplugin.sync;

import org.junit.jupiter.api.Test;
import tccrewplugin.sync.model.CollectionLogPayload;
import tccrewplugin.sync.model.PlayerSnapshot;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayerDeltaServiceTest
{
	private final PlayerDeltaService service = new PlayerDeltaService();

	@Test
	public void identicalSnapshotsProduceNoDelta()
	{
		PlayerSnapshot snapshot = snapshot(Map.of(1, 2), Map.of(3, 4), Map.of("Attack", 99), new CollectionLogPayload(1, 2, 1, "AQ=="));
		assertTrue(service.diff(snapshot, snapshot).isEmpty());
	}

	@Test
	public void changedSkillProducesDelta()
	{
		PlayerSnapshot previous = snapshot(Map.of(), Map.of(), Map.of("Attack", 99), null);
		PlayerSnapshot current = snapshot(Map.of(), Map.of(), Map.of("Attack", 98), null);
		assertEquals(1, service.diff(previous, current).getRealLevels().size());
	}

	@Test
	public void changedVarbitProducesDelta()
	{
		PlayerSnapshot previous = snapshot(Map.of(1, 1), Map.of(), Map.of(), null);
		PlayerSnapshot current = snapshot(Map.of(1, 2), Map.of(), Map.of(), null);
		assertEquals(1, service.diff(previous, current).getVarbits().size());
	}

	@Test
	public void newManifestKeyIsIncluded()
	{
		PlayerSnapshot previous = snapshot(Map.of(1, 1), Map.of(), Map.of(), null);
		PlayerSnapshot current = snapshot(Map.of(1, 1, 2, 3), Map.of(), Map.of(), null);
		assertTrue(service.diff(previous, current).getVarbits().containsKey(2));
	}

	@Test
	public void collectionLogEqualityIsDetected()
	{
		PlayerSnapshot previous = snapshot(Map.of(), Map.of(), Map.of(), new CollectionLogPayload(1, 2, 1, "AQ=="));
		PlayerSnapshot current = snapshot(Map.of(), Map.of(), Map.of(), new CollectionLogPayload(1, 2, 1, "AQ=="));
		assertTrue(service.diff(previous, current).getCollectionLog() == null);
	}

	private PlayerSnapshot snapshot(Map<Integer, Integer> varbits, Map<Integer, Integer> varps, Map<String, Integer> levels, CollectionLogPayload collectionLog)
	{
		return new PlayerSnapshot(new LinkedHashMap<>(varbits), new LinkedHashMap<>(varps), new LinkedHashMap<>(levels), collectionLog, Instant.parse("2026-07-17T22:30:00Z"));
	}
}
