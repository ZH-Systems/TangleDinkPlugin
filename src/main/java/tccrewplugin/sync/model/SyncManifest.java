package tccrewplugin.sync.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class SyncManifest
{
	private final int version;
	private final List<Integer> varbits;
	private final List<Integer> varps;
	private final List<Integer> collectionLogItems;

	public SyncManifest(int version, List<Integer> varbits, List<Integer> varps, List<Integer> collectionLogItems)
	{
		this.version = version;
		this.varbits = Collections.unmodifiableList(new ArrayList<>(varbits));
		this.varps = Collections.unmodifiableList(new ArrayList<>(varps));
		this.collectionLogItems = Collections.unmodifiableList(new ArrayList<>(collectionLogItems));
	}

	public int getVersion()
	{
		return version;
	}

	public List<Integer> getVarbits()
	{
		return varbits;
	}

	public List<Integer> getVarps()
	{
		return varps;
	}

	public List<Integer> getCollectionLogItems()
	{
		return collectionLogItems;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof SyncManifest))
		{
			return false;
		}
		SyncManifest that = (SyncManifest) o;
		return version == that.version && Objects.equals(varbits, that.varbits) && Objects.equals(varps, that.varps) && Objects.equals(collectionLogItems, that.collectionLogItems);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(version, varbits, varps, collectionLogItems);
	}
}
