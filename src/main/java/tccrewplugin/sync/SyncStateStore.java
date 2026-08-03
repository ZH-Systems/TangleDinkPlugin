package tccrewplugin.sync;

import tccrewplugin.sync.model.PlayerIdentity;
import tccrewplugin.sync.model.PlayerSnapshot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyncStateStore
{
	private final Map<PlayerIdentity, PlayerSnapshot> snapshots = new ConcurrentHashMap<>();

	public PlayerSnapshot get(PlayerIdentity identity)
	{
		return snapshots.get(identity);
	}

	public void put(PlayerIdentity identity, PlayerSnapshot snapshot)
	{
		snapshots.put(identity, snapshot);
	}

	public void clear()
	{
		snapshots.clear();
	}
}
