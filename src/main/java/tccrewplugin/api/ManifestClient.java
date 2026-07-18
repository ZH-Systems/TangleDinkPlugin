package tccrewplugin.api;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import tccrewplugin.sync.model.SyncManifest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ManifestClient extends ApiClient
{
	private final ApiRequestExecutor executor;

	public ManifestClient(OkHttpClient httpClient, Gson gson)
	{
		super(httpClient, gson);
		this.executor = new ApiRequestExecutor(httpClient, gson);
	}

	public CompletableFuture<SyncManifest> fetchManifest(String baseUrl)
	{
		if (baseUrl == null || baseUrl.trim().isEmpty())
		{
			return CompletableFuture.failedFuture(new IllegalArgumentException("API base URL is blank."));
		}

		okhttp3.HttpUrl url = parseUrl(baseUrl, "/api/sync/manifest");
		if (url == null)
		{
			return CompletableFuture.failedFuture(new IllegalArgumentException("API base URL is invalid."));
		}

		Request request = requestBuilder(url.toString()).get().build();
		return executor.executePlain(request).thenApply(result -> {
			if (!result.isSuccess())
			{
				throw new IllegalArgumentException("Manifest request failed with HTTP " + result.getStatusCode() + (result.getError() == null ? "" : ": " + result.getError()));
			}

			try
			{
				SyncManifest manifest = gson.fromJson(result.getBody(), SyncManifest.class);
				return validate(manifest);
			}
			catch (JsonParseException ex)
			{
				throw new IllegalArgumentException("Manifest response was malformed.", ex);
			}
		});
	}

	private SyncManifest validate(SyncManifest manifest)
	{
		if (manifest == null)
		{
			throw new IllegalArgumentException("Manifest response was empty.");
		}

		List<Integer> varbits = manifest.getVarbits() == null ? List.of() : new ArrayList<>(manifest.getVarbits());
		List<Integer> varps = manifest.getVarps() == null ? List.of() : new ArrayList<>(manifest.getVarps());
		List<Integer> clogs = manifest.getCollectionLogItems() == null ? List.of() : new ArrayList<>(manifest.getCollectionLogItems());
		return new SyncManifest(manifest.getVersion(), varbits, varps, clogs);
	}
}
