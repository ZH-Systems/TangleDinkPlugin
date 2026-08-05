package tccrewplugin.lfg;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.RuneScapeProfileType;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import tccrewplugin.api.ApiClient;
import tccrewplugin.api.ApiRequestExecutor;
import tccrewplugin.api.ApiResult;
import tccrewplugin.lfg.model.CreateLfgGroupRequest;
import tccrewplugin.lfg.model.LfgActionRequest;
import tccrewplugin.lfg.model.LfgActionResponse;
import tccrewplugin.lfg.model.LfgConfigurationResponse;
import tccrewplugin.lfg.model.LfgGroupsResponse;
import tccrewplugin.sync.model.PlayerIdentity;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class LfgApiClient extends ApiClient
{
	private final ApiRequestExecutor executor;

	public LfgApiClient(OkHttpClient httpClient, Gson gson)
	{
		super(httpClient, gson);
		this.executor = new ApiRequestExecutor(httpClient, gson);
	}

	public CompletableFuture<ApiResult<LfgConfigurationResponse>> fetchConfiguration(String baseUrl, String apiToken, String playerHeader, String pluginVersion)
	{
		HttpUrl url = parseUrl(normalizeBaseUrl(baseUrl), "/functions/v1/lfg-config");
		if (url == null)
		{
			return CompletableFuture.completedFuture(new ApiResult<>(-1, null, "Invalid LFG Supabase URL."));
		}
		if (log.isDebugEnabled())
		{
			log.debug("LFG config request URL: {}", url);
		}
		Request request = requestBuilder(url.toString())
			.get()
			.header("Authorization", bearer(apiToken))
			.header("Content-Type", "application/json")
			.header("X-TcCrew-Player", safe(playerHeader))
			.header("X-Plugin-Version", safe(pluginVersion))
			.build();
		return executor.execute(request, LfgConfigurationResponse.class);
	}

	public CompletableFuture<ApiResult<LfgGroupsResponse>> fetchGroups(String baseUrl, String apiToken, String playerHeader, String pluginVersion)
	{
		HttpUrl url = parseUrl(normalizeBaseUrl(baseUrl), "/functions/v1/lfg-groups");
		if (url == null)
		{
			return CompletableFuture.completedFuture(new ApiResult<>(-1, null, "Invalid LFG Supabase URL."));
		}
		if (log.isDebugEnabled())
		{
			log.debug("LFG groups request URL: {}", url);
		}
		Request request = requestBuilder(url.toString())
			.get()
			.header("Authorization", bearer(apiToken))
			.header("Content-Type", "application/json")
			.header("X-TcCrew-Player", safe(playerHeader))
			.header("X-Plugin-Version", safe(pluginVersion))
			.build();
		return executor.execute(request, LfgGroupsResponse.class);
	}

	public CompletableFuture<ApiResult<LfgActionResponse>> createGroup(String baseUrl, String apiToken, String playerHeader, String pluginVersion, String idempotencyKey, CreateLfgGroupRequest requestBody)
	{
		HttpUrl url = parseUrl(normalizeBaseUrl(baseUrl), "/functions/v1/lfg-groups");
		if (url == null)
		{
			return CompletableFuture.completedFuture(new ApiResult<>(-1, null, "Invalid LFG Supabase URL."));
		}
		RequestBody body = executor.jsonBody(requestBody);
		Request request = requestBuilder(url.toString())
			.post(body)
			.header("Authorization", bearer(apiToken))
			.header("Content-Type", "application/json")
			.header("X-TcCrew-Player", safe(playerHeader))
			.header("X-Idempotency-Key", safe(idempotencyKey))
			.header("X-Plugin-Version", safe(pluginVersion))
			.build();
		return executor.execute(request, LfgActionResponse.class);
	}

	public CompletableFuture<ApiResult<LfgActionResponse>> actOnGroup(String baseUrl, String apiToken, String playerHeader, String pluginVersion, LfgActionRequest requestBody)
	{
		HttpUrl url = parseUrl(normalizeBaseUrl(baseUrl), "/functions/v1/lfg-group-action");
		if (url == null)
		{
			return CompletableFuture.completedFuture(new ApiResult<>(-1, null, "Invalid LFG Supabase URL."));
		}
		RequestBody body = executor.jsonBody(requestBody);
		Request request = requestBuilder(url.toString())
			.post(body)
			.header("Authorization", bearer(apiToken))
			.header("Content-Type", "application/json")
			.header("X-TcCrew-Player", safe(playerHeader))
			.header("X-Idempotency-Key", safe(requestBody == null ? null : requestBody.getIdempotencyKey()))
			.header("X-Plugin-Version", safe(pluginVersion))
			.build();
		return executor.execute(request, LfgActionResponse.class);
	}

	private String bearer(String token)
	{
		return "Bearer " + safe(token);
	}

	private String safe(String value)
	{
		return value == null ? "" : value;
	}

	private String normalizeBaseUrl(String baseUrl)
	{
		HttpUrl url = HttpUrl.parse(baseUrl);
		if (url == null)
		{
			return baseUrl;
		}
		return url.newBuilder()
			.encodedPath("/")
			.query(null)
			.fragment(null)
			.build()
			.toString();
	}
}
