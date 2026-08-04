package tccrewplugin.lfg.ui;

import net.runelite.client.ui.ColorScheme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

public class LfgCategoryFilterPanel extends JPanel
{
	public interface ApplyHandler
	{
		void onApply(String visibleCategories);
	}

	private final JTextField allowListField = new JTextField();
	private final JLabel summaryLabel = new JLabel("Visible categories: all enabled");
	private final JButton applyButton = new JButton("Apply");

	public LfgCategoryFilterPanel(ApplyHandler handler)
	{
		setLayout(new BorderLayout(6, 6));
		setOpaque(true);
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		JPanel row = new JPanel();
		row.setOpaque(false);
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		summaryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.PLAIN, 11f));
		summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.add(summaryLabel);
		row.add(allowListField);
		add(row, BorderLayout.CENTER);

		LfgUiStyle.styleTextField(allowListField);

		LfgUiStyle.styleSecondaryButton(applyButton);
		applyButton.addActionListener(e -> {
			if (handler != null)
			{
				handler.onApply(allowListField.getText());
			}
		});
		add(applyButton, BorderLayout.EAST);
	}

	public void setCategories(List<?> categories, String allowList)
	{
		allowListField.setText(allowList == null ? "" : allowList);
		int count = categories == null ? 0 : categories.size();
		String summary = allowList == null || allowList.isBlank() ? "all enabled" : allowList;
		summaryLabel.setText("Visible categories: " + tccrewplugin.util.Utils.truncate(summary, 70) + " (" + count + " loaded)");
	}

	public void setBusy(boolean busy)
	{
		allowListField.setEnabled(!busy);
		applyButton.setEnabled(!busy);
	}
}
