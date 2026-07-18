package tccrewplugin.features;

public enum FeatureCategory
{
	ACCOUNT("Account", 0),
	SYNCHRONIZATION("Synchronization", 10),
	COLLECTION_LOG("Collection Log", 20),
	CLAN("Clan", 30),
	UTILITIES("Utilities", 40),
	SETTINGS("Settings", 50);

	private final String displayName;
	private final int order;

	FeatureCategory(String displayName, int order)
	{
		this.displayName = displayName;
		this.order = order;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getOrder()
	{
		return order;
	}
}
