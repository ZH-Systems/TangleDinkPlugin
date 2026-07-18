package tccrewplugin.features;

import tccrewplugin.TangleDinkConfig;
import tccrewplugin.sync.PlayerSyncService;
import tccrewplugin.clanchat.ClanChatService;
import tccrewplugin.collectionlog.CollectionLogService;
import tccrewplugin.ui.MainPanel;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

public class FeatureContext
{
	private final Client client;
	private final ClientThread clientThread;
	private final TangleDinkConfig config;
	private final PlayerSyncService playerSyncService;
	private final ClanChatService clanChatService;
	private final CollectionLogService collectionLogService;
	private final MainPanel mainPanel;

	public FeatureContext(
		Client client,
		ClientThread clientThread,
		TangleDinkConfig config,
		PlayerSyncService playerSyncService,
		ClanChatService clanChatService,
		CollectionLogService collectionLogService,
		MainPanel mainPanel
	)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.playerSyncService = playerSyncService;
		this.clanChatService = clanChatService;
		this.collectionLogService = collectionLogService;
		this.mainPanel = mainPanel;
	}

	public Client getClient()
	{
		return client;
	}

	public ClientThread getClientThread()
	{
		return clientThread;
	}

	public TangleDinkConfig getConfig()
	{
		return config;
	}

	public PlayerSyncService getPlayerSyncService()
	{
		return playerSyncService;
	}

	public ClanChatService getClanChatService()
	{
		return clanChatService;
	}

	public CollectionLogService getCollectionLogService()
	{
		return collectionLogService;
	}

	public MainPanel getMainPanel()
	{
		return mainPanel;
	}
}
