package tccrewplugin.api;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import tccrewplugin.PluginConstants;
import tccrewplugin.sync.model.PlayerSubmission;

import java.util.concurrent.CompletableFuture;

public class PlayerSyncApiClient extends ApiClient
{
	private final ApiRequestExecutor executor;

	public PlayerSyncApiClient(OkHttpClient httpClient, Gson gson)
	{
		super(httpClient, gson);
		this.executor = new ApiRequestExecutor(httpClient, gson);
	}

	public CompletableFuture<ApiResult<String>> submit(String baseUrl, String apiToken, PlayerSubmission submission)
	{
		okhttp3.HttpUrl url = parseUrl(baseUrl, "/api/sync/submit");
		if (url == null)
		{
			return CompletableFuture.completedFuture(new ApiResult<>(-1, null, "Invalid API base URL."));
		}

		RequestBody body = executor.jsonBody(submission);
		Request request = requestBuilder(url.toString())
			.post(body)
			.header("Authorization", "Bearer " + (apiToken == null ? "" : apiToken))
			.header("Content-Type", "application/json")
			.build();
		return executor.executePlain(request);
	}
}
