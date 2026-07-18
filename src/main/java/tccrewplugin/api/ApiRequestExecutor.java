package tccrewplugin.api;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ApiRequestExecutor
{
	public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

	private final OkHttpClient httpClient;
	private final Gson gson;

	public ApiRequestExecutor(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
	}

	public <T> CompletableFuture<ApiResult<T>> execute(Request request, Type responseType)
	{
		CompletableFuture<ApiResult<T>> future = new CompletableFuture<>();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				future.complete(new ApiResult<>(-1, null, sanitizeError(e)));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					String bodyText = body == null ? "" : body.string();
					T parsed = null;
					if (!bodyText.isEmpty() && responseType != null)
					{
						if (String.class.equals(responseType))
						{
							parsed = (T) bodyText;
						}
						else
						{
							parsed = gson.fromJson(bodyText, responseType);
						}
					}
					future.complete(new ApiResult<>(response.code(), parsed, response.isSuccessful() ? null : sanitizeBody(bodyText)));
				}
				catch (Exception ex)
				{
					future.complete(new ApiResult<>(response.code(), null, sanitizeError(ex)));
				}
			}
		});
		return future;
	}

	public CompletableFuture<ApiResult<String>> executePlain(Request request)
	{
		return execute(request, String.class);
	}

	public RequestBody jsonBody(Object value)
	{
		return RequestBody.create(JSON, gson.toJson(value));
	}

	private static String sanitizeBody(String body)
	{
		if (body == null || body.isEmpty())
		{
			return "empty response";
		}
		return body.length() > 512 ? body.substring(0, 512) + "…" : body;
	}

	private static String sanitizeError(Throwable error)
	{
		String message = error == null ? "unknown error" : error.getMessage();
		if (message == null || message.isEmpty())
		{
			return "unknown error";
		}
		return message.length() > 256 ? message.substring(0, 256) + "…" : message;
	}
}
