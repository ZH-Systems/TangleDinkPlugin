package tccrewplugin.features;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tccrewplugin.features.account.AccountFeature;
import tccrewplugin.features.collectionlog.CollectionLogFeature;
import tccrewplugin.features.clanchat.ClanChatFeature;
import tccrewplugin.features.settings.SettingsFeature;
import tccrewplugin.features.synchronization.SynchronizationFeature;
import org.jetbrains.annotations.VisibleForTesting;

@Slf4j
@Singleton
public class FeatureManager
{
	private final FeatureRegistry registry = new FeatureRegistry();
	private final List<PluginFeature> startupOrder = new ArrayList<>();
	private final Set<String> runningFeatureIds = new LinkedHashSet<>();

	@Inject
	public FeatureManager(
		AccountFeature accountFeature,
		SynchronizationFeature synchronizationFeature,
		CollectionLogFeature collectionLogFeature,
		ClanChatFeature clanChatFeature,
		SettingsFeature settingsFeature
	)
	{
		register(accountFeature);
		register(synchronizationFeature);
		register(collectionLogFeature);
		register(clanChatFeature);
		register(settingsFeature);
		startupOrder.sort(Comparator.comparingInt(feature -> feature.getCategory().getOrder()));
	}

	@VisibleForTesting
	public FeatureManager(List<PluginFeature> features)
	{
		for (PluginFeature feature : features)
		{
			register(feature);
		}
		startupOrder.sort(Comparator.comparingInt(feature -> feature.getCategory().getOrder()));
	}

	private void register(PluginFeature feature)
	{
		registry.register(feature);
		startupOrder.add(feature);
	}

	public FeatureRegistry getRegistry()
	{
		return registry;
	}

	public List<PluginFeature> getFeatures()
	{
		return Collections.unmodifiableList(startupOrder);
	}

	public void startUp()
	{
		refresh();
	}

	public synchronized void refresh()
	{
		for (PluginFeature feature : startupOrder)
		{
			boolean running = runningFeatureIds.contains(feature.getId());
			if (feature.isEnabled())
			{
				if (!running)
				{
					try
					{
						feature.startUp();
						runningFeatureIds.add(feature.getId());
					}
					catch (RuntimeException ex)
					{
						log.warn("Feature startup failed for {}", feature.getId(), ex);
					}
				}
			}
			else if (running)
			{
				try
				{
					feature.shutDown();
				}
				catch (RuntimeException ex)
				{
					log.warn("Feature shutdown failed for {}", feature.getId(), ex);
				}
				runningFeatureIds.remove(feature.getId());
			}
		}
	}

	public synchronized void shutDown()
	{
		for (int i = startupOrder.size() - 1; i >= 0; i--)
		{
			PluginFeature feature = startupOrder.get(i);
			if (!runningFeatureIds.contains(feature.getId()))
			{
				continue;
			}
			try
			{
				feature.shutDown();
			}
			catch (RuntimeException ex)
			{
				log.warn("Feature shutdown failed for {}", feature.getId(), ex);
			}
			runningFeatureIds.remove(feature.getId());
		}
	}

}
