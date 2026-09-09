package tccrewplugin.clanchat;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import tccrewplugin.api.ApiResult;
import tccrewplugin.api.ClanWebhookApiClient;
import tccrewplugin.api.ClanWebhookAuthenticationMode;
import tccrewplugin.clanchat.model.ClanWebhookPayload;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ClanWebhookDispatcher
{
	private final ClanWebhookApiClient apiClient;
	private final AtomicBoolean inFlight = new AtomicBoolean();

	public ClanWebhookDispatcher(OkHttpClient httpClient, Gson gson)
	{
		this.apiClient = new ClanWebhookApiClient(httpClient, gson);
	}

	public CompletableFuture<ApiResult<String>> send(
		String endpoint,
		String secret,
		ClanWebhookAuthenticationMode mode,
		ClanWebhookPayload payload
	)
	{
		if (!inFlight.compareAndSet(false, true))
		{
			return CompletableFuture.completedFuture(new ApiResult<>(-1, null, "delivery already in flight"));
		}
		CompletableFuture<ApiResult<String>> future = apiClient.send(endpoint, secret, mode, payload);
		future.whenComplete((result, throwable) -> inFlight.set(false));
		return future;
	}

	public boolean isBusy()
	{
		return inFlight.get();
	}
}
