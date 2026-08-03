package tccrewplugin.clanchat;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ClanAccountTypeResolver
{
	public AccountType getAccountType(String rawAuthor)
	{
		if (rawAuthor == null || rawAuthor.isBlank())
		{
			return AccountType.NORMAL;
		}

		if (rawAuthor.contains("<img=0>"))
		{
			return AccountType.PLAYER_MODERATOR;
		}
		if (rawAuthor.contains("<img=2>"))
		{
			return AccountType.IRON;
		}
		if (rawAuthor.contains("<img=10>"))
		{
			return AccountType.HARDCORE_IRON;
		}
		if (rawAuthor.contains("<img=3>"))
		{
			return AccountType.ULTIMATE_IRON;
		}
		if (rawAuthor.contains("<img=41>"))
		{
			return AccountType.GROUP_IRON;
		}
		if (rawAuthor.contains("<img=43>"))
		{
			return AccountType.UNRANKED_IRON;
		}
		if (rawAuthor.contains("<img=42>"))
		{
			return AccountType.HARDCORE_GROUP_IRON;
		}

		return AccountType.NORMAL;
	}
}
