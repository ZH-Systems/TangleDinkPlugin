package tccrewplugin.features.clanchat;

import tccrewplugin.TangleDinkConfig;
import tccrewplugin.clanchat.ClanChatService;
import tccrewplugin.features.FeatureCategory;
import tccrewplugin.features.PluginFeature;

import javax.swing.JComponent;

public class ClanChatFeature implements PluginFeature
{
	private final TangleDinkConfig config;
	private final ClanChatPanel panel;

	public ClanChatFeature(TangleDinkConfig config, ClanChatService clanChatService)
	{
		this.config = config;
		this.panel = new ClanChatPanel(config, clanChatService);
	}

	@Override
	public String getId()
	{
		return "clan";
	}

	@Override
	public String getDisplayName()
	{
		return "Clan Chat Webhook";
	}

	@Override
	public String getDescription()
	{
		return "Clan chat webhook forwarding";
	}

	@Override
	public FeatureCategory getCategory()
	{
		return FeatureCategory.CLAN;
	}

	@Override
	public JComponent getPanel()
	{
		return panel;
	}

	@Override
	public boolean isEnabled()
	{
		return config.enableClanChatFeature();
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
