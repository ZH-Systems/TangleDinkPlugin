package tccrewplugin.clanchat;

import lombok.experimental.UtilityClass;
import net.runelite.api.ChatMessageType;

import java.util.Locale;

@UtilityClass
public class ClanMessageClassifier
{
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
}
