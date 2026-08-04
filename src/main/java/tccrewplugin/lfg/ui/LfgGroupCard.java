package tccrewplugin.lfg.ui;

import net.runelite.client.ui.ColorScheme;
import org.apache.commons.lang3.StringUtils;
import tccrewplugin.lfg.model.LfgGroup;
import tccrewplugin.lfg.model.LfgMember;
import tccrewplugin.util.Utils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LfgGroupCard extends JPanel
{
	public interface ActionHandler
	{
		void onJoin(LfgGroup group);

		void onLeave(LfgGroup group);

		void onClose(LfgGroup group);
	}

	private final JLabel title = new JLabel();
	private final JLabel meta = new JLabel();
	private final JLabel details = new JLabel();
	private final JLabel members = new JLabel();
	private final JLabel description = new JLabel();
	private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

	public LfgGroupCard()
	{
		setLayout(new BorderLayout(4, 4));
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
			BorderFactory.createEmptyBorder(8, 10, 8, 10)
		));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setAlignmentX(LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		JPanel content = new JPanel();
		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setAlignmentX(LEFT_ALIGNMENT);
		content.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
		meta.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		details.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		members.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		description.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		title.setAlignmentX(LEFT_ALIGNMENT);
		meta.setAlignmentX(LEFT_ALIGNMENT);
		details.setAlignmentX(LEFT_ALIGNMENT);
		members.setAlignmentX(LEFT_ALIGNMENT);
		description.setAlignmentX(LEFT_ALIGNMENT);

		content.add(title);
		content.add(meta);
		content.add(details);
		content.add(members);
		content.add(description);
		add(content, BorderLayout.CENTER);

		actions.setOpaque(false);
		actions.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		actions.setAlignmentX(LEFT_ALIGNMENT);
		add(actions, BorderLayout.SOUTH);
	}

	public void setGroup(LfgGroup group, String currentPlayerId, ActionHandler handler)
	{
		actions.removeAll();
		if (group == null)
		{
			title.setText("");
			meta.setText("");
			details.setText("");
			members.setText("");
			description.setText("");
			actions.setVisible(false);
			return;
		}

		String category = group.getCategory() == null ? "Unknown" : group.getCategory().getDisplayName();
		String activity = StringUtils.defaultString(group.getActivity());
		String displayTitle = category + ": " + Utils.truncate(activity, 80);
		title.setText(displayTitle);
		title.setToolTipText(activity.length() > 80 ? activity : null);

		String start = group.getStartTime() == null ? "Now" : group.getStartTime().toString();
		String players = formatPlayers(group);
		String source = group.getSource() == null ? "Unknown" : group.getSource().name();
		String status = group.getStatus() == null ? "Unknown" : group.getStatus().name();
		meta.setText(String.format("%s \u2022 %s", source, status));
		details.setText(String.format("Start: %s  \u2022  Players: %s", start, players));

		List<LfgMember> memberList = group.getMembers() == null ? List.of() : group.getMembers();
		String memberNames = memberList.stream()
			.map(LfgMember::getRsn)
			.filter(StringUtils::isNotBlank)
			.collect(Collectors.joining(", "));
		if (memberNames.isBlank())
		{
			memberNames = "None";
		}
		members.setText("Members: " + memberNames);
		members.setToolTipText(memberNames.length() > 120 ? memberNames : null);

		String desc = StringUtils.defaultString(group.getDescription());
		if (desc.isBlank())
		{
			description.setText("");
			description.setToolTipText(null);
		}
		else
		{
			description.setText("Description: " + Utils.truncate(desc, 120));
			description.setToolTipText(desc);
		}

		boolean currentMember = isCurrentMember(group, currentPlayerId);
		boolean canClose = group.getPermissions() != null && group.getPermissions().isCanClose();
		boolean canLeave = currentMember && group.getPermissions() != null && group.getPermissions().isCanLeave();
		boolean canJoin = !currentMember && group.getPermissions() != null && group.getPermissions().isCanJoin();

		JButton primary = new JButton(currentMember ? "Leave" : "Join");
		LfgUiStyle.stylePrimaryButton(primary);
		primary.setAlignmentX(LEFT_ALIGNMENT);
		primary.setEnabled(currentMember ? canLeave : canJoin);
		primary.addActionListener(e -> {
			if (handler == null)
			{
				return;
			}
			if (currentMember)
			{
				handler.onLeave(group);
			}
			else
			{
				handler.onJoin(group);
			}
		});

		actions.add(primary);
		if (canClose)
		{
			JButton close = new JButton("Close");
			LfgUiStyle.styleSecondaryButton(close);
			close.setAlignmentX(LEFT_ALIGNMENT);
			close.addActionListener(e -> {
				if (handler != null)
				{
					handler.onClose(group);
				}
			});
			actions.add(close);
		}
		actions.setVisible(actions.getComponentCount() > 0);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
		revalidate();
		repaint();
	}

	private boolean isCurrentMember(LfgGroup group, String currentPlayerId)
	{
		if (StringUtils.isBlank(currentPlayerId) || group.getMembers() == null)
		{
			return false;
		}
		String normalized = currentPlayerId.trim().toLowerCase(Locale.ROOT);
		for (LfgMember member : group.getMembers())
		{
			if (member != null && StringUtils.isNotBlank(member.getRsn()) && normalized.equals(member.getRsn().trim().toLowerCase(Locale.ROOT)))
			{
				return true;
			}
		}
		return false;
	}

	private String formatPlayers(LfgGroup group)
	{
		int members = group.getMembers() == null ? 0 : group.getMembers().size();
		Integer maximum = group.getMaximumPlayers();
		if (maximum == null)
		{
			return members + " / unlimited";
		}
		return members + " / " + maximum;
	}
}
