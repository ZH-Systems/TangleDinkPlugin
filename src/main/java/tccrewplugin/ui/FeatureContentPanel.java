package tccrewplugin.ui;

import java.awt.CardLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.Map;

public class FeatureContentPanel extends JPanel
{
	private final CardLayout cardLayout = new CardLayout();
	private final Map<String, JPanel> panels = new LinkedHashMap<>();
	private final JPanel emptyPanel = new JPanel(new BorderLayout());

	public FeatureContentPanel()
	{
		setLayout(cardLayout);
		emptyPanel.add(new JLabel("Select a feature"), BorderLayout.CENTER);
		add(emptyPanel, "__empty__");
		cardLayout.show(this, "__empty__");
	}

	public void register(String id, JPanel panel)
	{
		if (id == null || panel == null)
		{
			return;
		}
		panels.put(id, panel);
		add(panel, id);
	}

	public void showFeature(String id)
	{
		if (id == null || !panels.containsKey(id))
		{
			cardLayout.show(this, "__empty__");
			return;
		}
		cardLayout.show(this, id);
	}
}
