package tccrewplugin.domain;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

@Value
public class RemoteEventConfig {
    int version;
    @Nullable Event event;

    @Value
    public static class Event {
        boolean active;
        @Nullable String eventId;
        @Nullable String name;
        @Nullable String requiredCommand;
        @Nullable String migrationUrl;
        @Nullable String message;
    }
}

