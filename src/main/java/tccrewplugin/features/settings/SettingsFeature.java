package tccrewplugin.features.settings;

import tccrewplugin.TangleCrewSyncConfig;
import tccrewplugin.features.FeatureCategory;
import tccrewplugin.features.PluginFeature;

import javax.inject.Inject;
import javax.swing.JComponent;

public class SettingsFeature implements PluginFeature
{
	private final TangleCrewSyncConfig config;
	private final SettingsPanel panel;

	@Inject
	public SettingsFeature(TangleCrewSyncConfig config)
	{
		this.config = config;
		this.panel = new SettingsPanel(config);
	}

	@Override
	public String getId()
	{
		return "settings";
	}

	@Override
	public String getDisplayName()
	{
		return "Plugin Settings";
	}

	@Override
	public String getDescription()
	{
		return "Plugin configuration summary";
	}

	@Override
	public FeatureCategory getCategory()
	{
		return FeatureCategory.SETTINGS;
	}

	@Override
	public JComponent getPanel()
	{
		return panel;
	}

	@Override
	public boolean isEnabled()
	{
		return config.enableSettingsFeature();
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
