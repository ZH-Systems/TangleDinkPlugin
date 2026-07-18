package tccrewplugin.features.clanchat;

import tccrewplugin.TangleDinkConfig;
import tccrewplugin.clanchat.ClanChatService;
import tccrewplugin.ui.components.LabeledValue;
import tccrewplugin.util.TimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.net.URI;
import java.net.URISyntaxException;

public class ClanChatPanel extends JPanel
{
	private final TangleDinkConfig config;
	private final ClanChatService clanChatService;
	private final LabeledValue enabled = new LabeledValue("Enabled");
	private final LabeledValue clanName = new LabeledValue("Clan");
	private final LabeledValue endpoint = new LabeledValue("Endpoint");
	private final LabeledValue secret = new LabeledValue("Secret");
	private final LabeledValue queue = new LabeledValue("Queue");
	private final LabeledValue lastAttempt = new LabeledValue("Last Attempt");
	private final LabeledValue lastSuccess = new LabeledValue("Last Success");

	public ClanChatPanel(TangleDinkConfig config, ClanChatService clanChatService)
	{
		this.config = config;
		this.clanChatService = clanChatService;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Clan Chat Webhook"));
		add(enabled);
		add(clanName);
		add(endpoint);
		add(secret);
		add(queue);
		add(lastAttempt);
		add(lastSuccess);
		JButton test = new JButton("Test Webhook");
		test.addActionListener(e -> clanChatService.enqueueTestWebhook());
		JButton clear = new JButton("Clear Local History");
		clear.addActionListener(e -> clanChatService.clearLocalHistory());
		JButton pause = new JButton("Pause Deliveries");
		pause.addActionListener(e -> clanChatService.pauseDeliveries());
		JButton resume = new JButton("Resume Deliveries");
		resume.addActionListener(e -> clanChatService.resumeDeliveries());
		add(test);
		add(clear);
		add(pause);
		add(resume);
	}

	public void refresh()
	{
		SwingUtilities.invokeLater(() -> {
			enabled.setValue(Boolean.toString(config.clanWebhookEnabled()));
			clanName.setValue(clanChatService.getCurrentClanName());
			endpoint.setValue(endpointHost(config.clanWebhookEndpoint()));
			secret.setValue(Boolean.toString(!config.clanWebhookSecret().isBlank()));
			queue.setValue(clanChatService.getQueueSize() + " / " + clanChatService.getQueueCapacity());
			lastAttempt.setValue(TimeFormatter.formatInstant(clanChatService.getLastDeliveryAttempt()));
			lastSuccess.setValue(TimeFormatter.formatInstant(clanChatService.getLastSuccessfulDelivery()));
		});
	}

	private String endpointHost(String value)
	{
		try
		{
			URI uri = new URI(value);
			return uri.getHost() == null ? "-" : uri.getHost();
		}
		catch (URISyntaxException ex)
		{
			return "-";
		}
	}
}
