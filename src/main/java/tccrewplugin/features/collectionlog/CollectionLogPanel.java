package tccrewplugin.features.collectionlog;

import tccrewplugin.collectionlog.CollectionLogService;
import tccrewplugin.sync.model.CollectionLogPayload;
import tccrewplugin.ui.components.LabeledValue;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class CollectionLogPanel extends JPanel
{
	private final CollectionLogService collectionLogService;
	private final LabeledValue mappingVersion = new LabeledValue("Mapping Version");
	private final LabeledValue owned = new LabeledValue("Owned");
	private final LabeledValue total = new LabeledValue("Total");

	public CollectionLogPanel(CollectionLogService collectionLogService)
	{
		this.collectionLogService = collectionLogService;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Collection Log Sync"));
		add(mappingVersion);
		add(owned);
		add(total);
		JButton begin = new JButton("Sync Collection Log");
		begin.addActionListener(e -> collectionLogService.beginCapture());
		JButton stop = new JButton("Stop Capture");
		stop.addActionListener(e -> collectionLogService.stopCapture());
		add(begin);
		add(stop);
	}

	public void refresh()
	{
		SwingUtilities.invokeLater(() -> {
			CollectionLogPayload payload = collectionLogService.currentPayload();
			if (payload == null)
			{
				mappingVersion.setValue("-");
				owned.setValue("-");
				total.setValue("-");
				return;
			}
			mappingVersion.setValue(String.valueOf(payload.getMappingVersion()));
			owned.setValue(String.valueOf(payload.getOwnedCount()));
			total.setValue(String.valueOf(payload.getItemCount()));
		});
	}
}
