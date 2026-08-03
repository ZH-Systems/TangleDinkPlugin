package tccrewplugin.sync;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import tccrewplugin.collectionlog.CollectionLogService;
import tccrewplugin.sync.model.CollectionLogPayload;
import tccrewplugin.sync.model.PlayerSnapshot;
import tccrewplugin.sync.model.SyncManifest;

import javax.inject.Singleton;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public class PlayerSnapshotService
{
	private final Client client;
	private final CollectionLogService collectionLogService;

	public PlayerSnapshotService(Client client, CollectionLogService collectionLogService)
	{
		this.client = client;
		this.collectionLogService = collectionLogService;
	}

	public PlayerSnapshot capture(SyncManifest manifest, boolean syncSkills, boolean syncVarbits, boolean syncVarps, boolean syncCollectionLog)
	{
		Map<Integer, Integer> varbits = new LinkedHashMap<>();
		if (syncVarbits && manifest != null)
		{
			for (Integer varbitId : manifest.getVarbits())
			{
				varbits.put(varbitId, safeVarbit(varbitId));
			}
		}

		Map<Integer, Integer> varps = new LinkedHashMap<>();
		if (syncVarps && manifest != null)
		{
			for (Integer varpId : manifest.getVarps())
			{
				varps.put(varpId, safeVarp(varpId));
			}
		}

		Map<String, Integer> levels = new LinkedHashMap<>();
		if (syncSkills)
		{
			for (Skill skill : Skill.values())
			{
				levels.put(skill.name(), client.getRealSkillLevel(skill));
			}
		}

		CollectionLogPayload collectionLog = syncCollectionLog ? collectionLogService.currentPayload() : null;
		return new PlayerSnapshot(varbits, varps, levels, collectionLog, Instant.now());
	}

	private int safeVarbit(int varbitId)
	{
		try
		{
			return client.getVarbitValue(varbitId);
		}
		catch (RuntimeException ex)
		{
			return 0;
		}
	}

	private int safeVarp(int varpId)
	{
		try
		{
			return client.getVarpValue(varpId);
		}
		catch (RuntimeException ex)
		{
			return 0;
		}
	}
}
