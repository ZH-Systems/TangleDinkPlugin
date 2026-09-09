package tccrewplugin.clanchat;

import net.runelite.api.clan.ClanChannel;

public class ClanMemberResolver
{
	public String resolveClanName(ClanChannel channel)
	{
		return channel == null ? null : channel.getName();
	}
}
