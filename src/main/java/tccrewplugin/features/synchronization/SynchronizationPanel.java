package tccrewplugin.features.synchronization;

import tccrewplugin.sync.PlayerSyncService;
import tccrewplugin.sync.SyncStatusModel;
import tccrewplugin.ui.components.LabeledValue;
import tccrewplugin.util.TimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class SynchronizationPanel extends JPanel
{
	private final PlayerSyncService playerSyncService;
	private final LabeledValue status = new LabeledValue("Status");
	private final LabeledValue manifestVersion = new LabeledValue("Manifest");
	private final LabeledValue pending = new LabeledValue("Pending");
	private final LabeledValue lastAttempt = new LabeledValue("Last Attempt");
	private final LabeledValue nextRetry = new LabeledValue("Next Retry");

	public SynchronizationPanel(PlayerSyncService playerSyncService)
	{
		this.playerSyncService = playerSyncService;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Sync Status"));
		add(status);
		add(manifestVersion);
		add(pending);
		add(lastAttempt);
		add(nextRetry);
	}

	public void refresh()
	{
		SwingUtilities.invokeLater(() -> {
			SyncStatusModel model = playerSyncService.getStatus();
			status.setValue(model.getStatus().name());
			manifestVersion.setValue(model.getManifestVersion() == null ? "-" : String.valueOf(model.getManifestVersion()));
			pending.setValue(String.valueOf(model.getPendingFieldCount()));
			lastAttempt.setValue(TimeFormatter.formatInstant(model.getLastAttempt()));
			nextRetry.setValue(TimeFormatter.formatInstant(model.getNextRetryTime()));
		});
	}
}
