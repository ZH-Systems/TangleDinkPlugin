package tccrewplugin.lfg;

import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.client.config.RuneScapeProfileType;
import org.apache.commons.lang3.StringUtils;
import tccrewplugin.sync.model.PlayerIdentity;

import java.util.Locale;

@RequiredArgsConstructor
public class LfgPlayerIdentityProvider
{
	private final Client client;

	public PlayerIdentity resolve()
	{
		String username = client.getLocalPlayer() == null ? "" : client.getLocalPlayer().getName();
		RuneScapeProfileType profileType = RuneScapeProfileType.getCurrent(client);
		String profile = profileType == null ? "" : profileType.name();
		return new PlayerIdentity(normalize(username), normalize(profile));
	}

	public String toHeaderValue(PlayerIdentity identity)
	{
		if (identity == null)
		{
			return "";
		}
		if (StringUtils.isBlank(identity.getProfileType()))
		{
			return identity.getUsername();
		}
		return identity.getUsername() + "|" + identity.getProfileType();
	}

	private String normalize(String value)
	{
		if (value == null)
		{
			return "";
		}
		return value.replace('\u00A0', ' ').trim();
	}
}
