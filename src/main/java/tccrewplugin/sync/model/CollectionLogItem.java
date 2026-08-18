package tccrewplugin.sync.model;

import lombok.Value;

@Value
public class CollectionLogItem
{
	Integer itemId;
	String itemName;
	int quantity;
	boolean obtained;
	String category;
	String subcategory;
}
