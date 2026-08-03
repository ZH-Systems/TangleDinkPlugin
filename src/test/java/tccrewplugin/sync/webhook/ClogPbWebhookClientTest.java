package tccrewplugin.sync.webhook;

import com.google.gson.Gson;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tccrewplugin.DinkPluginConfig;
import tccrewplugin.sync.model.PersonalBestRecord;
import tccrewplugin.sync.model.SyncAccountType;
import tccrewplugin.sync.model.SyncClientMetadata;
import tccrewplugin.sync.model.SyncPayload;
import tccrewplugin.sync.model.SyncPlayer;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ClogPbWebhookClientTest
{
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void tearDown()
    {
        executor.shutdownNow();
    }

    @Test
    void postsJsonWithBearerAndSignature() throws Exception
    {
        DinkPluginConfig config = Mockito.mock(DinkPluginConfig.class);
        when(config.clogPbWebhookUrl()).thenReturn("https://example.com/api");
        when(config.clogPbWebhookToken()).thenReturn("secret-token");
        when(config.clogPbSigningSecret()).thenReturn("signing-secret");

        AtomicReference<Request> requestRef = new AtomicReference<>();
        OkHttpClient httpClient = new OkHttpClient.Builder()
            .addInterceptor((Interceptor.Chain chain) ->
            {
                requestRef.set(chain.request());
                return new Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(204)
                    .message("No Content")
                    .body(ResponseBody.create(okhttp3.MediaType.parse("text/plain"), ""))
                    .build();
            })
            .build();

        ClogPbWebhookClient client = new ClogPbWebhookClient(httpClient, executor, new Gson(), config);
        client.start();

        SyncPayload payload = SyncPayload.of(
            "personal_bests.snapshot",
            "!pball",
            new SyncPlayer("Example Player", SyncAccountType.STANDARD),
            new SyncClientMetadata("1.12.33", "1.14.0"),
            null,
            null,
            Collections.singletonList(new PersonalBestRecord("zulrah", "Zulrah", null, null, 58_200L, "runelite-local-config"))
        );

        CompletableFuture<UploadOutcome> outcome = new CompletableFuture<>();
        assertTrue(client.submit(payload, UploadPriority.HIGH, outcome::complete));
        UploadOutcome result = outcome.get(5, TimeUnit.SECONDS);
        assertTrue(result.isSuccess());

        Request request = requestRef.get();
        assertNotNull(request);
        assertEquals("Bearer secret-token", request.header("Authorization"));
        assertEquals("personal_bests.snapshot", request.header("X-Event-Type"));
        assertNotNull(request.header("X-Event-Id"));
        assertNotNull(request.header("X-Captured-At"));

        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        String json = buffer.readUtf8();
        assertTrue(json.contains("\"eventType\":\"personal_bests.snapshot\""));
        assertTrue(request.header("X-Signature") != null && !request.header("X-Signature").isEmpty());
    }

    @Test
    void rejectsBlankUrl()
    {
        DinkPluginConfig config = Mockito.mock(DinkPluginConfig.class);
        when(config.clogPbWebhookUrl()).thenReturn("");
        when(config.clogPbWebhookToken()).thenReturn("secret-token");
        when(config.clogPbSigningSecret()).thenReturn("");

        ClogPbWebhookClient client = new ClogPbWebhookClient(new OkHttpClient(), executor, new Gson(), config);
        client.start();

        boolean accepted = client.submit(
            SyncPayload.of("test", "!pball", new SyncPlayer("Example", SyncAccountType.STANDARD), null, null, null, Collections.emptyList()),
            UploadPriority.HIGH,
            outcome -> { }
        );

        assertFalse(accepted);
    }

    @Test
    void discordWebhookAllowsBlankToken() throws Exception
    {
        DinkPluginConfig config = Mockito.mock(DinkPluginConfig.class);
        when(config.clogPbWebhookUrl()).thenReturn("https://discord.com/api/webhooks/123/abc");
        when(config.clogPbWebhookToken()).thenReturn("");
        when(config.clogPbSigningSecret()).thenReturn("");

        AtomicReference<Request> requestRef = new AtomicReference<>();
        OkHttpClient httpClient = new OkHttpClient.Builder()
            .addInterceptor((Interceptor.Chain chain) ->
            {
                requestRef.set(chain.request());
                return new Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(204)
                    .message("No Content")
                    .body(ResponseBody.create(okhttp3.MediaType.parse("text/plain"), ""))
                    .build();
            })
            .build();

        ClogPbWebhookClient client = new ClogPbWebhookClient(httpClient, executor, new Gson(), config);
        client.start();

        SyncPayload payload = SyncPayload.of(
            "player_data.snapshot",
            "!syncall",
            new SyncPlayer("Example Player", SyncAccountType.STANDARD),
            new SyncClientMetadata("1.12.33", "1.14.0"),
            new tccrewplugin.sync.model.CollectionLogSnapshot(
                tccrewplugin.sync.model.CollectionLogState.PARTIAL,
                "2026-08-03T03:15:00Z",
                3,
                2,
                10,
                1,
                1,
                Collections.singletonList(new tccrewplugin.sync.model.CollectionLogItem(123, "Dragon defender", 1, true, "Bosses", "Warriors' Guild"))
            ),
            new tccrewplugin.sync.model.PersonalBestSummary(1, 0, 0, 0),
            Collections.singletonList(new PersonalBestRecord("zulrah", "Zulrah", null, null, 58_200L, "runelite-local-config"))
        );

        CompletableFuture<UploadOutcome> outcome = new CompletableFuture<>();
        assertTrue(client.submit(payload, UploadPriority.HIGH, outcome::complete));
        UploadOutcome result = outcome.get(5, TimeUnit.SECONDS);
        assertTrue(result.isSuccess());

        Request request = requestRef.get();
        assertNotNull(request);
        assertEquals(null, request.header("Authorization"));

        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        String json = buffer.readUtf8();
        assertTrue(json.contains("\"content\""));
        assertFalse(json.contains("\"embeds\""));
        assertTrue(json.contains("```json"));
        assertTrue(json.contains("Dragon defender"));
        assertTrue(json.contains("Zulrah"));
    }
}
