package tccrewplugin.lfg.ui;

import net.runelite.client.ui.ColorScheme;
import tccrewplugin.lfg.model.LfgCategory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Font;
import java.util.List;

public class LfgCreateGroupPanel extends JPanel
{
	public interface CreateHandler
	{
		void onCreate(String categoryKey, String activity, String description, boolean scheduleNow, String startTimeText, Integer maximumPlayers);
	}

	private final JComboBox<LfgCategory> categoryBox = new JComboBox<>();
	private final JTextField activityField = new JTextField();
	private final JTextField descriptionField = new JTextField();
	private final JComboBox<String> startModeBox = new JComboBox<>(new String[]{"Now", "Scheduled"});
	private final JTextField startTimeField = new JTextField();
	private final JTextField maximumPlayersField = new JTextField();
	private final JButton createButton = new JButton("Create Group");

	public LfgCreateGroupPanel(CreateHandler handler)
	{
		setLayout(new BorderLayout(4, 6));
		setOpaque(true);
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		JPanel form = new JPanel(new GridBagLayout());
		form.setOpaque(false);
		form.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		form.setAlignmentX(Component.LEFT_ALIGNMENT);
		form.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new java.awt.Insets(2, 0, 2, 6);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		addRow(form, gbc, "Category", categoryBox);
		addRow(form, gbc, "Activity", activityField);
		addRow(form, gbc, "Start", startModeBox);
		addRow(form, gbc, "Start Time", startTimeField);
		addRow(form, gbc, "Maximum Players", maximumPlayersField);
		addRow(form, gbc, "Description", descriptionField);

		startTimeField.setEnabled(false);
		startModeBox.addActionListener(e -> startTimeField.setEnabled("Scheduled".equals(startModeBox.getSelectedItem())));
		LfgUiStyle.styleTextField(activityField);
		LfgUiStyle.styleTextField(descriptionField);
		LfgUiStyle.styleTextField(startTimeField);
		LfgUiStyle.styleTextField(maximumPlayersField);
		LfgUiStyle.styleComboBox(categoryBox);
		LfgUiStyle.styleComboBox(startModeBox);

		LfgUiStyle.stylePrimaryButton(createButton);
		createButton.addActionListener(e -> {
			if (handler != null)
			{
				LfgCategory category = (LfgCategory) categoryBox.getSelectedItem();
				handler.onCreate(
					category == null ? "" : category.getKey(),
					activityField.getText(),
					descriptionField.getText(),
					"Now".equals(startModeBox.getSelectedItem()),
					startTimeField.getText(),
					parseMaximumPlayers(maximumPlayersField.getText())
				);
			}
		});

		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);
		footer.setAlignmentX(Component.LEFT_ALIGNMENT);
		footer.add(createButton, BorderLayout.CENTER);
		add(form, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);
	}

	public void setCategories(List<LfgCategory> categories)
	{
		categoryBox.removeAllItems();
		if (categories != null)
		{
			for (LfgCategory category : categories)
			{
				categoryBox.addItem(category);
			}
		}
	}

	public void setBusy(boolean busy)
	{
		createButton.setEnabled(!busy);
		categoryBox.setEnabled(!busy);
		activityField.setEnabled(!busy);
		descriptionField.setEnabled(!busy);
		startModeBox.setEnabled(!busy);
		maximumPlayersField.setEnabled(!busy);
		startTimeField.setEnabled(!busy && "Scheduled".equals(startModeBox.getSelectedItem()));
	}

	private void addRow(JPanel form, GridBagConstraints gbc, String label, java.awt.Component component)
	{
		JLabel rowLabel = new JLabel(label);
		rowLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		rowLabel.setFont(rowLabel.getFont().deriveFont(Font.BOLD, 11f));
		gbc.weightx = 0;
		gbc.gridx = 0;
		form.add(rowLabel, gbc);
		gbc.weightx = 1;
		gbc.gridx = 1;
		form.add(component, gbc);
		gbc.gridy++;
	}

	private Integer parseMaximumPlayers(String value)
	{
		String trimmed = value == null ? "" : value.trim();
		if (trimmed.isEmpty())
		{
			return null;
		}
		try
		{
			return Integer.parseInt(trimmed);
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}
}
