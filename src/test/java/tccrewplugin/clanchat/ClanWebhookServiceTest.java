package tccrewplugin.clanchat;

import com.google.gson.Gson;
import tccrewplugin.DinkPluginConfig;
import net.runelite.api.ChatMessageType;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClanWebhookServiceTest
{
	private DinkPluginConfig config;
	private ScheduledExecutorService executor;
	private RecordingClanWebhookService service;
	private final AtomicReference<Callback> callbackRef = new AtomicReference<>();
	private final AtomicReference<Call> callRef = new AtomicReference<>();

	@BeforeEach
	void setUp()
	{
		config = mock(DinkPluginConfig.class);
		when(config.secretKey()).thenReturn("secret");
		when(config.webhookEndpoint()).thenReturn("https://example.com/api/");
		when(config.requestTimeoutSeconds()).thenReturn(10);
		when(config.debugLogging()).thenReturn(false);

		executor = mock(ScheduledExecutorService.class);
		doAnswer(invocation ->
		{
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).when(executor).execute(any(Runnable.class));

		service = new RecordingClanWebhookService(config, executor, new OkHttpClient(), new Gson());
		service.startUp();
	}

	@Test
	void buildWebhookUrlPreservesPathPrefixAndTrailingSlash()
	{
		assertEquals("https://example.com/api/webhook/secret", service.buildWebhookUrl().toString());
	}

	@Test
	void submitBuildsMultipartFieldAndJson()
	{
		ClanMessageEvent event = sampleEvent();
		assertTrue(service.submit(event));

		Request request = service.lastRequest;
		assertNotNull(request);
		assertEquals("POST", request.method());
		assertEquals("https://example.com/api/webhook/secret", request.url().toString());

		Buffer buffer = new Buffer();
		try
		{
			request.body().writeTo(buffer);
		}
		catch (IOException e)
		{
			throw new AssertionError(e);
		}

		String body = buffer.readUtf8();
		assertTrue(body.contains("name=\"data\""));
		assertTrue(body.contains("\"author\":\"Example Player\""));
		assertTrue(body.contains("\"systemMessageType\":\"COLLECTION_LOG\""));
		assertTrue(body.contains("\"chatMessageType\":\"CLAN_MESSAGE\""));
	}

	@Test
	void successfulResponseDoesNotRetry() throws IOException
	{
		ClanMessageEvent event = sampleEvent();
		service.submit(event);

		Callback callback = callbackRef.get();
		assertNotNull(callback);
		callback.onResponse(callRef.get(), responseFor(service.lastRequest, 204));

		assertEquals(0, service.retryCount);
	}

	@ParameterizedTest
	@ValueSource(ints = {429, 500, 502, 503, 504})
	void retryableStatusSchedulesRetry(int status) throws IOException
	{
		ClanMessageEvent event = sampleEvent();
		service.submit(event);

		Callback callback = callbackRef.get();
		assertNotNull(callback);
		callback.onResponse(callRef.get(), responseFor(service.lastRequest, status));

		assertEquals(1, service.retryCount);
		assertEquals(1, service.retryAttempt);
		assertEquals(1000L, service.retryDelayMillis);
	}

	@Test
	void badRequestDoesNotRetry() throws IOException
	{
		ClanMessageEvent event = sampleEvent();
		service.submit(event);

		Callback callback = callbackRef.get();
		callback.onResponse(callRef.get(), responseFor(service.lastRequest, 400));

		assertEquals(0, service.retryCount);
	}

	@Test
	void networkFailureSchedulesRetry()
	{
		ClanMessageEvent event = sampleEvent();
		service.submit(event);

		Callback callback = callbackRef.get();
		callback.onFailure(callRef.get(), new IOException("boom"));

		assertEquals(1, service.retryCount);
		assertEquals(1, service.retryAttempt);
		assertEquals(1000L, service.retryDelayMillis);
	}

	@Test
	void shutdownPreventsRetry()
	{
		ClanMessageEvent event = sampleEvent();
		service.submit(event);
		service.shutDown();

		Callback callback = callbackRef.get();
		callback.onFailure(callRef.get(), new IOException("boom"));

		assertEquals(0, service.retryCount);
	}

	@Test
	void submitAfterShutdownIsRejected()
	{
		service.shutDown();
		assertFalse(service.submit(sampleEvent()));
	}

	private ClanMessageEvent sampleEvent()
	{
		ClanMessageEvent event = new ClanMessageEvent(
			"Example Player",
			"Example Player received a new collection log item: Dragon defender",
			AccountType.IRON,
			SystemMessageType.COLLECTION_LOG,
			null,
			1775160000);
		event.setChatMessageType(ChatMessageType.CLAN_MESSAGE.name());
		event.setEventId("event-id");
		return event;
	}

	private Response responseFor(Request request, int code)
	{
		return new Response.Builder()
			.request(request)
			.protocol(Protocol.HTTP_1_1)
			.code(code)
			.message("test")
			.body(ResponseBody.create(MediaType.parse("text/plain"), ""))
			.build();
	}

	private final class RecordingClanWebhookService extends ClanWebhookService
	{
		private Request lastRequest;
		private int retryCount;
		private int retryAttempt;
		private long retryDelayMillis;

		private RecordingClanWebhookService(DinkPluginConfig config, ScheduledExecutorService executor, OkHttpClient baseClient, Gson gson)
		{
			super(config, executor, baseClient, gson);
		}

		@Override
		protected Call newCall(Request request)
		{
			lastRequest = request;
			Call call = mock(Call.class);
			doAnswer(invocation ->
			{
				callbackRef.set(invocation.getArgument(0));
				return null;
			}).when(call).enqueue(any());
			callRef.set(call);
			return call;
		}

		@Override
		protected void scheduleRetry(ClanMessageEvent event, boolean highPriority, int nextAttempt, long delayMillis)
		{
			retryCount++;
			retryAttempt = nextAttempt;
			retryDelayMillis = delayMillis;
		}
	}
}
