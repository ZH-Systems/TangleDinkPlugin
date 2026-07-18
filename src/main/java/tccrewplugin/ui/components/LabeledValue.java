package tccrewplugin.ui.components;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class LabeledValue extends JPanel
{
	private final JLabel valueLabel = new JLabel("-");

	public LabeledValue(String label)
	{
		setLayout(new BorderLayout(8, 0));
		setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		JLabel nameLabel = new JLabel(label);
		JPanel inner = new JPanel();
		inner.setLayout(new BoxLayout(inner, BoxLayout.X_AXIS));
		inner.add(nameLabel);
		inner.add(javax.swing.Box.createHorizontalGlue());
		inner.add(valueLabel);
		add(inner, BorderLayout.CENTER);
	}

	public void setValue(String value)
	{
		valueLabel.setText(value == null || value.isEmpty() ? "-" : value);
	}
}
