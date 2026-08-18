package tccrewplugin.sync.model;

import lombok.Value;

import java.util.List;

@Value
public class CollectionLogSnapshot
{
	CollectionLogState state;
	String capturedAt;
	int obtainedSlots;
	int observedSlots;
	Integer knownTotalSlots;
	int observedCategoryCount;
	int expectedCategoryCount;
	List<CollectionLogItem> items;
}
