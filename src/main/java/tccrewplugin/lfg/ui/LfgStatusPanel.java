package tccrewplugin.lfg.ui;

import net.runelite.client.ui.ColorScheme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

public class LfgStatusPanel extends JPanel
{
	private final JLabel playerLabel = new JLabel("Player: -");
	private final JLabel currentEventLabel = new JLabel("Current Event: -");
	private final JLabel statusLabel = new JLabel("Status: -");
	private final JLabel errorLabel = new JLabel("");

	public LfgStatusPanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		playerLabel.setForeground(Color.WHITE);
		playerLabel.setFont(playerLabel.getFont().deriveFont(Font.BOLD, 12f));
		playerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		currentEventLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		currentEventLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		currentEventLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		errorLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, 11f));
		errorLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		add(playerLabel);
		add(currentEventLabel);
		add(statusLabel);
		add(errorLabel);
	}

	public void setPlayer(String player)
	{
		playerLabel.setText("Player: " + (player == null || player.isBlank() ? "-" : player));
	}

	public void setStatus(String status)
	{
		statusLabel.setText("Status: " + (status == null || status.isBlank() ? "-" : status));
	}

	public void setCurrentEvent(String currentEvent)
	{
		currentEventLabel.setText("Current Event: " + (currentEvent == null || currentEvent.isBlank() ? "-" : currentEvent));
		currentEventLabel.setToolTipText(currentEvent == null || currentEvent.isBlank() || "-".equals(currentEvent) ? null : currentEvent);
	}

	public void setError(String error)
	{
		errorLabel.setText(error == null ? "" : error);
	}
}
