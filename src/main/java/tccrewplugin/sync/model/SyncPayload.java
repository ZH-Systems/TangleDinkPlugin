package tccrewplugin.sync.model;

import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
public class SyncPayload
{
	int schemaVersion;
	String eventType;
	String eventId;
	String capturedAt;
	String command;
	SyncPlayer player;
	SyncClientMetadata client;
	CollectionLogSnapshot collectionLog;
	PersonalBestSummary personalBestSummary;
	List<PersonalBestRecord> personalBests;

    public static SyncPayload of(
        String eventType,
        String command,
        SyncPlayer player,
        SyncClientMetadata client,
        CollectionLogSnapshot collectionLog,
        PersonalBestSummary personalBestSummary,
        List<PersonalBestRecord> personalBests)
    {
        return new SyncPayload(
            1,
            eventType,
            UUID.randomUUID().toString(),
            java.time.Instant.now().toString(),
            command,
            player,
            client,
			collectionLog,
			personalBestSummary,
			personalBests
		);
    }
}
