package tccrewplugin.features.collectionlog;

import tccrewplugin.TangleDinkConfig;
import tccrewplugin.collectionlog.CollectionLogService;
import tccrewplugin.features.FeatureCategory;
import tccrewplugin.features.PluginFeature;

import javax.inject.Inject;
import javax.swing.JComponent;

public class CollectionLogFeature implements PluginFeature
{
	private final TangleDinkConfig config;
	private final CollectionLogPanel panel;

	@Inject
	public CollectionLogFeature(TangleDinkConfig config, CollectionLogService collectionLogService)
	{
		this.config = config;
		this.panel = new CollectionLogPanel(collectionLogService);
	}

	@Override
	public String getId()
	{
		return "collection-log";
	}

	@Override
	public String getDisplayName()
	{
		return "Collection Log Sync";
	}

	@Override
	public String getDescription()
	{
		return "Collection log capture and sync";
	}

	@Override
	public FeatureCategory getCategory()
	{
		return FeatureCategory.COLLECTION_LOG;
	}

	@Override
	public JComponent getPanel()
	{
		return panel;
	}

	@Override
	public boolean isEnabled()
	{
		return config.enableCollectionLogFeature();
	}

	@Override
	public void startUp()
	{
		panel.refresh();
	}

	@Override
	public void shutDown()
	{
	}
}
