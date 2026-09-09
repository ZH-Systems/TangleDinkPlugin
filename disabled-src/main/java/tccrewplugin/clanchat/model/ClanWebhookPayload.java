package tccrewplugin.clanchat.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ClanWebhookPayload
{
	private final int schemaVersion;
	private final UUID eventId;
	private final String eventType;
	private final Instant occurredAt;
	private final String pluginVersion;
	private final Player player;
	private final Clan clan;
	private final Message message;
	private final boolean test;

	public ClanWebhookPayload(
		int schemaVersion,
		UUID eventId,
		String eventType,
		Instant occurredAt,
		String pluginVersion,
		Player player,
		Clan clan,
		Message message,
		boolean test
	)
	{
		this.schemaVersion = schemaVersion;
		this.eventId = eventId;
		this.eventType = eventType;
		this.occurredAt = occurredAt;
		this.pluginVersion = pluginVersion;
		this.player = player;
		this.clan = clan;
		this.message = message;
		this.test = test;
	}

	public int getSchemaVersion()
	{
		return schemaVersion;
	}

	public UUID getEventId()
	{
		return eventId;
	}

	public String getEventType()
	{
		return eventType;
	}

	public Instant getOccurredAt()
	{
		return occurredAt;
	}

	public String getPluginVersion()
	{
		return pluginVersion;
	}

	public Player getPlayer()
	{
		return player;
	}

	public Clan getClan()
	{
		return clan;
	}

	public Message getMessage()
	{
		return message;
	}

	public boolean isTest()
	{
		return test;
	}

	@Override
	public String toString()
	{
		return "ClanWebhookPayload{" +
			"schemaVersion=" + schemaVersion +
			", eventId=" + eventId +
			", eventType='" + eventType + '\'' +
			", test=" + test +
			'}';
	}

	public static final class Player
	{
		private final String username;
		private final String profile;
		private final Integer world;

		public Player(String username, String profile, Integer world)
		{
			this.username = username;
			this.profile = profile;
			this.world = world;
		}

		public String getUsername()
		{
			return username;
		}

		public String getProfile()
		{
			return profile;
		}

		public Integer getWorld()
		{
			return world;
		}
	}

	public static final class Clan
	{
		private final String name;

		public Clan(String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}
	}

	public static final class Message
	{
		private final String type;
		private final String sender;
		private final String senderRank;
		private final String text;
		private final boolean guest;

		public Message(String type, String sender, String senderRank, String text, boolean guest)
		{
			this.type = type;
			this.sender = sender;
			this.senderRank = senderRank;
			this.text = text;
			this.guest = guest;
		}

		public String getType()
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
	}
}
