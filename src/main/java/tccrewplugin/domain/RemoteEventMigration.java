package tccrewplugin.domain;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Value
public class RemoteEventMigration {
    int version;
    @Nullable String eventId;
    @Nullable Map<String, Object> config;
}

