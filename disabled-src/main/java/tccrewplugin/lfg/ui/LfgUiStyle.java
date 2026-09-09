package tccrewplugin.lfg.ui;

import net.runelite.client.ui.ColorScheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

public final class LfgUiStyle
{
	private LfgUiStyle()
	{
	}

	public static void styleButton(JButton button)
	{
		styleSecondaryButton(button);
	}

	public static void styleSecondaryButton(JButton button)
	{
		if (button == null)
		{
			return;
		}
		button.setFocusable(false);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			BorderFactory.createEmptyBorder(4, 8, 4, 8)
		));
	}

	public static void stylePrimaryButton(JButton button)
	{
		if (button == null)
		{
			return;
		}
		button.setFocusable(false);
		button.setBackground(new Color(70, 120, 64));
		button.setForeground(Color.WHITE);
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(54, 92, 49)),
			BorderFactory.createEmptyBorder(4, 10, 4, 10)
		));
	}

	public static void styleTextField(JTextField field)
	{
		if (field == null)
		{
			return;
		}
		field.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		field.setForeground(Color.WHITE);
		field.setCaretColor(Color.WHITE);
		field.setOpaque(true);
		field.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)
		));
	}

	public static void styleComboBox(JComboBox<?> comboBox)
	{
		if (comboBox == null)
		{
			return;
		}
		comboBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		comboBox.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		comboBox.setOpaque(true);
		comboBox.setFocusable(false);
		comboBox.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
	}

	public static void styleScrollPane(JScrollPane scrollPane)
	{
		if (scrollPane == null)
		{
			return;
		}
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(12);
		scrollPane.getVerticalScrollBar().setOpaque(false);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
		scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI()
		{
			@Override
			protected void configureScrollBarColors()
			{
				thumbColor = ColorScheme.MEDIUM_GRAY_COLOR;
				trackColor = ColorScheme.DARK_GRAY_COLOR;
			}

			@Override
			protected JButton createDecreaseButton(int orientation)
			{
				return createZeroButton();
			}

			@Override
			protected JButton createIncreaseButton(int orientation)
			{
				return createZeroButton();
			}

			private JButton createZeroButton()
			{
				JButton button = new JButton();
				button.setPreferredSize(new Dimension(0, 0));
				button.setMinimumSize(new Dimension(0, 0));
				button.setMaximumSize(new Dimension(0, 0));
				return button;
			}

			@Override
			protected void paintTrack(Graphics g, javax.swing.JComponent c, Rectangle trackBounds)
			{
				g.setColor(trackColor);
				g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
			}

			@Override
			protected void paintThumb(Graphics g, javax.swing.JComponent c, Rectangle thumbBounds)
			{
				if (thumbBounds.isEmpty() || !scrollbar.isEnabled())
				{
					return;
				}

				g.setColor(thumbColor);
				g.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 6, 6);
			}
		});
	}
}
