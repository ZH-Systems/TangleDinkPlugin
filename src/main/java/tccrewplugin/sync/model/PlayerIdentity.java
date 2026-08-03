package tccrewplugin.sync.model;

import java.util.Objects;

public final class PlayerIdentity
{
	private final String username;
	private final String profileType;

	public PlayerIdentity(String username, String profileType)
	{
		this.username = username == null ? "" : username.trim();
		this.profileType = profileType == null ? "" : profileType.trim();
	}

	public String getUsername()
	{
		return username;
	}

	public String getProfileType()
	{
		return profileType;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof PlayerIdentity))
		{
			return false;
		}
		PlayerIdentity that = (PlayerIdentity) o;
		return username.equals(that.username) && profileType.equals(that.profileType);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(username, profileType);
	}
}
