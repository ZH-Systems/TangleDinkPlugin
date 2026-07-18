package tccrewplugin.clanchat;

import org.junit.jupiter.api.Test;
import tccrewplugin.TestConfig;
import tccrewplugin.clanchat.model.ClanMessageRecord;
import tccrewplugin.clanchat.model.ClanMessageType;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClanMessageLogicTest
{
	@Test
	public void classifierStripsFormattingTags()
	{
		ClanMessageClassifier classifier = new ClanMessageClassifier();
		ClanMessageRecord record = classifier.classify("CLAN_CHAT", "Bob", "General", "Clan", "<col=ff0000>Hello</col>", 301, false, Instant.parse("2026-07-17T22:30:00Z"), false);
		assertEquals("Hello", record.getText());
	}

	@Test
	public void broadcastClassificationIsDetected()
	{
		ClanMessageClassifier classifier = new ClanMessageClassifier();
		ClanMessageRecord record = classifier.classify("GAMEMESSAGE", "Bob", null, "Clan", "Bob just received a new collection log item: Bandos chestplate", 301, false, Instant.now(), false);
		assertTrue(record.getType() == ClanMessageType.CLAN_BROADCAST || record.getType() == ClanMessageType.COLLECTION_LOG);
	}

	@Test
	public void requiredClanMatchingRespectsNormalization()
	{
		TestConfig config = new TestConfig();
		config.requiredClanName = "Example Clan";
		ClanMessageFilter filter = new ClanMessageFilter(1);
		ClanMessageRecord record = new ClanMessageClassifier().classify("CLAN_CHAT", "Bob", "General", "Example Clan", "hello", 301, false, Instant.parse("2026-07-17T22:30:00Z"), false);
		assertTrue(filter.allow(config, record, true, " example   clan ").isAccepted());
	}

	@Test
	public void requiredClanMismatchIsRejected()
	{
		TestConfig config = new TestConfig();
		config.requiredClanName = "Example Clan";
		ClanMessageFilter filter = new ClanMessageFilter(1);
		ClanMessageRecord record = new ClanMessageClassifier().classify("CLAN_CHAT", "Bob", "General", "Other Clan", "hello", 301, false, Instant.parse("2026-07-17T22:30:00Z"), false);
		assertFalse(filter.allow(config, record, true, "Other Clan").isAccepted());
	}

	@Test
	public void guestBroadcastRejectedWhenDisabled()
	{
		TestConfig config = new TestConfig();
		config.sendGuestBroadcasts = false;
		ClanMessageFilter filter = new ClanMessageFilter(1);
		ClanMessageRecord record = new ClanMessageClassifier().classify("CLAN_BROADCAST", "Guest", null, "Clan", "message", 301, true, Instant.parse("2026-07-17T22:30:00Z"), false);
		assertFalse(filter.allow(config, record, true, "Clan").isAccepted());
	}

	@Test
	public void approvedGuestMatchingWorks()
	{
		TestConfig config = new TestConfig();
		config.sendGuestBroadcasts = true;
		config.approvedGuestUsernames = "Guest One, Guest Two";
		ClanMessageFilter filter = new ClanMessageFilter(1);
		ClanMessageRecord record = new ClanMessageClassifier().classify("CLAN_BROADCAST", "Guest One", null, "Clan", "message", 301, true, Instant.parse("2026-07-17T22:30:00Z"), false);
		assertTrue(filter.allow(config, record, true, "Clan").isAccepted());
	}

	@Test
	public void duplicateFingerprintIsGenerated()
	{
		ClanMessageClassifier classifier = new ClanMessageClassifier();
		ClanMessageRecord record = classifier.classify("CLAN_CHAT", "Bob", "General", "Clan", "hello", 301, false, Instant.parse("2026-07-17T22:30:00Z"), false);
		assertTrue(record.getFingerprint().contains("bob"));
	}

	@Test
	public void duplicateDetectionExpires()
	{
		ClanMessageFilter filter = new ClanMessageFilter(1);
		TestConfig config = new TestConfig();
		ClanMessageClassifier classifier = new ClanMessageClassifier();
		ClanMessageRecord record = classifier.classify("CLAN_CHAT", "Bob", "General", "Clan", "hello", 301, false, Instant.parse("2026-07-17T22:30:00Z"), false);
		assertTrue(filter.isDuplicate(record) == false);
		assertTrue(filter.isDuplicate(record));
		try
		{
			Thread.sleep(1100L);
		}
		catch (InterruptedException ignored)
		{
			Thread.currentThread().interrupt();
		}
		assertTrue(filter.allow(config, record, true, "Clan").isAccepted());
	}
}
