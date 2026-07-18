package tccrewplugin.ui;

import tccrewplugin.features.FeatureCategory;
import tccrewplugin.features.FeatureManager;
import tccrewplugin.features.PluginFeature;
import tccrewplugin.ui.components.FeatureNavigationItem;
import tccrewplugin.ui.components.FolderHeader;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FeatureNavigationPanel extends JPanel
{
	private final FeatureManager featureManager;
	private final Consumer<String> selectionConsumer;
	private final Map<FeatureCategory, JPanel> categoryBodies = new EnumMap<>(FeatureCategory.class);
	private final Map<FeatureCategory, FolderHeader> categoryHeaders = new EnumMap<>(FeatureCategory.class);
	private final Map<FeatureCategory, Boolean> expanded = new EnumMap<>(FeatureCategory.class);
	private final Map<String, FeatureNavigationItem> itemButtons = new LinkedHashMap<>();
	private final java.util.Set<String> visibleFeatureIds = new java.util.LinkedHashSet<>();
	private String selectedId;

	public FeatureNavigationPanel(FeatureManager featureManager, Consumer<String> selectionConsumer)
	{
		this.featureManager = featureManager;
		this.selectionConsumer = selectionConsumer;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		refresh();
	}

	public final void refresh()
	{
		removeAll();
		categoryBodies.clear();
		itemButtons.clear();
		visibleFeatureIds.clear();
		for (FeatureCategory category : FeatureCategory.values())
		{
			expanded.putIfAbsent(category, true);
			JPanel body = new JPanel();
			body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
			body.setAlignmentX(Component.LEFT_ALIGNMENT);
			categoryBodies.put(category, body);

			FolderHeader header = new FolderHeader(toggleLabel(category));
			header.setAlignmentX(Component.LEFT_ALIGNMENT);
			header.addActionListener(e -> toggleCategory(category));
			categoryHeaders.put(category, header);
			add(header);
			add(body);
		}
		for (PluginFeature feature : featureManager.getFeatures())
		{
			FeatureNavigationItem item = new FeatureNavigationItem(feature.getDisplayName());
			item.setAlignmentX(Component.LEFT_ALIGNMENT);
			item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
			item.addActionListener(e -> select(feature.getId()));
			itemButtons.put(feature.getId(), item);
			JPanel body = categoryBodies.get(feature.getCategory());
			if (body != null && feature.isEnabled())
			{
				body.add(item);
				visibleFeatureIds.add(feature.getId());
			}
		}
		updateVisibility();
		revalidate();
		repaint();
		if (selectedId == null || !visibleFeatureIds.contains(selectedId))
		{
			selectFirstAvailable();
		}
		else
		{
			select(selectedId);
		}
	}

	public void select(String id)
	{
		selectedId = id;
		for (Map.Entry<String, FeatureNavigationItem> entry : itemButtons.entrySet())
		{
			entry.getValue().setEnabled(!entry.getKey().equals(id));
		}
		selectionConsumer.accept(id);
	}

	public String getSelectedId()
	{
		return selectedId;
	}

	private void selectFirstAvailable()
	{
		if (!itemButtons.isEmpty())
		{
			String firstVisible = visibleFeatureIds.stream().findFirst().orElse(null);
			if (firstVisible != null)
			{
				select(firstVisible);
			}
		}
	}

	private void toggleCategory(FeatureCategory category)
	{
		expanded.put(category, !expanded.getOrDefault(category, true));
		FolderHeader header = categoryHeaders.get(category);
		if (header != null)
		{
			header.setText(toggleLabel(category));
		}
		updateVisibility();
	}

	private void updateVisibility()
	{
		for (FeatureCategory category : FeatureCategory.values())
		{
			JPanel body = categoryBodies.get(category);
			if (body != null)
			{
				body.setVisible(expanded.getOrDefault(category, true));
			}
		}
		SwingUtilities.invokeLater(this::repaint);
	}

	private String toggleLabel(FeatureCategory category)
	{
		return (expanded.getOrDefault(category, true) ? "▾ " : "▸ ") + category.getDisplayName();
	}
}
