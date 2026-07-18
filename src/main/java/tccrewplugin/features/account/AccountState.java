package tccrewplugin.features.account;

import java.time.Instant;

public final class AccountState
{
	private final String username;
	private final String profileType;
	private final String clanName;
	private final Instant lastSync;

	public AccountState(String username, String profileType, String clanName, Instant lastSync)
	{
		this.username = username;
		this.profileType = profileType;
		this.clanName = clanName;
		this.lastSync = lastSync;
	}

	public String getUsername()
	{
		return username;
	}

	public String getProfileType()
	{
		return profileType;
	}

	public String getClanName()
	{
		return clanName;
	}

	public Instant getLastSync()
	{
		return lastSync;
	}
}
