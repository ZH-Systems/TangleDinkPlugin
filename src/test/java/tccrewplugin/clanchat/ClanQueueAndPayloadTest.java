package tccrewplugin.clanchat;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import tccrewplugin.clanchat.model.ClanMessageRecord;
import tccrewplugin.clanchat.model.ClanWebhookPayload;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClanQueueAndPayloadTest
{
	@Test
	public void queueRejectsNewestWhenFull()
	{
		ClanMessageQueue queue = new ClanMessageQueue(1);
		ClanMessageRecord first = record("a");
		ClanMessageRecord second = record("b");
		assertTrue(queue.offer(first));
		assertFalse(queue.offer(second));
	}

	@Test
	public void payloadSerializesWithoutSecrets()
	{
		ClanWebhookPayload payload = new ClanWebhookPayload(
			1,
			UUID.fromString("04bfed56-85fd-4b03-bb63-d3e5129cd88c"),
			"TEST",
			Instant.parse("2026-07-17T22:45:00Z"),
			"1.0.0",
			new ClanWebhookPayload.Player("Player", "STANDARD", 301),
			new ClanWebhookPayload.Clan("Example Clan"),
			new ClanWebhookPayload.Message("CHAT", "Sender", "GENERAL", "Hello", false),
			true
		);
		String json = new Gson().toJson(payload);
		assertTrue(json.contains("\"eventType\":\"TEST\""));
		assertFalse(payload.toString().contains("secret"));
	}

	private ClanMessageRecord record(String text)
	{
		return new ClanMessageClassifier().classify("CLAN_CHAT", "Bob", "General", "Clan", text, 301, false, Instant.now(), false);
	}
}
