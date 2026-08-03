package tccrewplugin.clanchat;

import net.runelite.api.ChatMessageType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClanMessageClassifierTest
{
	static Stream<Arguments> cases()
	{
		return Stream.of(
			Arguments.of("received special loot from a raid: scythe", ChatMessageType.CLAN_MESSAGE, SystemMessageType.RAID_DROP),
			Arguments.of("received a drop: Rune axe", ChatMessageType.CLAN_MESSAGE, SystemMessageType.DROP),
			Arguments.of("has a funny feeling like backpack: something special:", ChatMessageType.CLAN_MESSAGE, SystemMessageType.PET_DROP),
			Arguments.of("personal best: 1:23", ChatMessageType.CLAN_MESSAGE, SystemMessageType.PERSONAL_BEST),
			Arguments.of("received a new collection log item: Dragon defender", ChatMessageType.CLAN_MESSAGE, SystemMessageType.COLLECTION_LOG),
			Arguments.of("has completed a quest: Cook's Assistant", ChatMessageType.CLAN_MESSAGE, SystemMessageType.QUESTS),
			Arguments.of("has defeated Zulrah", ChatMessageType.CLAN_MESSAGE, SystemMessageType.PVP),
			Arguments.of("has left.", ChatMessageType.CLAN_MESSAGE, SystemMessageType.ATTENDANCE),
			Arguments.of("has reached a total level of 2277", ChatMessageType.CLAN_MESSAGE, SystemMessageType.LEVEL_UP),
			Arguments.of("has completed a tier of rewards from Combat Achievements!", ChatMessageType.CLAN_MESSAGE, SystemMessageType.COMBAT_ACHIEVEMENTS),
			Arguments.of("received a clue item: Master clue scroll", ChatMessageType.CLAN_MESSAGE, SystemMessageType.CLUE_DROP),
			Arguments.of("has completed the Ardougne diary.", ChatMessageType.CLAN_MESSAGE, SystemMessageType.DIARY),
			Arguments.of("To talk in your clan's channel, start each line of chat with", ChatMessageType.CLAN_MESSAGE, SystemMessageType.LOGIN),
			Arguments.of("something else", ChatMessageType.CLAN_MESSAGE, SystemMessageType.UNKNOWN)
		);
	}

	@ParameterizedTest
	@MethodSource("cases")
	void classifiesMessages(String message, ChatMessageType type, SystemMessageType expected)
	{
		assertEquals(expected, ClanMessageClassifier.getSystemMessageType(message, type));
	}

	@Test
	void clanChatAlwaysNormal()
	{
		assertEquals(SystemMessageType.NORMAL, ClanMessageClassifier.getSystemMessageType("received a drop:", ChatMessageType.CLAN_CHAT));
	}
}
