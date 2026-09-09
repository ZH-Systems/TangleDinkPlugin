package tccrewplugin.clanchat;

import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClanMemberTitleResolver
{
	private final Client client;

	public String resolveClanTitle(ClanChannel clanChannel, String author)
	{
		if (clanChannel == null || StringUtils.isBlank(author))
		{
			return null;
		}

		ClanSettings clanSettings = client.getClanSettings();
		if (clanSettings == null)
		{
			return null;
		}

		ClanChannelMember member = clanChannel.findMember(author);
		if (member == null)
		{
			return null;
		}

		ClanTitle title = clanSettings.titleForRank(member.getRank());
		return title != null ? title.getName() : null;
	}
}
