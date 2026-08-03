package tccrewplugin.clanchat;

import net.runelite.api.Client;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClanMemberTitleResolverTest
{
	private Client client;
	private ClanMemberTitleResolver resolver;

	@BeforeEach
	void setUp()
	{
		client = mock(Client.class);
		resolver = new ClanMemberTitleResolver(client);
	}

	@Test
	void resolvesTitleForMember()
	{
		ClanChannel channel = mock(ClanChannel.class);
		ClanSettings settings = mock(ClanSettings.class);
		ClanChannelMember member = mock(ClanChannelMember.class);
		ClanTitle title = new ClanTitle(1, "Queen");

		when(client.getClanSettings()).thenReturn(settings);
		when(channel.findMember("Zach")).thenReturn(member);
		when(member.getRank()).thenReturn(ClanRank.OWNER);
		when(settings.titleForRank(ClanRank.OWNER)).thenReturn(title);

		assertEquals("Queen", resolver.resolveClanTitle(channel, "Zach"));
	}

	@Test
	void returnsNullWhenMemberMissing()
	{
		ClanChannel channel = mock(ClanChannel.class);
		when(client.getClanSettings()).thenReturn(mock(ClanSettings.class));

		assertNull(resolver.resolveClanTitle(channel, "Zach"));
	}

	@Test
	void returnsNullWhenSettingsMissing()
	{
		ClanChannel channel = mock(ClanChannel.class);
		assertNull(resolver.resolveClanTitle(channel, "Zach"));
	}

	@Test
	void returnsNullWhenTitleMissing()
	{
		ClanChannel channel = mock(ClanChannel.class);
		ClanSettings settings = mock(ClanSettings.class);
		ClanChannelMember member = mock(ClanChannelMember.class);

		when(client.getClanSettings()).thenReturn(settings);
		when(channel.findMember("Zach")).thenReturn(member);
		when(member.getRank()).thenReturn(ClanRank.OWNER);
		when(settings.titleForRank(ClanRank.OWNER)).thenReturn(null);

		assertNull(resolver.resolveClanTitle(channel, "Zach"));
	}
}
