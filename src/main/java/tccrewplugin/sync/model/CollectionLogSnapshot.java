package tccrewplugin.sync.model;

import lombok.Data;

import java.util.List;

@Data
public class CollectionLogSnapshot
{
    private final CollectionLogState state;
    private final String capturedAt;
    private final int obtainedSlots;
    private final int observedSlots;
    private final Integer knownTotalSlots;
    private final int observedCategoryCount;
    private final int expectedCategoryCount;
    private final List<CollectionLogItem> items;
}
