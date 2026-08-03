package tccrewplugin.features.account;

import tccrewplugin.TangleDinkConfig;
import tccrewplugin.clanchat.ClanChatService;
import tccrewplugin.features.FeatureCategory;
import tccrewplugin.features.PluginFeature;
import tccrewplugin.sync.PlayerSyncService;

import javax.swing.JComponent;

public class AccountFeature implements PluginFeature
{
	private final TangleDinkConfig config;
	private final AccountPanel panel;

	public AccountFeature(TangleDinkConfig config, PlayerSyncService playerSyncService, ClanChatService clanChatService)
	{
		this.config = config;
		this.panel = new AccountPanel(playerSyncService, clanChatService);
	}

	@Override
	public String getId()
	{
		return "account";
	}

	@Override
	public String getDisplayName()
	{
		return "Player Overview";
	}

	@Override
	public String getDescription()
	{
		return "Current player and profile details";
	}

	@Override
	public FeatureCategory getCategory()
	{
		return FeatureCategory.ACCOUNT;
	}

	@Override
	public JComponent getPanel()
	{
		return panel;
	}

	@Override
	public boolean isEnabled()
	{
		return config.enableAccountFeature();
	}

	@Override
	public void startUp()
	{
		panel.refresh();
	}

	@Override
	public void shutDown()
	{
	}
}
