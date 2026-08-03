package tccrewplugin.clanchat;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.base.Ticker;

import java.util.concurrent.TimeUnit;

public class ClanMessageDeduplicator
{
	private final Cache<String, Boolean> cache;

	public ClanMessageDeduplicator()
	{
		this(Ticker.systemTicker());
	}

	public ClanMessageDeduplicator(Ticker ticker)
	{
		this.cache = CacheBuilder.newBuilder()
			.expireAfterWrite(5, TimeUnit.SECONDS)
			.maximumSize(500)
			.ticker(ticker)
			.build();
	}

	public boolean recordIfNew(String key)
	{
		return cache.asMap().putIfAbsent(key, Boolean.TRUE) == null;
	}

	public void clear()
	{
		cache.invalidateAll();
	}
}
