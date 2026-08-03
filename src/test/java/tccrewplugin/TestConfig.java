package tccrewplugin;

public class TestConfig implements TangleDinkConfig
{
	public boolean clanWebhookEnabled = true;
	public String clanWebhookEndpoint = "http://localhost";
	public String clanWebhookSecret = "secret";
	public String requiredClanName = "";
	public boolean sendPublicClanMessages = true;
	public boolean sendClanBroadcasts = true;
	public boolean sendGuestBroadcasts = false;
	public boolean sendSystemClanMessages = true;
	public String approvedGuestUsernames = "";

	@Override
	public boolean clanWebhookEnabled()
	{
		return clanWebhookEnabled;
	}

	@Override
	public String clanWebhookEndpoint()
	{
		return clanWebhookEndpoint;
	}

	@Override
	public String clanWebhookSecret()
	{
		return clanWebhookSecret;
	}

	@Override
	public String requiredClanName()
	{
		return requiredClanName;
	}

	@Override
	public boolean sendPublicClanMessages()
	{
		return sendPublicClanMessages;
	}

	@Override
	public boolean sendClanBroadcasts()
	{
		return sendClanBroadcasts;
	}

	@Override
	public boolean sendGuestBroadcasts()
	{
		return sendGuestBroadcasts;
	}

	@Override
	public boolean sendSystemClanMessages()
	{
		return sendSystemClanMessages;
	}

	@Override
	public String approvedGuestUsernames()
	{
		return approvedGuestUsernames;
	}
}
