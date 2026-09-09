package tccrewplugin.clanchat.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ClanMessageRecord
{
	private final UUID eventId;
	private final Instant occurredAt;
	private final ClanMessageType type;
	private final String sender;
	private final String senderRank;
	private final String text;
	private final boolean guest;
	private final String clanName;
	private final Integer world;
	private final String fingerprint;
	private final boolean test;

	public ClanMessageRecord(
		UUID eventId,
		Instant occurredAt,
		ClanMessageType type,
		String sender,
		String senderRank,
		String text,
		boolean guest,
		String clanName,
		Integer world,
		String fingerprint,
		boolean test
	)
	{
		this.eventId = eventId;
		this.occurredAt = occurredAt;
		this.type = type;
		this.sender = sender;
		this.senderRank = senderRank;
		this.text = text;
		this.guest = guest;
		this.clanName = clanName;
		this.world = world;
		this.fingerprint = fingerprint;
		this.test = test;
	}

	public UUID getEventId()
	{
		return eventId;
	}

	public Instant getOccurredAt()
	{
		return occurredAt;
	}

	public ClanMessageType getType()
	{
		return type;
	}

	public String getSender()
	{
		return sender;
	}

	public String getSenderRank()
	{
		return senderRank;
	}

	public String getText()
	{
		return text;
	}

	public boolean isGuest()
	{
		return guest;
	}

	public String getClanName()
	{
		return clanName;
	}

	public Integer getWorld()
	{
		return world;
	}

	public String getFingerprint()
	{
		return fingerprint;
	}

	public boolean isTest()
	{
		return test;
	}

	@Override
	public String toString()
	{
		return "ClanMessageRecord{" +
			"type=" + type +
			", sender='" + sender + '\'' +
			", clanName='" + clanName + '\'' +
			", world=" + world +
			", test=" + test +
			'}';
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof ClanMessageRecord))
		{
			return false;
		}
		ClanMessageRecord that = (ClanMessageRecord) o;
		return Objects.equals(eventId, that.eventId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(eventId);
	}
}
