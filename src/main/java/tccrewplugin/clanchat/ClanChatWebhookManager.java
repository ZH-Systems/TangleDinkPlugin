package tccrewplugin.clanchat;

import tccrewplugin.DinkPluginConfig;
import tccrewplugin.TcCrewPlugin;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.client.RuneLite;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Singleton
public class ClanChatWebhookManager
{
	private final Client client;
	private final DinkPluginConfig config;
	private final ClanWebhookService webhookService;
	private final ClanMemberTitleResolver titleResolver;

	private volatile boolean activeClan;

	@Inject
	public ClanChatWebhookManager(Client client, DinkPluginConfig config, ClanWebhookService webhookService, ClanMemberTitleResolver titleResolver)
	{
		this.client = client;
		this.config = config;
		this.webhookService = webhookService;
		this.titleResolver = titleResolver;
	}

	public void startUp()
	{
		activeClan = false;
		webhookService.startUp();
		refreshActiveClanState();
	}

	public void shutDown()
	{
		activeClan = false;
		webhookService.shutDown();
	}

	public void onConfigChanged(String key)
	{
		webhookService.onConfigChanged(key);
	}

	public void onClanChannelChanged(ClanChannelChanged event)
	{
		if (event.getClanId() != ClanID.CLAN)
		{
			return;
		}

		ClanChannel channel = event.getClanChannel();
		activeClan = channel != null && StringUtils.isNotBlank(ClanMessageSanitizer.normalizeWhitespace(channel.getName()));
	}

	public void onChatMessage(ChatMessage event)
	{
		if (event == null)
		{
			return;
		}

		ChatMessageType type = event.getType();
		if (type != ChatMessageType.CLAN_CHAT && type != ChatMessageType.CLAN_MESSAGE)
		{
			return;
		}

		Optional<ClanChannel> clanChannelOptional = getActiveClanChannel();
		if (!clanChannelOptional.isPresent())
		{
			return;
		}

		ClanChannel clanChannel = clanChannelOptional.get();
		if (!matchesConfiguredClan(clanChannel))
		{
			return;
		}

		if (type == ChatMessageType.CLAN_CHAT && !config.sendNormalChat())
		{
			return;
		}

		if (type == ChatMessageType.CLAN_MESSAGE && !config.sendSystemBroadcasts())
		{
			return;
		}

		String rawAuthor = event.getName();
		String author = ClanMessageSanitizer.sanitizeAuthor(rawAuthor);
		String content = ClanMessageSanitizer.sanitizeMessage(event.getMessage());
		if (content.isEmpty() || containsUnsupportedColorMarkup(content))
		{
			return;
		}

		AccountType accountType = ClanAccountTypeResolver.getAccountType(rawAuthor);
		SystemMessageType systemMessageType = ClanMessageClassifier.getSystemMessageType(event.getMessage(), type);
		if (systemMessageType == SystemMessageType.LOGIN && !config.sendLoginGuidance())
		{
			return;
		}
		if (systemMessageType == SystemMessageType.UNKNOWN && !config.sendUnknownBroadcasts())
		{
			return;
		}

		String clanTitle = type == ChatMessageType.CLAN_CHAT
			? titleResolver.resolveClanTitle(clanChannel, author)
			: null;

		ClanMessageEvent messageEvent = new ClanMessageEvent(
			author,
			content,
			accountType,
			systemMessageType,
			clanTitle,
			event.getTimestamp());
		messageEvent.setEventId(UUID.randomUUID().toString());
		messageEvent.setChatMessageType(type.name());

		if (config.includeClientMetadata())
		{
			messageEvent.setClanName(ClanMessageSanitizer.normalizeWhitespace(clanChannel.getName()));
			messageEvent.setPluginVersion(getPluginVersion());
			messageEvent.setRuneliteVersion(RuneLite.USER_AGENT);
		}

		if (config.debugLogging())
		{
			log.debug("Queued clan chat webhook event type={} classification={} author={}",
				type,
				systemMessageType,
				author);
		}

		webhookService.submit(messageEvent);
	}

	private void refreshActiveClanState()
	{
		Optional<ClanChannel> clanChannel = getActiveClanChannel();
		activeClan = clanChannel.isPresent()
			&& StringUtils.isNotBlank(ClanMessageSanitizer.normalizeWhitespace(clanChannel.get().getName()));
	}

	private Optional<ClanChannel> getActiveClanChannel()
	{
		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel == null)
		{
			return Optional.empty();
		}

		String name = ClanMessageSanitizer.normalizeWhitespace(clanChannel.getName());
		if (name.isEmpty())
		{
			return Optional.empty();
		}

		return Optional.of(clanChannel);
	}

	private boolean matchesConfiguredClan(ClanChannel clanChannel)
	{
		String configuredClanName = ClanMessageSanitizer.normalizeWhitespace(config.clanName());
		if (configuredClanName.isEmpty())
		{
			return true;
		}

		String activeClanName = ClanMessageSanitizer.normalizeWhitespace(clanChannel.getName());
		return !activeClanName.isEmpty() && configuredClanName.equalsIgnoreCase(activeClanName);
	}

	private boolean containsUnsupportedColorMarkup(String content)
	{
		// RuneLite clan chat format can leave closing color tags behind after sanitization.
		// The reference receiver does not handle those fragments safely, so suppress them.
		return content != null && content.contains("</col>");
	}

	private String getPluginVersion()
	{
		String version = TcCrewPlugin.class.getPackage() != null
			? TcCrewPlugin.class.getPackage().getImplementationVersion()
			: null;
		return StringUtils.defaultIfBlank(version, "unknown");
	}
}
