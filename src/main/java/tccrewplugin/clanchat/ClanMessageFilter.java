package tccrewplugin.clanchat;

import tccrewplugin.TangleDinkConfig;
import tccrewplugin.clanchat.model.ClanMessageRecord;
import tccrewplugin.clanchat.model.ClanMessageType;
import tccrewplugin.util.TextSanitizer;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClanMessageFilter
{
	private final ConcurrentMap<String, Instant> recentFingerprints = new ConcurrentHashMap<>();
	private final int duplicateWindowSeconds;

	public ClanMessageFilter()
	{
		this(60);
	}

	public ClanMessageFilter(int duplicateWindowSeconds)
	{
		this.duplicateWindowSeconds = Math.max(1, duplicateWindowSeconds);
	}

	public Decision allow(TangleDinkConfig config, ClanMessageRecord record, boolean loggedIn, String activeClanName)
	{
		if (!config.clanWebhookEnabled())
		{
			return Decision.rejected("webhooks disabled");
		}
		if (TextSanitizer.stripTags(config.clanWebhookEndpoint()).isEmpty())
		{
			return Decision.rejected("endpoint missing");
		}
		if (TextSanitizer.stripTags(config.clanWebhookSecret()).isEmpty())
		{
			return Decision.rejected("secret missing");
		}
		if (!loggedIn)
		{
			return Decision.rejected("player not logged in");
		}
		if (record == null || TextSanitizer.normalizeRuneScapeName(record.getText()).isEmpty())
		{
			return Decision.rejected("empty message");
		}
		if (record.isTest())
		{
			return Decision.accepted();
		}
		if (!matchesType(config, record))
		{
			return Decision.rejected("message type disabled");
		}
		if (!matchesClan(config, activeClanName, record.getClanName()))
		{
			return Decision.rejected("clan mismatch");
		}
		if (record.isGuest() && !guestAllowed(config, record))
		{
			return Decision.rejected("guest broadcast disabled");
		}
		if (isDuplicate(record))
		{
			return Decision.rejected("duplicate");
		}
		if (looksLikePluginMessage(record.getText()))
		{
			return Decision.rejected("plugin loop");
		}
		return Decision.accepted();
	}

	private boolean matchesType(TangleDinkConfig config, ClanMessageRecord record)
	{
		ClanMessageType type = record.getType();
		switch (type)
		{
			case CHAT:
				return config.sendPublicClanMessages();
			case SYSTEM:
				return config.sendSystemClanMessages();
			case CLAN_BROADCAST:
				return config.sendClanBroadcasts();
			case GUEST_BROADCAST:
				return config.sendGuestBroadcasts();
			case LEVEL_UP:
			case QUEST:
			case COLLECTION_LOG:
			case COMBAT_ACHIEVEMENT:
			case LOOT:
				return config.sendClanBroadcasts();
			default:
				return false;
		}
	}

	private boolean matchesClan(TangleDinkConfig config, String activeClanName, String eventClanName)
	{
		String required = TextSanitizer.normalizeClanName(config.requiredClanName());
		if (required.isEmpty())
		{
			return true;
		}

		String active = TextSanitizer.normalizeClanName(activeClanName);
		String eventClan = TextSanitizer.normalizeClanName(eventClanName);
		return required.equals(active) || required.equals(eventClan);
	}

	private boolean guestAllowed(TangleDinkConfig config, ClanMessageRecord record)
	{
		Set<String> approved = approvedGuests(config);
		if (approved.isEmpty())
		{
			return config.sendGuestBroadcasts();
		}
		return approved.contains(TextSanitizer.normalizeRuneScapeName(record.getSender()));
	}

	private Set<String> approvedGuests(TangleDinkConfig config)
	{
		String raw = config.approvedGuestUsernames();
		if (raw == null || raw.trim().isEmpty())
		{
			return Set.of();
		}
		Set<String> result = new HashSet<>();
		Arrays.stream(raw.split(","))
			.map(TextSanitizer::normalizeRuneScapeName)
			.filter(s -> !s.isEmpty())
			.forEach(result::add);
		return result;
	}

	public boolean isDuplicate(ClanMessageRecord record)
	{
		String fingerprint = record.getFingerprint();
		Instant now = Instant.now();
		purge(now);
		Instant existing = recentFingerprints.putIfAbsent(fingerprint, now);
		return existing != null;
	}

	public String fingerprint(ClanMessageRecord record)
	{
		return record.getFingerprint();
	}

	public void clear()
	{
		recentFingerprints.clear();
	}

	private void purge(Instant now)
	{
		recentFingerprints.entrySet().removeIf(entry -> entry.getValue().plusSeconds(duplicateWindowSeconds).isBefore(now));
	}

	private boolean looksLikePluginMessage(String text)
	{
		String normalized = TextSanitizer.normalizeRuneScapeName(text);
		return normalized.startsWith("tangle dink")
			|| normalized.startsWith("dink plugin")
			|| normalized.contains("webhook test message");
	}

	public static final class Decision
	{
		private final boolean accepted;
		private final String reason;

		private Decision(boolean accepted, String reason)
		{
			this.accepted = accepted;
			this.reason = reason;
		}

		public static Decision accepted()
		{
			return new Decision(true, null);
		}

		public static Decision rejected(String reason)
		{
			return new Decision(false, reason);
		}

		public boolean isAccepted()
		{
			return accepted;
		}

		public String getReason()
		{
			return reason;
		}
	}
}
