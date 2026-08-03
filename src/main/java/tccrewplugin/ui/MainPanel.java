package tccrewplugin.ui;

import net.runelite.client.ui.PluginPanel;
import tccrewplugin.PluginConstants;
import tccrewplugin.clanchat.ClanChatService;
import tccrewplugin.features.FeatureManager;
import tccrewplugin.sync.PlayerSyncService;
import tccrewplugin.sync.SyncStatusModel;
import tccrewplugin.ui.components.ErrorPanel;
import tccrewplugin.ui.components.LabeledValue;
import tccrewplugin.ui.components.StatusCard;
import tccrewplugin.util.TimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;

public class MainPanel extends PluginPanel
{
	private final PlayerSyncService playerSyncService;
	private final ClanChatService clanChatService;
	private final FeatureManager featureManager;
	private final FeatureNavigationPanel navigationPanel;
	private final FeatureContentPanel contentPanel = new FeatureContentPanel();
	private final ErrorPanel errorPanel = new ErrorPanel();
	private final StatusCard connectionState = new StatusCard("Connection");
	private final LabeledValue playerName = new LabeledValue("Player");
	private final LabeledValue profileType = new LabeledValue("Profile");
	private final LabeledValue clanName = new LabeledValue("Clan");
	private final LabeledValue lastSync = new LabeledValue("Last Player Sync");
	private final LabeledValue lastWebhook = new LabeledValue("Last Webhook");
	private final LabeledValue manifestVersion = new LabeledValue("Manifest");
	private final LabeledValue pendingChanges = new LabeledValue("Pending Changes");
	private final LabeledValue pendingQueue = new LabeledValue("Queue Size");

	public MainPanel(PlayerSyncService playerSyncService, ClanChatService clanChatService, FeatureManager featureManager)
	{
		this.playerSyncService = playerSyncService;
		this.clanChatService = clanChatService;
		this.featureManager = featureManager;
		this.navigationPanel = new FeatureNavigationPanel(featureManager, contentPanel::showFeature);
		for (var feature : featureManager.getFeatures())
		{
			contentPanel.register(feature.getId(), (javax.swing.JPanel) feature.getPanel());
		}
		setLayout(new BorderLayout(8, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		add(buildHeader(), BorderLayout.NORTH);
		add(buildCenter(), BorderLayout.CENTER);
		add(errorPanel, BorderLayout.SOUTH);
		refresh();
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel title = new JLabel(PluginConstants.PLUGIN_NAME);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		header.add(title);
		header.add(connectionState);
		header.add(playerName);
		header.add(profileType);
		header.add(clanName);
		header.add(lastSync);
		header.add(lastWebhook);
		header.add(manifestVersion);
		header.add(pendingChanges);
		header.add(pendingQueue);
		JPanel actions = new JPanel();
		JButton syncButton = new JButton("Manual Sync");
		syncButton.addActionListener(e -> playerSyncService.requestSync());
		JButton reloadButton = new JButton("Reload Manifest");
		reloadButton.addActionListener(e -> playerSyncService.reloadManifest());
		JButton testButton = new JButton("Test Webhook");
		testButton.addActionListener(e -> clanChatService.enqueueTestWebhook());
		actions.add(syncButton);
		actions.add(reloadButton);
		actions.add(testButton);
		header.add(actions);
		return header;
	}

	private JPanel buildCenter()
	{
		JPanel center = new JPanel(new BorderLayout(8, 8));
		center.add(navigationPanel, BorderLayout.WEST);
		center.add(contentPanel, BorderLayout.CENTER);
		return center;
	}

	public void refresh()
	{
		SwingUtilities.invokeLater(() -> {
			SyncStatusModel status = playerSyncService.getStatus();
			connectionState.setValue(status.getStatus().name());
			playerName.setValue(playerSyncService.getCurrentUsername());
			profileType.setValue(playerSyncService.getCurrentProfileType());
			clanName.setValue(clanChatService.getCurrentClanName());
			lastSync.setValue(TimeFormatter.formatInstant(playerSyncService.getLastSuccessfulSync()));
			lastWebhook.setValue(TimeFormatter.formatInstant(clanChatService.getLastSuccessfulDelivery()));
			manifestVersion.setValue(playerSyncService.getManifestVersionValue() == null ? "-" : String.valueOf(playerSyncService.getManifestVersionValue()));
			pendingChanges.setValue(String.valueOf(status.getPendingFieldCount()));
			pendingQueue.setValue(String.valueOf(clanChatService.getQueueSize()));
			StringBuilder errors = new StringBuilder();
			if (status.getSanitizedError() != null && !status.getSanitizedError().isEmpty())
			{
				errors.append(status.getSanitizedError());
			}
			String clanError = clanChatService.getLastError();
			if (clanError != null && !clanError.isEmpty())
			{
				if (errors.length() > 0)
				{
					errors.append('\n');
				}
				errors.append(clanError);
			}
			errorPanel.setMessage(errors.toString());
			navigationPanel.refresh();
		});
	}
}
