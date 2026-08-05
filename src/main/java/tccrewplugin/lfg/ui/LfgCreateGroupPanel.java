package tccrewplugin.lfg.ui;

import net.runelite.client.ui.ColorScheme;
import tccrewplugin.lfg.model.LfgActivity;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class LfgCreateGroupPanel extends JPanel
{
	public interface CreateHandler
	{
		void onCreate(String categoryKey, String activity, String description, Instant startTime, Integer maximumPlayers);
	}

	private final JComboBox<LfgCategory> categoryBox = new JComboBox<>();
	private final JComboBox<LfgActivity> activityBox = new JComboBox<>();
	private final JTextField descriptionField = new JTextField();
	private final JComboBox<StartTimeOption> startTimeBox = new JComboBox<>(StartTimeOption.values());
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
		addRow(form, gbc, "Activity", activityBox);
		addRow(form, gbc, "Start Time", startTimeBox);
		addRow(form, gbc, "Maximum Players", maximumPlayersField);
		addRow(form, gbc, "Description", descriptionField);

		LfgUiStyle.styleTextField(descriptionField);
		LfgUiStyle.styleTextField(maximumPlayersField);
		LfgUiStyle.styleComboBox(categoryBox);
		LfgUiStyle.styleComboBox(activityBox);
		LfgUiStyle.styleComboBox(startTimeBox);

		categoryBox.addActionListener(e -> refreshActivities());
		activityBox.addActionListener(e -> applySelectedActivityDefaults());

		LfgUiStyle.stylePrimaryButton(createButton);
		createButton.addActionListener(e -> {
			if (handler != null)
			{
				LfgCategory category = (LfgCategory) categoryBox.getSelectedItem();
				LfgActivity activity = (LfgActivity) activityBox.getSelectedItem();
				StartTimeOption startTimeOption = (StartTimeOption) startTimeBox.getSelectedItem();
				handler.onCreate(
					category == null ? "" : category.getKey(),
					activity == null ? "" : activity.getKey(),
					descriptionField.getText(),
					startTimeOption == null ? null : startTimeOption.toInstant(),
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
		refreshActivities();
	}

	public void setBusy(boolean busy)
	{
		createButton.setEnabled(!busy);
		categoryBox.setEnabled(!busy);
		activityBox.setEnabled(!busy);
		descriptionField.setEnabled(!busy);
		startTimeBox.setEnabled(!busy);
		maximumPlayersField.setEnabled(!busy);
	}

	private void refreshActivities()
	{
		LfgActivity current = (LfgActivity) activityBox.getSelectedItem();
		String currentKey = current == null ? "" : String.valueOf(current.getKey());
		activityBox.removeAllItems();
		LfgCategory category = (LfgCategory) categoryBox.getSelectedItem();
		if (category != null && category.getActivities() != null)
		{
			for (LfgActivity activity : category.getActivities())
			{
				if (activity != null && activity.isEnabled())
				{
					activityBox.addItem(activity);
				}
			}
		}
		for (int i = 0; i < activityBox.getItemCount(); i++)
		{
			LfgActivity item = activityBox.getItemAt(i);
			if (item != null && currentKey.equalsIgnoreCase(String.valueOf(item.getKey())))
			{
				activityBox.setSelectedIndex(i);
				applySelectedActivityDefaults();
				return;
			}
		}
		if (activityBox.getItemCount() > 0)
		{
			activityBox.setSelectedIndex(0);
		}
		else
		{
			maximumPlayersField.setText("");
		}
		applySelectedActivityDefaults();
	}

	private void applySelectedActivityDefaults()
	{
		LfgActivity activity = (LfgActivity) activityBox.getSelectedItem();
		if (activity == null)
		{
			maximumPlayersField.setText("");
			return;
		}
		Integer maximumPlayers = activity.getMaximumPlayers();
		if (maximumPlayers != null)
		{
			maximumPlayersField.setText(String.valueOf(maximumPlayers));
		}
		else if (activity.isSupportsMass())
		{
			maximumPlayersField.setText("");
		}
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

	private enum StartTimeOption
	{
		NOW("Now", null),
		IN_30_MINUTES("In 30 minutes", Duration.ofMinutes(30)),
		IN_1_HOUR("In 1 hour", Duration.ofHours(1)),
		IN_2_HOURS("In 2 hours", Duration.ofHours(2)),
		IN_4_HOURS("In 4 hours", Duration.ofHours(4)),
		IN_8_HOURS("In 8 hours", Duration.ofHours(8));

		private final String label;
		private final Duration offset;

		StartTimeOption(String label, Duration offset)
		{
			this.label = label;
			this.offset = offset;
		}

		Instant toInstant()
		{
			return offset == null ? null : Instant.now().plus(offset);
		}

		@Override
		public String toString()
		{
			return label;
		}
	}
}
