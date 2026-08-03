package tccrewplugin.features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FeatureRegistry
{
	private final Map<String, PluginFeature> features = new LinkedHashMap<>();

	public synchronized void register(PluginFeature feature)
	{
		if (feature == null)
		{
			throw new IllegalArgumentException("feature");
		}

		String id = feature.getId();
		if (features.containsKey(id))
		{
			throw new IllegalArgumentException("Duplicate feature id: " + id);
		}

		features.put(id, feature);
	}

	public synchronized Collection<PluginFeature> getAll()
	{
		return Collections.unmodifiableList(new ArrayList<>(features.values()));
	}

	public synchronized List<PluginFeature> getByCategory(FeatureCategory category)
	{
		List<PluginFeature> result = new ArrayList<>();
		for (PluginFeature feature : features.values())
		{
			if (feature.getCategory() == category)
			{
				result.add(feature);
			}
		}
		return Collections.unmodifiableList(result);
	}

	public synchronized PluginFeature getById(String id)
	{
		return features.get(id);
	}
}
