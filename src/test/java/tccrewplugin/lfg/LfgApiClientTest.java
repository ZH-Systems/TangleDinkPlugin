package tccrewplugin.lfg;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LfgApiClientTest
{
	@Test
	void addsExpectedHeadersOnConfigurationRequest() throws Exception
	{
		AtomicReference<Request> captured = new AtomicReference<>();
		OkHttpClient httpClient = new OkHttpClient.Builder()
			.addInterceptor(chain -> {
				captured.set(chain.request());
				ResponseBody body = ResponseBody.create(MediaType.get("application/json"), "{\"categories\":[]}");
				return new Response.Builder()
					.request(chain.request())
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body(body)
					.build();
			})
			.build();

		LfgApiClient client = new LfgApiClient(httpClient, new Gson());
		assertNotNull(client.fetchConfiguration("https://example.supabase.co", "secret-token", "Example Player|STANDARD", "1.0.0").get(5, TimeUnit.SECONDS));

		Request request = captured.get();
		assertNotNull(request);
		assertEquals("Bearer secret-token", request.header("Authorization"));
		assertEquals("Example Player|STANDARD", request.header("X-TcCrew-Player"));
		assertEquals("1.0.0", request.header("X-Plugin-Version"));
	}

	@Test
	void writesIncludeIdempotencyKey() throws Exception
	{
		AtomicReference<Request> captured = new AtomicReference<>();
		OkHttpClient httpClient = new OkHttpClient.Builder()
			.addInterceptor(chain -> {
				captured.set(chain.request());
				ResponseBody body = ResponseBody.create(MediaType.get("application/json"), "{\"success\":true}");
				return new Response.Builder()
					.request(chain.request())
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body(body)
					.build();
			})
			.build();

		LfgApiClient client = new LfgApiClient(httpClient, new Gson());
		client.actOnGroup("https://example.supabase.co", "secret-token", "Example Player|STANDARD", "1.0.0", new tccrewplugin.lfg.model.LfgActionRequest("join", "group-123", "request-123"))
			.get(5, TimeUnit.SECONDS);

		Request request = captured.get();
		assertNotNull(request);
		assertEquals("request-123", request.header("X-Idempotency-Key"));
	}

	@Test
	void normalizesFunctionUrlsBackToTheProjectOrigin() throws Exception
	{
		AtomicReference<Request> captured = new AtomicReference<>();
		OkHttpClient httpClient = new OkHttpClient.Builder()
			.addInterceptor(chain -> {
				captured.set(chain.request());
				ResponseBody body = ResponseBody.create(MediaType.get("application/json"), "{\"categories\":[]}");
				return new Response.Builder()
					.request(chain.request())
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body(body)
					.build();
			})
			.build();

		LfgApiClient client = new LfgApiClient(httpClient, new Gson());
		client.fetchConfiguration("https://example.supabase.co/functions/v1/lfg-groups", "secret-token", "Example Player|STANDARD", "1.0.0")
			.get(5, TimeUnit.SECONDS);

		Request request = captured.get();
		assertNotNull(request);
		assertEquals("https://example.supabase.co/functions/v1/lfg-config", request.url().toString());
	}

	@Test
	void serializesCreateRequestStartTimeAsIsoInstant() throws Exception
	{
		AtomicReference<Request> captured = new AtomicReference<>();
		OkHttpClient httpClient = new OkHttpClient.Builder()
			.addInterceptor(chain -> {
				captured.set(chain.request());
				ResponseBody body = ResponseBody.create(MediaType.get("application/json"), "{\"success\":true}");
				return new Response.Builder()
					.request(chain.request())
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body(body)
					.build();
			})
			.build();

		LfgApiClient client = new LfgApiClient(httpClient, new Gson());
		client.createGroup(
			"https://example.supabase.co",
			"secret-token",
			"Example Player|STANDARD",
			"1.0.0",
			"request-123",
			new tccrewplugin.lfg.model.CreateLfgGroupRequest("raid", "Theatre of Blood", "Learner friendly", "2026-08-04T20:00:00Z", 5)
		).get(5, TimeUnit.SECONDS);

		Request request = captured.get();
		assertNotNull(request);
		Buffer buffer = new Buffer();
		request.body().writeTo(buffer);
		String body = buffer.readUtf8();
		JsonObject json = new Gson().fromJson(body, JsonObject.class);
		assertEquals("2026-08-04T20:00:00Z", json.get("startTime").getAsString());
		assertTrue(json.has("categoryKey"));
	}
}
