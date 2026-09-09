package tccrewplugin.features.account;

import tccrewplugin.sync.PlayerSyncService;
import tccrewplugin.ui.components.LabeledValue;
import tccrewplugin.util.TimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class AccountPanel extends JPanel
{
	private final PlayerSyncService playerSyncService;
	private final LabeledValue username = new LabeledValue("Player");
	private final LabeledValue profile = new LabeledValue("Profile");
	private final LabeledValue lastSync = new LabeledValue("Last Sync");

	public AccountPanel(PlayerSyncService playerSyncService)
	{
		this.playerSyncService = playerSyncService;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Player Overview"));
		add(username);
		add(profile);
		add(lastSync);
	}

	public void refresh()
	{
		SwingUtilities.invokeLater(() -> {
			username.setValue(playerSyncService.getCurrentUsername());
			profile.setValue(playerSyncService.getCurrentProfileType());
			lastSync.setValue(TimeFormatter.formatInstant(playerSyncService.getLastSuccessfulSync()));
		});
	}
}
