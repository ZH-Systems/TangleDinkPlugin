package tccrewplugin.api;

import com.google.gson.Gson;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public abstract class ApiClient
{
	protected final OkHttpClient httpClient;
	protected final Gson gson;

	protected ApiClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
	}

	protected HttpUrl parseUrl(String baseUrl, String path)
	{
		HttpUrl base = HttpUrl.parse(baseUrl);
		if (base == null)
		{
			return null;
		}
		return base.newBuilder().addPathSegments(path.startsWith("/") ? path.substring(1) : path).build();
	}

	protected Request.Builder requestBuilder(String url)
	{
		return new Request.Builder().url(url);
	}
}
