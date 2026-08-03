package tccrewplugin.clanchat;

import tccrewplugin.clanchat.model.ClanMessageRecord;
import tccrewplugin.clanchat.model.ClanMessageType;
import lombok.experimental.UtilityClass;
import net.runelite.api.ChatMessageType;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@UtilityClass
public class ClanMessageClassifier
{
	public ClanMessageRecord classify(
		String chatTypeName,
		String sender,
		String senderRank,
		String clanName,
		String message,
		Integer world,
		boolean guest,
		Instant occurredAt,
		boolean test
	)
	{
		String text = ClanMessageSanitizer.sanitizeMessage(message);
		ChatMessageType messageType = parseChatMessageType(chatTypeName);
		SystemMessageType systemMessageType = getSystemMessageType(text, messageType);
		ClanMessageType clanMessageType = inferTypeFromText(text);
		if (clanMessageType == ClanMessageType.UNKNOWN)
		{
			clanMessageType = mapType(chatTypeName, messageType, systemMessageType, guest, test);
		}
		String fingerprint = String.join("|",
			lower(chatTypeName),
			lower(sender),
			lower(senderRank),
			lower(clanName),
			lower(text),
			String.valueOf(world),
			String.valueOf(guest),
			String.valueOf(test));

		return new ClanMessageRecord(
			UUID.randomUUID(),
			occurredAt == null ? Instant.now() : occurredAt,
			clanMessageType,
			sender,
			senderRank,
			text,
			guest,
			clanName,
			world,
			fingerprint,
			test
		);
	}

	public SystemMessageType getSystemMessageType(String message, ChatMessageType messageType)
	{
		if (messageType != ChatMessageType.CLAN_MESSAGE)
		{
			return SystemMessageType.NORMAL;
		}

		String text = message == null ? "" : message.toLowerCase(Locale.ENGLISH);

		if (text.contains("to talk in your clan's channel, start each line of chat with"))
		{
			return SystemMessageType.LOGIN;
		}
		if (text.contains("received special loot from a raid:"))
		{
			return SystemMessageType.RAID_DROP;
		}
		if (text.contains("received a new collection log item:"))
		{
			return SystemMessageType.COLLECTION_LOG;
		}
		if (text.contains("received a clue item:"))
		{
			return SystemMessageType.CLUE_DROP;
		}
		if (text.contains("has a funny feeling like")
			|| text.contains("backpack:")
			|| text.contains("something special:"))
		{
			return SystemMessageType.PET_DROP;
		}
		if (text.contains("received a drop:"))
		{
			return SystemMessageType.DROP;
		}
		if (text.contains("personal best:"))
		{
			return SystemMessageType.PERSONAL_BEST;
		}
		if (text.contains("has completed a quest:"))
		{
			return SystemMessageType.QUESTS;
		}
		if (text.contains("tier of rewards from combat achievements!")
			|| (text.contains("has completed") && text.contains("combat task")))
		{
			return SystemMessageType.COMBAT_ACHIEVEMENTS;
		}
		if (text.contains("has completed the") && text.contains("diary."))
		{
			return SystemMessageType.DIARY;
		}
		if (text.contains("has reached a total level of")
			|| (text.contains("has reached") && (text.contains(" level") || text.contains(" xp"))))
		{
			return SystemMessageType.LEVEL_UP;
		}
		if (text.contains("has defeated") || text.contains("has been defeated by"))
		{
			return SystemMessageType.PVP;
		}
		if (text.contains("has left.")
			|| text.contains("has been invited into the clan by")
			|| text.contains("has joined."))
		{
			return SystemMessageType.ATTENDANCE;
		}

		return SystemMessageType.UNKNOWN;
	}

	private ChatMessageType parseChatMessageType(String chatTypeName)
	{
		if (chatTypeName == null)
		{
			return ChatMessageType.GAMEMESSAGE;
		}

		if ("CLAN_CHAT".equalsIgnoreCase(chatTypeName)
			|| "CLAN_BROADCAST".equalsIgnoreCase(chatTypeName))
		{
			return ChatMessageType.CLAN_CHAT;
		}
		if ("CLAN_MESSAGE".equalsIgnoreCase(chatTypeName))
		{
			return ChatMessageType.CLAN_MESSAGE;
		}
		if ("GAMEMESSAGE".equalsIgnoreCase(chatTypeName))
		{
			return ChatMessageType.GAMEMESSAGE;
		}

		try
		{
			return ChatMessageType.valueOf(chatTypeName.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex)
		{
			return ChatMessageType.GAMEMESSAGE;
		}
	}

	private ClanMessageType mapType(String chatTypeName, ChatMessageType messageType, SystemMessageType systemMessageType, boolean guest, boolean test)
	{
		if (test)
		{
			return ClanMessageType.CLAN_BROADCAST;
		}

		if (messageType == ChatMessageType.CLAN_CHAT)
		{
			if (guest || "CLAN_BROADCAST".equalsIgnoreCase(chatTypeName))
			{
				return ClanMessageType.GUEST_BROADCAST;
			}
			return ClanMessageType.CHAT;
		}

		if (messageType == ChatMessageType.CLAN_MESSAGE)
		{
			switch (systemMessageType)
			{
				case LOGIN:
				case ATTENDANCE:
					return ClanMessageType.SYSTEM;
				case LEVEL_UP:
					return ClanMessageType.LEVEL_UP;
				case QUESTS:
					return ClanMessageType.QUEST;
				case COLLECTION_LOG:
					return ClanMessageType.COLLECTION_LOG;
				case COMBAT_ACHIEVEMENTS:
					return ClanMessageType.COMBAT_ACHIEVEMENT;
				case DROP:
				case RAID_DROP:
				case PET_DROP:
				case CLUE_DROP:
					return ClanMessageType.LOOT;
				case PERSONAL_BEST:
					return ClanMessageType.CLAN_BROADCAST;
				case PVP:
					return ClanMessageType.CLAN_BROADCAST;
				case DIARY:
					return ClanMessageType.CLAN_BROADCAST;
				case UNKNOWN:
				case NORMAL:
				default:
					return ClanMessageType.SYSTEM;
			}
		}

		return ClanMessageType.UNKNOWN;
	}

	private ClanMessageType inferTypeFromText(String message)
	{
		String text = message == null ? "" : message.toLowerCase(Locale.ENGLISH);
		if (text.contains("received a new collection log item"))
		{
			return ClanMessageType.COLLECTION_LOG;
		}
		if (text.contains("personal best"))
		{
			return ClanMessageType.CLAN_BROADCAST;
		}
		if (text.contains("completed a quest"))
		{
			return ClanMessageType.QUEST;
		}
		if (text.contains("combat task") || text.contains("combat achievements"))
		{
			return ClanMessageType.COMBAT_ACHIEVEMENT;
		}

		return ClanMessageType.UNKNOWN;
	}

	private String lower(String value)
	{
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
