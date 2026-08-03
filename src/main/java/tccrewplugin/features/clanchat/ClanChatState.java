package tccrewplugin.features.clanchat;

import java.time.Instant;

public final class ClanChatState
{
	private final String clanName;
	private final int queueSize;
	private final int queueCapacity;
	private final Instant lastDelivery;

	public ClanChatState(String clanName, int queueSize, int queueCapacity, Instant lastDelivery)
	{
		this.clanName = clanName;
		this.queueSize = queueSize;
		this.queueCapacity = queueCapacity;
		this.lastDelivery = lastDelivery;
	}

	public String getClanName()
	{
		return clanName;
	}

	public int getQueueSize()
	{
		return queueSize;
	}

	public int getQueueCapacity()
	{
		return queueCapacity;
	}

	public Instant getLastDelivery()
	{
		return lastDelivery;
	}
}
