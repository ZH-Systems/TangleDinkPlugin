package tccrewplugin.features.account;

import tccrewplugin.clanchat.ClanChatService;
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
	private final ClanChatService clanChatService;
	private final LabeledValue username = new LabeledValue("Player");
	private final LabeledValue profile = new LabeledValue("Profile");
	private final LabeledValue clan = new LabeledValue("Clan");
	private final LabeledValue lastSync = new LabeledValue("Last Sync");

	public AccountPanel(PlayerSyncService playerSyncService, ClanChatService clanChatService)
	{
		this.playerSyncService = playerSyncService;
		this.clanChatService = clanChatService;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Player Overview"));
		add(username);
		add(profile);
		add(clan);
		add(lastSync);
	}

	public void refresh()
	{
		SwingUtilities.invokeLater(() -> {
			username.setValue(playerSyncService.getCurrentUsername());
			profile.setValue(playerSyncService.getCurrentProfileType());
			clan.setValue(clanChatService.getCurrentClanName());
			lastSync.setValue(TimeFormatter.formatInstant(playerSyncService.getLastSuccessfulSync()));
		});
	}
}
