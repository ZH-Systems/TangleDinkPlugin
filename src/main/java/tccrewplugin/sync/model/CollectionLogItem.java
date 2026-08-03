package tccrewplugin.sync.model;

import lombok.Data;

@Data
public class CollectionLogItem
{
    private final Integer itemId;
    private final String itemName;
    private final int quantity;
    private final boolean obtained;
    private final String category;
    private final String subcategory;
}
