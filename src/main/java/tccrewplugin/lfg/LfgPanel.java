package tccrewplugin.lfg;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import org.apache.commons.lang3.StringUtils;
import tccrewplugin.DinkPluginConfig;
import tccrewplugin.lfg.model.LfgCategory;
import tccrewplugin.lfg.model.LfgGroup;
import tccrewplugin.lfg.model.LfgMember;
import tccrewplugin.lfg.ui.LfgCategoryFilterPanel;
import tccrewplugin.lfg.ui.LfgCreateGroupPanel;
import tccrewplugin.lfg.ui.LfgGroupCard;
import tccrewplugin.lfg.ui.LfgGroupListPanel;
import tccrewplugin.lfg.ui.LfgUiStyle;
import tccrewplugin.lfg.ui.LfgStatusPanel;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.List;

@Singleton
public class LfgPanel extends PluginPanel
{
	private final LfgService service;
	private final DinkPluginConfig config;
	private final LfgStatusPanel statusPanel = new LfgStatusPanel();
	private final LfgCategoryFilterPanel categoryFilterPanel;
	private final LfgGroupListPanel groupListPanel = new LfgGroupListPanel();
	private final LfgCreateGroupPanel createGroupPanel;
	private final JButton refreshButton = new JButton("Refresh");
	private final JLabel title = new JLabel("Looking For Group");
	private final JLabel subtitle = new JLabel("Supabase-backed group board");
	private final JPanel content = new SidebarContentPanel();

	@Inject
	public LfgPanel(LfgService service, DinkPluginConfig config)
	{
		this.service = service;
		this.config = config;
		this.categoryFilterPanel = new LfgCategoryFilterPanel(service::updateVisibleCategories);
		this.createGroupPanel = new LfgCreateGroupPanel((categoryKey, activity, description, startTime, maximumPlayers) ->
			service.createGroup(categoryKey, activity, description, startTime, maximumPlayers)
		);

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		title.setForeground(Color.WHITE);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));
		LfgUiStyle.styleSecondaryButton(refreshButton);
		refreshButton.addActionListener(e -> service.refreshNow());

		JPanel header = new JPanel();
		header.setOpaque(false);
		header.setLayout(new BorderLayout(8, 4));
		header.setAlignmentX(Component.LEFT_ALIGNMENT);
		JPanel headerText = new JPanel();
		headerText.setOpaque(false);
		headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
		headerText.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerText.add(title);
		headerText.add(subtitle);
		header.add(headerText, BorderLayout.WEST);
		header.add(refreshButton, BorderLayout.EAST);
		JPanel statusWrap = new JPanel(new BorderLayout());
		statusWrap.setOpaque(false);
		statusWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
		statusWrap.add(statusPanel, BorderLayout.CENTER);
		statusWrap.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		header.add(statusWrap, BorderLayout.SOUTH);

		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		content.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(header);
		content.add(Box.createVerticalStrut(8));
		content.add(createSection("Filters", categoryFilterPanel));
		content.add(Box.createVerticalStrut(8));
		content.add(createSection("Active Groups", groupListPanel));
		content.add(Box.createVerticalStrut(8));
		content.add(createSection("Create a Group", createGroupPanel));

		JScrollPane scrollPane = new JScrollPane(content);
		LfgUiStyle.styleScrollPane(scrollPane);
		add(scrollPane, BorderLayout.CENTER);

		addHierarchyListener(new HierarchyListener()
		{
			@Override
			public void hierarchyChanged(HierarchyEvent e)
			{
				if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
				{
					service.setPanelVisible(isShowing());
				}
			}
		});
	}

	public void updateState(
		List<LfgCategory> categories,
		List<LfgGroup> groups,
		tccrewplugin.sync.model.PlayerIdentity identity,
		String status,
		String error,
		String allowList,
		boolean showFullGroups,
		boolean showDiscordGroups,
		boolean showRuneLiteGroups,
		boolean busy
	)
	{
		statusPanel.setPlayer(identity == null ? "" : identity.getUsername());
		statusPanel.setStatus(status);
		statusPanel.setError(error);
		categoryFilterPanel.setCategories(categories, allowList);
		createGroupPanel.setCategories(categories);
		createGroupPanel.setBusy(busy);
		categoryFilterPanel.setBusy(busy);
		List<LfgGroup> safeGroups = groups == null ? List.of() : groups;
		groupListPanel.setGroups(safeGroups, identity == null ? "" : identity.getUsername(), new LfgGroupCard.ActionHandler()
		{
			@Override
			public void onJoin(LfgGroup group)
			{
				service.joinGroup(group == null ? "" : group.getId());
			}

			@Override
			public void onLeave(LfgGroup group)
			{
				service.leaveGroup(group == null ? "" : group.getId());
			}

			@Override
			public void onClose(LfgGroup group)
			{
				service.closeGroup(group == null ? "" : group.getId());
			}
		});
		refreshButton.setEnabled(!busy);
		revalidate();
		repaint();
	}

	public void setBusy(boolean busy)
	{
		refreshButton.setEnabled(!busy);
	}

	private JPanel createSection(String heading, Component body)
	{
		JPanel section = new JPanel(new BorderLayout());
		section.setOpaque(true);
		section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		section.setAlignmentX(Component.LEFT_ALIGNMENT);
		section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		section.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
			BorderFactory.createEmptyBorder(8, 10, 10, 10)
		));

		JLabel label = new JLabel(heading);
		label.setForeground(Color.WHITE);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel wrapper = new JPanel();
		wrapper.setOpaque(false);
		wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
		wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		wrapper.add(label);
		if (body != null)
		{
			if (body instanceof javax.swing.JComponent)
			{
				((javax.swing.JComponent) body).setAlignmentX(Component.LEFT_ALIGNMENT);
				((javax.swing.JComponent) body).setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
			}
			wrapper.add(body);
		}

		section.add(wrapper, BorderLayout.CENTER);
		return section;
	}

	private static final class SidebarContentPanel extends JPanel implements Scrollable
	{
		private SidebarContentPanel()
		{
			super();
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
		{
			return Math.max(visibleRect.height - 16, 16);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
