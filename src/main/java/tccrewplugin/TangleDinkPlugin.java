package tccrewplugin;

import com.google.inject.Provides;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.ScriptEvent;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import tccrewplugin.collectionlog.CollectionLogConstants;
import tccrewplugin.collectionlog.CollectionLogService;
import tccrewplugin.clanchat.ClanChatService;
import tccrewplugin.features.FeatureManager;
import tccrewplugin.sync.PlayerSyncService;
import tccrewplugin.ui.MainPanel;
import tccrewplugin.ui.SidebarNavigationManager;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
	name = PluginConstants.PLUGIN_NAME,
	description = "Player sync and clan webhook plugin",
	tags = { "sync", "clan", "webhook", "collection log" }
)
public class TangleDinkPlugin extends Plugin
{
	@Inject
	private FeatureManager featureManager;
	@Inject
	private PlayerSyncService playerSyncService;
	@Inject
	private CollectionLogService collectionLogService;
	@Inject
	private ClanChatService clanChatService;
	@Inject
	private MainPanel mainPanel;
	@Inject
	private SidebarNavigationManager sidebarNavigationManager;

	@Provides
	TangleDinkConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TangleDinkConfig.class);
	}

	@Override
	protected void startUp()
	{
		clanChatService.startUp();
		playerSyncService.startUp();
		featureManager.startUp();
		sidebarNavigationManager.addNavigationButton();
		mainPanel.refresh();
	}

	@Override
	protected void shutDown()
	{
		sidebarNavigationManager.removeNavigationButton();
		collectionLogService.stopCapture();
		clanChatService.shutDown();
		playerSyncService.shutDown();
		featureManager.shutDown();
	}

	@Subscribe
	public void onConfigChanged(net.runelite.client.events.ConfigChanged event)
	{
		if (!PluginConstants.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		playerSyncService.onConfigChanged(event.getKey());
		clanChatService.onConfigChanged(event.getKey());
		featureManager.refresh();
		mainPanel.refresh();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		playerSyncService.onGameStateChanged(gameState);
		clanChatService.onGameStateChanged(gameState);
		collectionLogService.resetForAccountChange();
		mainPanel.refresh();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (mainPanel.isShowing())
		{
			mainPanel.refresh();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String typeName = event.getType() == null ? null : event.getType().name();
		String sender = event.getName();
		String senderRank = event.getSender();
		String message = event.getMessage();
		boolean guest = typeName != null && typeName.toUpperCase().contains("GUEST");
		boolean plausible = typeName != null && (typeName.toUpperCase().contains("CLAN") || typeName.toUpperCase().contains("BROADCAST") || typeName.toUpperCase().contains("GUEST"));
		if (plausible)
		{
			clanChatService.onChatMessage(typeName, sender, senderRank, message, guest);
		}
	}

	@Subscribe
	public void onClanChannelChanged(net.runelite.api.events.ClanChannelChanged event)
	{
		if (event.getClanChannel() != null)
		{
			clanChatService.onClanChannelChanged(event.getClanChannel());
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		ScriptEvent scriptEvent = event.getScriptEvent();
		if (event.getScriptId() == CollectionLogConstants.SCRIPT_COLLECTION_LOG && scriptEvent != null)
		{
			collectionLogService.ingestScriptArguments(scriptEvent.getArguments());
		}
	}
}
