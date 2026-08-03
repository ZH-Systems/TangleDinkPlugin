package tccrewplugin.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class StatusCard extends JPanel
{
	private final JLabel value = new JLabel("-");

	public StatusCard(String title)
	{
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createEtchedBorder(),
			BorderFactory.createEmptyBorder(6, 8, 6, 8)
		));
		add(new JLabel(title), BorderLayout.NORTH);
		add(value, BorderLayout.CENTER);
	}

	public void setValue(String text)
	{
		value.setText(text == null || text.isEmpty() ? "-" : text);
	}
}
