package tccrewplugin.sync.model;

import lombok.Value;

@Value
public class PlayerIdentity
{
	String username;
	String profileType;

	public PlayerIdentity(String username, String profileType)
	{
		this.username = username == null ? "" : username.trim();
		this.profileType = profileType == null ? "" : profileType.trim();
	}
}
