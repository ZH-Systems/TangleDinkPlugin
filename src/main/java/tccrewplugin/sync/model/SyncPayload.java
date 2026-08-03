package tccrewplugin.sync.model;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SyncPayload
{
    private final int schemaVersion;
    private final String eventType;
    private final String eventId;
    private final String capturedAt;
    private final String command;
    private final SyncPlayer player;
    private final SyncClientMetadata client;
    private final CollectionLogSnapshot collectionLog;
    private final PersonalBestSummary personalBestSummary;
    private final List<PersonalBestRecord> personalBests;

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
