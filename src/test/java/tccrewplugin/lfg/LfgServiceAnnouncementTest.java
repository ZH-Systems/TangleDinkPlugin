package tccrewplugin.lfg;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import tccrewplugin.DinkPluginConfig;
import tccrewplugin.lfg.model.LfgCategory;
import tccrewplugin.lfg.model.LfgGroup;
import tccrewplugin.lfg.model.LfgGroupStatus;
import tccrewplugin.lfg.model.LfgMember;
import tccrewplugin.lfg.model.LfgPermissions;
import tccrewplugin.lfg.model.LfgSource;
import tccrewplugin.sync.model.PlayerIdentity;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LfgServiceAnnouncementTest
{
	@Test
	void announcesNewExternalGroupsOnce()
	{
		ServiceFixture fixture = newService();

		LfgGroup group = buildGroup("group-1", 4, "Other Player", LfgSource.RUNELITE, LfgGroupStatus.OPEN, true);

		fixture.service.announceVisibleGroups(List.of(group), new PlayerIdentity("Zatch-Ary", "STANDARD"));
		fixture.service.announceVisibleGroups(List.of(group), new PlayerIdentity("Zatch-Ary", "STANDARD"));

		verify(fixture.chatMessageManager, times(1)).queue(any());
	}

	@Test
	void skipsGroupsOwnedByCurrentPlayer()
	{
		ServiceFixture fixture = newService();

		LfgGroup group = buildGroup("group-2", 1, "Zatch-Ary", LfgSource.DISCORD, LfgGroupStatus.OPEN, true);

		fixture.service.announceVisibleGroups(List.of(group), new PlayerIdentity("Zatch-Ary", "STANDARD"));

		verify(fixture.chatMessageManager, never()).queue(any());
	}

	private ServiceFixture newService()
	{
		Client client = mock(Client.class);
		ClientThread clientThread = mock(ClientThread.class);
		DinkPluginConfig config = mock(DinkPluginConfig.class);
		ConfigManager configManager = mock(ConfigManager.class);
		Gson gson = new Gson();
		ChatMessageManager chatMessageManager = mock(ChatMessageManager.class);
		ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		OkHttpClient httpClient = new OkHttpClient();

		when(config.lfgEnabled()).thenReturn(true);
		when(config.lfgShowChatMessages()).thenReturn(true);
		when(config.lfgRefreshIntervalSeconds()).thenReturn(20);
		when(config.lfgVisibleCategories()).thenReturn("");
		when(config.lfgShowFullGroups()).thenReturn(true);
		when(config.lfgShowDiscordGroups()).thenReturn(true);
		when(config.lfgShowRuneLiteGroups()).thenReturn(true);
		when(config.lfgSupabaseUrl()).thenReturn("https://example.com");
		when(config.lfgApiToken()).thenReturn("token");
		when(config.lfgMasterChannelWebhook()).thenReturn("");
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);

		LfgService service = new LfgService(client, clientThread, config, configManager, gson, chatMessageManager, executor, httpClient);
		service.startUp();
		service.onGameStateChanged(gameStateChanged(GameState.LOGGED_IN));
		return new ServiceFixture(service, chatMessageManager);
	}

	private GameStateChanged gameStateChanged(GameState state)
	{
		GameStateChanged event = mock(GameStateChanged.class);
		when(event.getGameState()).thenReturn(state);
		return event;
	}

	private LfgGroup buildGroup(String id, int version, String creatorName, LfgSource source, LfgGroupStatus status, boolean canJoin)
	{
		LfgCategory category = new LfgCategory("raid", "raid", "Raid", "Raids", true, 10);
		LfgMember creator = new LfgMember("creator", creatorName, null, source, Instant.now());
		LfgMember other = new LfgMember("member", "Other Member", null, source, Instant.now());
		return new LfgGroup(
			id,
			version,
			category,
			"Theatre of Blood",
			"Learner friendly",
			Instant.now(),
			5,
			status,
			source,
			creator,
			List.of(other),
			new LfgPermissions(canJoin, true, false),
			null,
			Instant.now(),
			Instant.now(),
			Instant.now().plusSeconds(3600)
		);
	}

	private static final class ServiceFixture
	{
		private final LfgService service;
		private final ChatMessageManager chatMessageManager;

		private ServiceFixture(LfgService service, ChatMessageManager chatMessageManager)
		{
			this.service = service;
			this.chatMessageManager = chatMessageManager;
		}
	}
}
