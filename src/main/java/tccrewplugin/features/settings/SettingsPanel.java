package tccrewplugin.features.settings;

import tccrewplugin.TangleDinkConfig;
import tccrewplugin.ui.components.LabeledValue;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class SettingsPanel extends JPanel
{
	private final TangleDinkConfig config;
	private final LabeledValue apiBaseUrl = new LabeledValue("API Base URL");
	private final LabeledValue syncInterval = new LabeledValue("Sync Interval");

	public SettingsPanel(TangleDinkConfig config)
	{
		this.config = config;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Plugin Settings"));
		add(apiBaseUrl);
		add(syncInterval);
	}

	public void refresh()
	{
		SwingUtilities.invokeLater(() -> {
			apiBaseUrl.setValue(config.apiBaseUrl());
			syncInterval.setValue(Integer.toString(config.syncIntervalSeconds()));
			// clan chat webhook controls are hidden for the initial PR
		});
	}
}
