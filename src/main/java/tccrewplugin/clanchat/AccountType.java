package tccrewplugin.clanchat;

public enum AccountType
{
	NORMAL(1),
	IRON(2),
	HARDCORE_IRON(3),
	ULTIMATE_IRON(4),
	UNRANKED_IRON(5),
	GROUP_IRON(6),
	HARDCORE_GROUP_IRON(7),
	PLAYER_MODERATOR(8),
	JAGEX_MODERATOR(9);

	private final int code;

	AccountType(int code)
	{
		this.code = code;
	}

	public int getCode()
	{
		return code;
	}
}
