package tccrewplugin.clanchat;

public enum SystemMessageType
{
	NORMAL(1),
	DROP(2),
	RAID_DROP(3),
	PET_DROP(4),
	PERSONAL_BEST(5),
	COLLECTION_LOG(6),
	QUESTS(7),
	PVP(8),
	ATTENDANCE(9),
	LEVEL_UP(10),
	COMBAT_ACHIEVEMENTS(11),
	CLUE_DROP(12),
	DIARY(13),
	UNKNOWN(100),
	LOGIN(-1);

	private final int code;

	SystemMessageType(int code)
	{
		this.code = code;
	}

	public int getCode()
	{
		return code;
	}
}
