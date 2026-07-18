package tccrewplugin.clanchat.model;

public final class ClanWebhookResponse
{
	private final int httpStatus;
	private final String message;

	public ClanWebhookResponse(int httpStatus, String message)
	{
		this.httpStatus = httpStatus;
		this.message = message;
	}

	public int getHttpStatus()
	{
		return httpStatus;
	}

	public String getMessage()
	{
		return message;
	}
}
