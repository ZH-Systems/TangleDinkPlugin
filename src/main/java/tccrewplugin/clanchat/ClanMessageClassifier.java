package tccrewplugin.clanchat;

import tccrewplugin.clanchat.model.ClanMessageRecord;
import tccrewplugin.clanchat.model.ClanMessageType;
import tccrewplugin.util.TextSanitizer;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class ClanMessageClassifier
{
	public ClanMessageRecord classify(
		String chatTypeName,
		String sender,
		String senderRank,
		String clanName,
		String messageText,
		Integer world,
		boolean guest,
		Instant occurredAt,
		boolean test
	)
	{
		String stripped = TextSanitizer.stripTags(messageText);
		ClanMessageType type = classifyType(chatTypeName, stripped, guest);
		String normalizedSender = TextSanitizer.normalizeRuneScapeName(sender);
		String normalizedClan = TextSanitizer.normalizeClanName(clanName);
		String fingerprint = fingerprint(type, normalizedSender, normalizedClan, TextSanitizer.normalizeRuneScapeName(stripped), world, occurredAt, guest);
		return new ClanMessageRecord(
			UUID.nameUUIDFromBytes(fingerprint.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
			occurredAt == null ? Instant.now() : occurredAt,
			type,
			TextSanitizer.stripTags(sender),
			TextSanitizer.stripTags(senderRank),
			stripped,
			guest,
			TextSanitizer.stripTags(clanName),
			world,
			fingerprint,
			test
		);
	}

	private ClanMessageType classifyType(String chatTypeName, String messageText, boolean guest)
	{
		String type = chatTypeName == null ? "" : chatTypeName.trim().toUpperCase(Locale.ROOT);
		if (type.contains("CLAN") && type.contains("BROADCAST"))
		{
			return guest ? ClanMessageType.GUEST_BROADCAST : ClanMessageType.CLAN_BROADCAST;
		}
		if (type.contains("CLAN") && type.contains("SYSTEM"))
		{
			return ClanMessageType.SYSTEM;
		}
		if (type.contains("CLAN") && (type.contains("CHAT") || type.contains("MESSAGE")))
		{
			return ClanMessageType.CHAT;
		}
		if (looksLikeBroadcast(messageText))
		{
			return guest ? ClanMessageType.GUEST_BROADCAST : ClanMessageType.CLAN_BROADCAST;
		}
		return ClanMessageType.UNKNOWN;
	}

	private boolean looksLikeBroadcast(String messageText)
	{
		String normalized = TextSanitizer.normalizeRuneScapeName(messageText);
		return normalized.contains("just received")
			|| normalized.contains("completed a")
			|| normalized.contains("leveled up")
			|| normalized.contains("completed the quest")
			|| normalized.contains("received a new collection log item")
			|| normalized.contains("completed a combat achievement")
			|| normalized.contains("obtained");
	}

	public String fingerprint(ClanMessageRecord record)
	{
		return record.getFingerprint();
	}

	public String fingerprint(ClanMessageType type, String sender, String clan, String message, Integer world, Instant occurredAt, boolean guest)
	{
		long bucket = occurredAt == null ? 0L : occurredAt.getEpochSecond() / 30L;
		return type + "|" + sender + "|" + clan + "|" + message + "|" + (world == null ? 0 : world) + "|" + bucket + "|" + guest;
	}
}
