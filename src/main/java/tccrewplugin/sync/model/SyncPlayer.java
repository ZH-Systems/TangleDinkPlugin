package tccrewplugin.sync.model;

import lombok.Data;

@Data
public class SyncPlayer
{
    private final String displayName;
    private final SyncAccountType accountType;
}
