package tccrewplugin.clanchat;

import tccrewplugin.DinkPluginConfig;
import net.runelite.api.ChatMessageType;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.clan.ClanID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClanChatWebhookManagerTest
{
	private net.runelite.api.Client client;
	private DinkPluginConfig config;
	private ClanWebhookService service;
	private ClanMemberTitleResolver titleResolver;
	private ClanChatWebhookManager manager;
	private ClanChannel clanChannel;

	@BeforeEach
	void setUp()
	{
		client = mock(net.runelite.api.Client.class);
		clanChannel = mock(ClanChannel.class);
		when(client.getClanChannel()).thenReturn(clanChannel);

		config = mock(DinkPluginConfig.class);
		when(config.sendNormalChat()).thenReturn(true);
		when(config.sendSystemBroadcasts()).thenReturn(true);
		when(config.sendUnknownBroadcasts()).thenReturn(true);
		when(config.sendLoginGuidance()).thenReturn(false);
		when(config.includeClientMetadata()).thenReturn(false);
		when(config.clanName()).thenReturn("");

		service = mock(ClanWebhookService.class);
		when(service.submit(any())).thenReturn(true);

		titleResolver = mock(ClanMemberTitleResolver.class);
		manager = new ClanChatWebhookManager(client, config, service, titleResolver);
	}

	@Test
	void blankClanFilterAllowsCurrentClan()
	{
		when(clanChannel.getName()).thenReturn("My Clan");
		ChatMessage message = new ChatMessage(null, ChatMessageType.CLAN_CHAT, "<img=2>Zach", "Hello", null, 123);

		manager.onChatMessage(message);

		ArgumentCaptor<ClanMessageEvent> captor = ArgumentCaptor.forClass(ClanMessageEvent.class);
		verify(service).submit(captor.capture());
		ClanMessageEvent event = captor.getValue();
		assertNotNull(event);
		assertEquals("Zach", event.getAuthor());
		assertEquals(SystemMessageType.NORMAL, event.getSystemMessageType());
		verify(titleResolver).resolveClanTitle(clanChannel, "Zach");
	}

	@Test
	void clanNameMismatchBlocksSending()
	{
		when(clanChannel.getName()).thenReturn("My Clan");
		when(config.clanName()).thenReturn("Other Clan");
		ChatMessage message = new ChatMessage(null, ChatMessageType.CLAN_CHAT, "Zach", "Hello", null, 123);

		manager.onChatMessage(message);

		verify(service, never()).submit(any());
	}

	@Test
	void nullClanChannelBlocksSendingSafely()
	{
		when(client.getClanChannel()).thenReturn(null);
		ChatMessage message = new ChatMessage(null, ChatMessageType.CLAN_CHAT, "Zach", "Hello", null, 123);

		manager.onChatMessage(message);

		verify(service, never()).submit(any());
	}

	@Test
	void systemBroadcastClassificationIsPreserved()
	{
		when(clanChannel.getName()).thenReturn("My Clan");
		ChatMessage message = new ChatMessage(null, ChatMessageType.CLAN_MESSAGE, null, "received a new collection log item: Dragon defender", null, 123);

		manager.onChatMessage(message);

		ArgumentCaptor<ClanMessageEvent> captor = ArgumentCaptor.forClass(ClanMessageEvent.class);
		verify(service).submit(captor.capture());
		ClanMessageEvent event = captor.getValue();
		assertEquals(SystemMessageType.COLLECTION_LOG, event.getSystemMessageType());
		assertEquals(null, event.getClanTitle());
	}

	@Test
	void clanChannelChangeDoesNotThrow()
	{
		manager.onClanChannelChanged(new ClanChannelChanged(clanChannel, ClanID.CLAN, false));
		manager.onClanChannelChanged(new ClanChannelChanged(null, ClanID.CLAN, false));
	}
}
