package tccrewplugin.features;

import javax.swing.JComponent;

public interface PluginFeature
{
	String getId();

	String getDisplayName();

	String getDescription();

	FeatureCategory getCategory();

	JComponent getPanel();

	boolean isEnabled();

	void startUp();

	void shutDown();
}
