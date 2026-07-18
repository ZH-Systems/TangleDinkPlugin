package tccrewplugin.collectionlog;

import lombok.extern.slf4j.Slf4j;
import tccrewplugin.PluginConstants;
import tccrewplugin.sync.model.CollectionLogPayload;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Singleton
public class CollectionLogService
{
	private final CollectionLogItemMapper itemMapper = new CollectionLogItemMapper();
	private final CollectionLogCacheParser cacheParser = new CollectionLogCacheParser();
	private final AtomicReference<CollectionLogCaptureSession> activeSession = new AtomicReference<>();
	private volatile CollectionLogItemMapper.Mapping mapping = itemMapper.empty();

	public synchronized void setManifestItems(List<Integer> manifestItems, List<Integer> cacheItems)
	{
		mapping = itemMapper.build(manifestItems, cacheItems);
	}

	public CollectionLogItemMapper.Mapping getMapping()
	{
		return mapping;
	}

	public void beginCapture()
	{
		activeSession.compareAndSet(null, new CollectionLogCaptureSession(PluginConstants.DEFAULT_COLLECTION_LOG_MAPPING_VERSION, mapping.getOrderedItems().size()));
	}

	public void stopCapture()
	{
		CollectionLogCaptureSession session = activeSession.getAndSet(null);
		if (session != null)
		{
			log.debug("Collection log capture session ended with {} owned slots", session.getOwnedCount());
		}
	}

	public void resetForAccountChange()
	{
		stopCapture();
	}

	public void ingestScriptArguments(Object scriptArguments)
	{
		CollectionLogCaptureSession session = activeSession.get();
		if (session == null)
		{
			return;
		}

		for (Integer itemId : cacheParser.parseItemIds(scriptArguments))
		{
			Integer index = mapping.getIndexByItemId().get(itemId);
			if (index != null)
			{
				session.markOwned(index);
			}
		}
	}

	public CollectionLogPayload currentPayload()
	{
		CollectionLogCaptureSession session = activeSession.get();
		return session == null ? null : session.toPayload();
	}
}
