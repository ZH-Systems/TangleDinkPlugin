package tccrewplugin.features;

import org.junit.jupiter.api.Test;
import tccrewplugin.features.FeatureCategory;

import javax.swing.JPanel;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FeatureFrameworkTest
{
	@Test
	public void registryRejectsDuplicates()
	{
		FeatureRegistry registry = new FeatureRegistry();
		PluginFeature feature = feature("id", FeatureCategory.ACCOUNT);
		registry.register(feature);
		assertThrows(IllegalArgumentException.class, () -> registry.register(feature("id", FeatureCategory.ACCOUNT)));
	}

	@Test
	public void registryGroupsByCategory()
	{
		FeatureRegistry registry = new FeatureRegistry();
		registry.register(feature("a", FeatureCategory.ACCOUNT));
		registry.register(feature("b", FeatureCategory.CLAN));
		assertEquals(1, registry.getByCategory(FeatureCategory.ACCOUNT).size());
		assertEquals(1, registry.getByCategory(FeatureCategory.CLAN).size());
	}

	@Test
	public void managerStartsEnabledFeaturesAndSkipsDisabledOnes()
	{
		AtomicInteger started = new AtomicInteger();
		PluginFeature enabled = feature("enabled", FeatureCategory.ACCOUNT, true, started);
		PluginFeature disabled = feature("disabled", FeatureCategory.CLAN, false, started);
		new FeatureManager(List.of(enabled, disabled)).startUp();
		assertEquals(1, started.get());
	}

	@Test
	public void managerIsolatesStartupFailures()
	{
		AtomicInteger started = new AtomicInteger();
		PluginFeature broken = featureThrowing("broken", FeatureCategory.ACCOUNT);
		PluginFeature healthy = feature("healthy", FeatureCategory.CLAN, true, started);
		new FeatureManager(List.of(broken, healthy)).startUp();
		assertEquals(1, started.get());
	}

	@Test
	public void shutdownRunsInReverseOrder()
	{
		StringBuilder order = new StringBuilder();
		PluginFeature first = featureWithOrder("first", FeatureCategory.ACCOUNT, order);
		PluginFeature second = featureWithOrder("second", FeatureCategory.CLAN, order);
		FeatureManager manager = new FeatureManager(List.of(first, second));
		manager.shutDown();
		assertTrue(order.toString().endsWith("secondfirst"));
	}

	private PluginFeature feature(String id, FeatureCategory category)
	{
		return feature(id, category, true, new AtomicInteger());
	}

	private PluginFeature feature(String id, FeatureCategory category, boolean enabled, AtomicInteger started)
	{
		return new PluginFeature()
		{
			@Override public String getId() { return id; }
			@Override public String getDisplayName() { return id; }
			@Override public String getDescription() { return id; }
			@Override public FeatureCategory getCategory() { return category; }
			@Override public JPanel getPanel() { return new JPanel(); }
			@Override public boolean isEnabled() { return enabled; }
			@Override public void startUp() { started.incrementAndGet(); }
			@Override public void shutDown() { }
		};
	}

	private PluginFeature featureThrowing(String id, FeatureCategory category)
	{
		return new PluginFeature()
		{
			@Override public String getId() { return id; }
			@Override public String getDisplayName() { return id; }
			@Override public String getDescription() { return id; }
			@Override public FeatureCategory getCategory() { return category; }
			@Override public JPanel getPanel() { return new JPanel(); }
			@Override public boolean isEnabled() { return true; }
			@Override public void startUp() { throw new RuntimeException("boom"); }
			@Override public void shutDown() { }
		};
	}

	private PluginFeature featureWithOrder(String id, FeatureCategory category, StringBuilder order)
	{
		return new PluginFeature()
		{
			@Override public String getId() { return id; }
			@Override public String getDisplayName() { return id; }
			@Override public String getDescription() { return id; }
			@Override public FeatureCategory getCategory() { return category; }
			@Override public JPanel getPanel() { return new JPanel(); }
			@Override public boolean isEnabled() { return true; }
			@Override public void startUp() { }
			@Override public void shutDown() { order.append(id); }
		};
	}
}
