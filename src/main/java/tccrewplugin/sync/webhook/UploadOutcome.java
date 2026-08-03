package tccrewplugin.sync.webhook;

import lombok.Value;

@Value
public class UploadOutcome
{
    boolean success;
    int attempts;
    Integer statusCode;
    String message;
    boolean retryScheduled;
}
