package tccrewplugin.features.synchronization;

import tccrewplugin.TangleDinkConfig;
import tccrewplugin.features.FeatureCategory;
import tccrewplugin.features.PluginFeature;
import tccrewplugin.sync.PlayerSyncService;

import javax.inject.Inject;
import javax.swing.JComponent;

public class SynchronizationFeature implements PluginFeature
{
	private final TangleDinkConfig config;
	private final SynchronizationPanel panel;

	@Inject
	public SynchronizationFeature(TangleDinkConfig config, PlayerSyncService playerSyncService)
	{
		this.config = config;
		this.panel = new SynchronizationPanel(playerSyncService);
	}

	@Override
	public String getId()
	{
		return "sync";
	}

	@Override
	public String getDisplayName()
	{
		return "Sync Status";
	}

	@Override
	public String getDescription()
	{
		return "Player synchronization status and controls";
	}

	@Override
	public FeatureCategory getCategory()
	{
		return FeatureCategory.SYNCHRONIZATION;
	}

	@Override
	public JComponent getPanel()
	{
		return panel;
	}

	@Override
	public boolean isEnabled()
	{
		return config.enableSyncFeature();
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
