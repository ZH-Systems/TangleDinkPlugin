package tccrewplugin.sync.webhook;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tccrewplugin.DinkPluginConfig;
import tccrewplugin.TcCrewPlugin;
import tccrewplugin.sync.model.CollectionLogSnapshot;
import tccrewplugin.sync.model.PersonalBestSummary;
import tccrewplugin.sync.model.SyncPayload;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Singleton
public class ClogPbWebhookClient
{
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_QUEUE_SIZE = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration BASE_RETRY_DELAY = Duration.ofSeconds(2);
    private static final int DISCORD_INLINE_JSON_LIMIT = 1800;
    private static final String DISCORD_JSON_FILENAME = "clog-pb-sync.json";

    private final OkHttpClient httpClient;
    private final ScheduledExecutorService executor;
    private final Gson gson;
    private final DinkPluginConfig config;
    private final Object queueLock = new Object();
    private final Deque<QueuedUpload> queue = new ArrayDeque<>();
    private final Deque<ScheduledFuture<?>> retryFutures = new ArrayDeque<>();
    private final SecureRandom random = new SecureRandom();

    private volatile boolean started;
    private volatile boolean shuttingDown;
    private volatile Call currentCall;

    @Inject
    public ClogPbWebhookClient(OkHttpClient httpClient, ScheduledExecutorService executor, Gson gson, DinkPluginConfig config)
    {
        this.httpClient = httpClient;
        this.executor = executor;
        this.gson = gson;
        this.config = config;
    }

    public void start()
    {
        started = true;
        shuttingDown = false;
    }

    public void shutdown()
    {
        shuttingDown = true;
        synchronized (queueLock)
        {
            queue.clear();
            if (currentCall != null)
            {
                currentCall.cancel();
                currentCall = null;
            }
            while (!retryFutures.isEmpty())
            {
                ScheduledFuture<?> future = retryFutures.pollFirst();
                if (future != null)
                {
                    future.cancel(false);
                }
            }
        }
    }

    public boolean hasPendingWork()
    {
        synchronized (queueLock)
        {
            return currentCall != null || !queue.isEmpty() || !retryFutures.isEmpty();
        }
    }

    public void clearQueuedBacklog()
    {
        synchronized (queueLock)
        {
            queue.clear();
            if (currentCall != null)
            {
                currentCall.cancel();
                currentCall = null;
            }
            while (!retryFutures.isEmpty())
            {
                ScheduledFuture<?> future = retryFutures.pollFirst();
                if (future != null)
                {
                    future.cancel(false);
                }
            }
        }
    }

    public boolean submit(SyncPayload payload, UploadPriority priority, Consumer<UploadOutcome> callback)
    {
        if (!started || shuttingDown)
        {
            return false;
        }

        WebhookTarget target = resolveTarget();
        if (target == null)
        {
            if (callback != null)
            {
                callback.accept(new UploadOutcome(false, 0, null, "invalid webhook configuration", false));
            }
            return false;
        }

        QueuedUpload upload = new QueuedUpload(payload, priority, callback, target);
        synchronized (queueLock)
        {
            if (queue.size() >= MAX_QUEUE_SIZE)
            {
                if (priority == UploadPriority.LOW)
                {
                    if (callback != null)
                    {
                        callback.accept(new UploadOutcome(false, 0, null, "queue full", false));
                    }
                    return false;
                }

                while (queue.size() >= MAX_QUEUE_SIZE && removeLastLowPriority() != null)
                {
                    // keep removing low-priority work until a manual upload fits
                }

                if (queue.size() >= MAX_QUEUE_SIZE)
                {
                    QueuedUpload dropped = queue.pollFirst();
                    if (dropped != null && log.isDebugEnabled())
                    {
                        log.debug("Dropped oldest queued upload to make room for a manual sync request");
                    }
                }
            }

            queue.addLast(upload);
            if (currentCall == null)
            {
                drainQueue();
            }
        }

        return true;
    }

    private QueuedUpload removeLastLowPriority()
    {
        QueuedUpload lastLow = null;
        for (QueuedUpload queuedUpload : queue)
        {
            if (queuedUpload.priority == UploadPriority.LOW)
            {
                lastLow = queuedUpload;
            }
        }

        if (lastLow != null)
        {
            queue.removeLastOccurrence(lastLow);
        }
        return lastLow;
    }

