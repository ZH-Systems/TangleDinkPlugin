package tccrewplugin.api;

public enum ClanWebhookAuthenticationMode
{
	BEARER_TOKEN,
	SECRET_HEADER,
	JSON_SECRET_FIELD;

	public static ClanWebhookAuthenticationMode fromConfig(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return BEARER_TOKEN;
		}

		try
		{
			return valueOf(value.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex)
		{
			return BEARER_TOKEN;
		}
	}
}
