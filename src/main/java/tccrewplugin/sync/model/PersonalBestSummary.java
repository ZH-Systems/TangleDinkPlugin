package tccrewplugin.sync.model;

import lombok.Data;

@Data
public class PersonalBestSummary
{
    private final int known;
    private final int notLoaded;
    private final int malformed;
    private final int unsupported;
}
