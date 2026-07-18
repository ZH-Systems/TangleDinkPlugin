package tccrewplugin.collectionlog;

import tccrewplugin.PluginConstants;
import tccrewplugin.sync.model.CollectionLogPayload;

import java.time.Instant;
import java.util.BitSet;

public class CollectionLogCaptureSession
{
	private final int mappingVersion;
	private final int totalItems;
	private final BitSet ownedItems = new BitSet();
	private Instant capturedAt;

	public CollectionLogCaptureSession(int mappingVersion, int totalItems)
	{
		this.mappingVersion = mappingVersion;
		this.totalItems = totalItems;
	}

	public void markOwned(int index)
	{
		if (index >= 0)
		{
			ownedItems.set(index);
			capturedAt = Instant.now();
		}
	}

	public CollectionLogPayload toPayload()
	{
		return new CollectionLogPayload(
			mappingVersion,
			totalItems,
			ownedItems.cardinality(),
			CollectionLogPayload.encode(ownedItems)
		);
	}

	public void clear()
	{
		ownedItems.clear();
		capturedAt = null;
	}

	public Instant getCapturedAt()
	{
		return capturedAt;
	}

	public int getOwnedCount()
	{
		return ownedItems.cardinality();
	}
}
