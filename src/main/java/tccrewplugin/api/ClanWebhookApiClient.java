package tccrewplugin.api;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import tccrewplugin.clanchat.model.ClanWebhookPayload;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ClanWebhookApiClient extends ApiClient
{
	private final ApiRequestExecutor executor;

	public ClanWebhookApiClient(OkHttpClient httpClient, Gson gson)
	{
		super(httpClient, gson);
		this.executor = new ApiRequestExecutor(httpClient, gson);
	}

	public CompletableFuture<ApiResult<String>> send(
		String endpoint,
		String secret,
		ClanWebhookAuthenticationMode mode,
		ClanWebhookPayload payload
	)
	{
		if (endpoint == null || endpoint.trim().isEmpty())
		{
			return CompletableFuture.completedFuture(new ApiResult<>(-1, null, "Endpoint is blank."));
		}

		Request.Builder builder = requestBuilder(endpoint)
			.post(createBody(mode, secret, payload))
			.header("Content-Type", "application/json");

		if (mode == ClanWebhookAuthenticationMode.BEARER_TOKEN)
		{
			builder.header("Authorization", "Bearer " + (secret == null ? "" : secret));
		}
		else if (mode == ClanWebhookAuthenticationMode.SECRET_HEADER)
		{
			builder.header("X-Clan-Webhook-Secret", secret == null ? "" : secret);
		}

		return executor.executePlain(builder.build());
	}

	private RequestBody createBody(ClanWebhookAuthenticationMode mode, String secret, ClanWebhookPayload payload)
	{
		if (mode == ClanWebhookAuthenticationMode.JSON_SECRET_FIELD)
		{
			Map<String, Object> envelope = new LinkedHashMap<>();
			envelope.put("secret", secret);
			envelope.put("payload", payload);
			return executor.jsonBody(envelope);
		}

		return executor.jsonBody(payload);
	}
}
