package tccrewplugin.collectionlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class CollectionLogItemMapper
{
	public Mapping empty()
	{
		return new Mapping(List.of(), Map.of());
	}

	public Mapping build(List<Integer> manifestItems, List<Integer> cacheItems)
	{
		TreeSet<Integer> ordered = new TreeSet<>();
		if (manifestItems != null)
		{
			ordered.addAll(manifestItems);
		}

		List<Integer> missing = new ArrayList<>();
		if (cacheItems != null)
		{
			for (Integer id : cacheItems)
			{
				if (id != null && !ordered.contains(id))
				{
					missing.add(id);
				}
			}
		}
		Collections.sort(missing);
		ordered.addAll(missing);

		Map<Integer, Integer> indexByItemId = new LinkedHashMap<>();
		List<Integer> orderedItems = new ArrayList<>(ordered);
		for (int i = 0; i < orderedItems.size(); i++)
		{
			indexByItemId.put(orderedItems.get(i), i);
		}

		return new Mapping(orderedItems, indexByItemId);
	}

	public static final class Mapping
	{
		private final List<Integer> orderedItems;
		private final Map<Integer, Integer> indexByItemId;

		private Mapping(List<Integer> orderedItems, Map<Integer, Integer> indexByItemId)
		{
			this.orderedItems = Collections.unmodifiableList(new ArrayList<>(orderedItems));
			this.indexByItemId = Collections.unmodifiableMap(new LinkedHashMap<>(indexByItemId));
		}

		public List<Integer> getOrderedItems()
		{
			return orderedItems;
		}

		public Map<Integer, Integer> getIndexByItemId()
		{
			return indexByItemId;
		}
	}
}