    private void drainQueue()
    {
        if (shuttingDown || currentCall != null)
        {
            return;
        }

        QueuedUpload upload = queue.pollFirst();
        if (upload == null)
        {
            return;
        }

        Request request = buildRequest(upload);
        if (request == null)
        {
            complete(upload, new UploadOutcome(false, upload.attempt, null, "invalid request", false));
            return;
        }

        currentCall = httpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .callTimeout(CALL_TIMEOUT)
            .build()
            .newCall(request);

        currentCall.enqueue(new okhttp3.Callback()
        {
            @Override
            public void onFailure(Call call, java.io.IOException e)
            {
                currentCall = null;
                if (call.isCanceled() || shuttingDown)
                {
                    synchronized (queueLock)
                    {
                        drainQueue();
                    }
                    return;
                }
                if (upload.attempt < MAX_ATTEMPTS)
                {
                    scheduleRetry(upload, null, null);
                }
                else
                {
                    complete(upload, new UploadOutcome(false, upload.attempt, null, e.getClass().getSimpleName(), false));
                }
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response ignored = response)
                {
                    currentCall = null;
                    if (call.isCanceled() || shuttingDown)
                    {
                        synchronized (queueLock)
                        {
                            drainQueue();
                        }
                        return;
                    }
                    int code = response.code();
                    if (code >= 200 && code < 300)
                    {
                        complete(upload, new UploadOutcome(true, upload.attempt, code, "ok", false));
                        return;
                    }

                    if (isRetryable(code) && upload.attempt < MAX_ATTEMPTS)
                    {
                        scheduleRetry(upload, code, response.header("Retry-After"));
                        return;
                    }

                    complete(upload, new UploadOutcome(false, upload.attempt, code, "HTTP " + code, false));
                }
            }
        });
    }

    private void scheduleRetry(QueuedUpload upload, Integer code, String retryAfterHeader)
    {
        upload.attempt++;
        long delay = computeDelayMs(upload.attempt, retryAfterHeader);
        ScheduledFuture<?> future = executor.schedule(() ->
        {
            if (shuttingDown)
            {
                return;
            }

            synchronized (queueLock)
            {
                retryFutures.removeIf(ScheduledFuture::isDone);
            }

            synchronized (queueLock)
            {
                queue.addFirst(upload);
                if (currentCall == null)
                {
                    drainQueue();
                }
            }
        }, delay, TimeUnit.MILLISECONDS);

        synchronized (queueLock)
        {
            retryFutures.addLast(future);
        }

        if (upload.callback != null)
        {
            upload.callback.accept(new UploadOutcome(false, upload.attempt - 1, code, "retry scheduled", true));
        }
    }

    private long computeDelayMs(int attempt, String retryAfterHeader)
    {
        if (retryAfterHeader != null)
        {
            try
            {
                long seconds = Long.parseLong(retryAfterHeader.trim());
                if (seconds > 0)
                {
                    return TimeUnit.SECONDS.toMillis(seconds);
                }
            }
            catch (NumberFormatException ignored)
            {
            }
        }

        long base = BASE_RETRY_DELAY.toMillis() * (1L << Math.max(0, attempt - 1));
        return base + random.nextInt(250);
    }

    private boolean isRetryable(int code)
    {
        return code == 408 || code == 429 || code == 500 || code == 502 || code == 503 || code == 504;
    }

    private void complete(QueuedUpload upload, UploadOutcome outcome)
    {
        if (upload.callback != null)
        {
            upload.callback.accept(outcome);
        }

        synchronized (queueLock)
        {
            if (currentCall == null)
            {
                drainQueue();
            }
        }
    }

    private WebhookTarget resolveTarget()
    {
        String raw = config.clogPbWebhookUrl();
        if (raw == null || raw.trim().isEmpty())
        {
            return null;
        }

        HttpUrl parsed = HttpUrl.parse(raw.trim());
        if (parsed == null)
        {
            return null;
        }

        if (!"https".equalsIgnoreCase(parsed.scheme()))
        {
            return null;
        }

        WebhookMode mode = isDiscordWebhook(parsed) ? WebhookMode.DISCORD : WebhookMode.RECEIVER;
        if (mode == WebhookMode.RECEIVER)
        {
            String token = config.clogPbWebhookToken();
            if (token == null || token.trim().isEmpty())
            {
                return null;
            }
        }

        return new WebhookTarget(parsed, mode);
    }

    private Request buildRequest(QueuedUpload upload)
    {
        if (upload.target.mode == WebhookMode.DISCORD)
        {
            return buildDiscordRequest(upload);
        }

        String bodyJson = gson.toJson(upload.payload);
        byte[] bodyBytes = bodyJson.getBytes(StandardCharsets.UTF_8);
        String contentHash = sha256(bodyBytes);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = java.util.UUID.randomUUID().toString();
        String signature = sign(upload.target.url, timestamp, nonce, contentHash);

        Request.Builder builder = new Request.Builder()
            .url(upload.target.url)
            .post(RequestBody.create(JSON, bodyJson))
            .header("User-Agent", TcCrewPlugin.USER_AGENT)
            .header("Authorization", "Bearer " + config.clogPbWebhookToken().trim())
            .header("X-Event-Id", upload.payload.getEventId())
            .header("X-Event-Type", upload.payload.getEventType())
            .header("X-Captured-At", upload.payload.getCapturedAt())
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .header("X-Content-SHA256", contentHash);

        if (signature != null)
        {
            builder.header("X-Signature", signature);
        }

        return builder.build();
    }

    private Request buildDiscordRequest(QueuedUpload upload)
    {
        String payloadJson = gson.toJson(upload.payload);
        JsonObject body = new JsonObject();
        body.addProperty("username", "Tangle Crew Plugin");
        JsonObject allowedMentions = new JsonObject();
        allowedMentions.add("parse", new JsonArray());
        body.add("allowed_mentions", allowedMentions);

        if (payloadJson.length() <= DISCORD_INLINE_JSON_LIMIT)
        {
            body.addProperty("content", "```json\n" + payloadJson + "\n```");
            return new Request.Builder()
                .url(upload.target.url)
                .post(RequestBody.create(JSON, gson.toJson(body)))
                .header("User-Agent", TcCrewPlugin.USER_AGENT)
                .build();
        }

        body.addProperty("content", "Raw JSON payload attached as " + DISCORD_JSON_FILENAME);
        MultipartBody multipart = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", gson.toJson(body))
            .addFormDataPart("files[0]", DISCORD_JSON_FILENAME, RequestBody.create(JSON, payloadJson))
            .build();

        return new Request.Builder()
            .url(upload.target.url)
            .post(multipart)
            .header("User-Agent", TcCrewPlugin.USER_AGENT)
            .build();
    }

    private boolean isDiscordWebhook(HttpUrl url)
    {
        String host = url.host().toLowerCase(java.util.Locale.ROOT);
        String path = url.encodedPath().toLowerCase(java.util.Locale.ROOT);
        return (host.endsWith("discord.com") || host.endsWith("discordapp.com"))
            && path.contains("/api/webhooks/");
    }

    private String sign(HttpUrl url, String timestamp, String nonce, String contentHash)
    {
        String secret = config.clogPbSigningSecret();
        if (secret == null || secret.trim().isEmpty())
        {
            return null;
        }

        try
        {
            String canonical = String.join("\n", "POST", url.encodedPath(), timestamp, nonce, contentHash);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception ex)
        {
            log.debug("Unable to sign sync request", ex);
            return null;
        }
    }

    private String sha256(byte[] bytes)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(bytes));
        }
        catch (Exception ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    private String toHex(byte[] bytes)
    {
        char[] hex = new char[bytes.length * 2];
        final char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++)
        {
            int b = bytes[i] & 0xff;
            hex[i * 2] = digits[b >>> 4];
            hex[i * 2 + 1] = digits[b & 0x0f];
        }
        return new String(hex);
    }

    private static final class QueuedUpload
    {
        private final SyncPayload payload;
        private final UploadPriority priority;
        private final Consumer<UploadOutcome> callback;
        private final WebhookTarget target;
        private int attempt = 1;

        private QueuedUpload(SyncPayload payload, UploadPriority priority, Consumer<UploadOutcome> callback, WebhookTarget target)
        {
            this.payload = Objects.requireNonNull(payload);
            this.priority = Objects.requireNonNull(priority);
            this.callback = callback;
            this.target = Objects.requireNonNull(target);
        }
    }

    private enum WebhookMode
    {
        RECEIVER,
        DISCORD
    }

    private static final class WebhookTarget
    {
        private final HttpUrl url;
        private final WebhookMode mode;

        private WebhookTarget(HttpUrl url, WebhookMode mode)
        {
            this.url = url;
            this.mode = mode;
        }
    }
}
