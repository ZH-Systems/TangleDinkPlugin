package tccrewplugin.lfg.ui;

import net.runelite.client.ui.ColorScheme;
import tccrewplugin.lfg.model.LfgGroup;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

public class LfgGroupListPanel extends JPanel
{
	private final JPanel list = new JPanel();
	private final JLabel empty = new JLabel("No active groups");

	public LfgGroupListPanel()
	{
		setLayout(new BorderLayout());
		setOpaque(true);
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setOpaque(false);
		list.setAlignmentX(Component.LEFT_ALIGNMENT);
		list.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		empty.setFont(empty.getFont().deriveFont(Font.ITALIC, 11f));
		empty.setAlignmentX(Component.LEFT_ALIGNMENT);

		add(list, BorderLayout.NORTH);
	}

	public void setGroups(List<LfgGroup> groups, String currentPlayerId, LfgGroupCard.ActionHandler handler)
	{
		list.removeAll();
		if (groups == null || groups.isEmpty())
		{
			list.add(empty);
		}
		else
		{
			for (int i = 0; i < groups.size(); i++)
			{
				LfgGroup group = groups.get(i);
				LfgGroupCard card = new LfgGroupCard();
				card.setGroup(group, currentPlayerId, handler);
				list.add(card);
				if (i + 1 < groups.size())
				{
					list.add(Box.createVerticalStrut(6));
				}
			}
		}
		revalidate();
		repaint();
	}
}
