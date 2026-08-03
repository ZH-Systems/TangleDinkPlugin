package tccrewplugin.sync.model;

import lombok.Data;

@Data
public class PersonalBestRecord
{
    private final String activityKey;
    private final String activityName;
    private final String variant;
    private final String teamSize;
    private final long durationMilliseconds;
    private final String source;
}
