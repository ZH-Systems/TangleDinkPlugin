package tccrewplugin.clanchat;

import com.google.gson.Gson;
import tccrewplugin.DinkPluginConfig;
import tccrewplugin.TcCrewPlugin;
import tccrewplugin.util.ConfigProxyAuth;
import tccrewplugin.util.ConfigProxyServer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Singleton
public class ClanWebhookService
{
	private static final int MAX_QUEUE_SIZE = 128;
	private static final int MAX_ATTEMPTS = 3;
	private static final Set<Integer> RETRY_STATUS = Set.of(429, 500, 502, 503, 504);

	private final DinkPluginConfig config;
	private final ScheduledExecutorService executor;
	private final OkHttpClient baseClient;
	private final Gson gson;
	private final ClanMessageDeduplicator deduplicator = new ClanMessageDeduplicator();
	private final Object queueLock = new Object();
	private final Deque<QueuedMessage> queue = new ArrayDeque<>();
	private final Set<ScheduledFuture<?>> scheduledTasks = ConcurrentHashMap.newKeySet();
	private final AtomicBoolean draining = new AtomicBoolean();
	private final AtomicBoolean running = new AtomicBoolean();

	private volatile OkHttpClient httpClient;
	private volatile String lastInvalidConfigKey;
	private volatile long lastQueueWarningNanos;

	@Inject
	public ClanWebhookService(DinkPluginConfig config, ScheduledExecutorService executor, OkHttpClient baseClient, Gson gson)
	{
		this.config = config;
		this.executor = executor;
		this.baseClient = baseClient;
		this.gson = gson;
		rehydrateClient();
	}

	public void startUp()
	{
		running.set(true);
		lastInvalidConfigKey = null;
		lastQueueWarningNanos = 0L;
		deduplicator.clear();
		synchronized (queueLock)
		{
			queue.clear();
		}
		cancelScheduledTasks();
		rehydrateClient();
	}

	public void shutDown()
	{
		running.set(false);
		cancelScheduledTasks();
		deduplicator.clear();
		synchronized (queueLock)
		{
			queue.clear();
		}
		draining.set(false);
		lastInvalidConfigKey = null;
	}

	public void onConfigChanged(String key)
	{
		if ("requestTimeoutSeconds".equals(key))
		{
			rehydrateClient();
		}

		if ("secret".equals(key) || "webhook_endpoint".equals(key) || "requestTimeoutSeconds".equals(key))
		{
			lastInvalidConfigKey = null;
		}
	}

	public boolean submit(ClanMessageEvent event)
	{
		if (event == null || !running.get())
		{
			return false;
		}

		if (!isValidConfig())
		{
			return false;
		}

		if (!deduplicator.recordIfNew(buildDuplicateKey(event)))
		{
			return false;
		}

		if (!enqueue(event, isHighPriority(event), 0, false, true))
		{
			return false;
		}

		scheduleDrain();
		return true;
	}

	private boolean enqueue(ClanMessageEvent event, boolean highPriority, int attempt, boolean retry, boolean logOnDrop)
	{
		synchronized (queueLock)
		{
			if (queue.size() >= MAX_QUEUE_SIZE)
			{
				if (!highPriority)
				{
					if (logOnDrop)
					{
						logQueueDrop(event, "queue is full");
					}
					return false;
				}

				if (!dropOldestLowPriority())
				{
					queue.pollFirst();
				}
			}

			QueuedMessage queuedMessage = new QueuedMessage(event, highPriority, attempt);
			if (retry)
			{
				queue.addFirst(queuedMessage);
			}
			else
			{
				queue.addLast(queuedMessage);
			}
			return true;
		}
	}

	private boolean dropOldestLowPriority()
	{
		QueuedMessage[] messages = queue.toArray(new QueuedMessage[0]);
		for (QueuedMessage queuedMessage : messages)
		{
			if (!queuedMessage.highPriority)
			{
				queue.remove(queuedMessage);
				logQueueDrop(queuedMessage.event, "queue is full");
				return true;
			}
		}
		return false;
	}

	private void scheduleDrain()
	{
		if (!running.get() || !draining.compareAndSet(false, true))
		{
			return;
		}

		executor.execute(this::drainQueue);
	}

	private void drainQueue()
	{
		if (!running.get())
		{
			draining.set(false);
			return;
		}

		QueuedMessage next;
		synchronized (queueLock)
		{
			next = queue.pollFirst();
		}

		if (next == null)
		{
			draining.set(false);
			return;
		}

		send(next);
	}

	private void send(QueuedMessage queuedMessage)
	{
		if (!running.get())
		{
			draining.set(false);
			return;
		}

		HttpUrl url = buildWebhookUrl();
		if (url == null)
		{
			draining.set(false);
			return;
		}

		Request request = new Request.Builder()
			.url(url)
			.post(new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("data", gson.toJson(queuedMessage.event))
				.build())
			.build();

		newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				if (!running.get())
				{
					draining.set(false);
					return;
				}

				if (queuedMessage.attempt < MAX_ATTEMPTS - 1)
				{
					scheduleRetry(queuedMessage.event, queuedMessage.highPriority, queuedMessage.attempt + 1, failureDelayMillis(queuedMessage.attempt));
				}
				else
				{
					log.warn("Clan chat webhook failed after {} attempts: {}", queuedMessage.attempt + 1, e.getMessage());
				}

				finishAttempt();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (Response ignored = response)
				{
					if (response.isSuccessful())
					{
						if (config.debugLogging())
						{
							log.debug("Sent clan chat webhook event type={} classification={}",
								queuedMessage.event.getChatMessageType(),
								queuedMessage.event.getSystemMessageType());
						}
					}
					else if (shouldRetry(response.code()) && queuedMessage.attempt < MAX_ATTEMPTS - 1)
					{
						log.warn("Clan chat webhook returned HTTP {} for message type {}",
							response.code(),
							queuedMessage.event.getSystemMessageType());
						scheduleRetry(queuedMessage.event, queuedMessage.highPriority, queuedMessage.attempt + 1, failureDelayMillis(queuedMessage.attempt));
					}
					else if (!response.isSuccessful())
					{
						log.warn("Clan chat webhook returned HTTP {} for message type {}",
							response.code(),
							queuedMessage.event.getSystemMessageType());
					}
				}
				finally
				{
					finishAttempt();
				}
			}
		});
	}

	private void finishAttempt()
	{
		draining.set(false);
		scheduleDrain();
	}

	protected void scheduleRetry(ClanMessageEvent event, boolean highPriority, int nextAttempt, long delayMillis)
	{
		if (!running.get())
		{
			return;
		}

		final ScheduledFuture<?>[] futureRef = new ScheduledFuture<?>[1];
		ScheduledFuture<?> future = executor.schedule(() ->
		{
			scheduledTasks.remove(futureRef[0]);
			if (!running.get())
			{
				return;
			}

			enqueue(event, highPriority, nextAttempt, true, false);
			scheduleDrain();
			scheduledTasks.remove(futureRef[0]);
		}, delayMillis, TimeUnit.MILLISECONDS);

		futureRef[0] = future;
		scheduledTasks.add(future);
	}

	private long failureDelayMillis(int attempt)
	{
		return 1000L << Math.min(attempt, 2);
	}

	private boolean shouldRetry(int code)
	{
		return RETRY_STATUS.contains(code);
	}

	private boolean isValidConfig()
	{
		String secret = StringUtils.trimToEmpty(config.secretKey());
		String endpoint = StringUtils.trimToEmpty(config.webhookEndpoint());
		String signature = secret + "|" + endpoint;

		if (secret.isEmpty() || endpoint.isEmpty())
		{
			logInvalidConfigOnce(signature, "Clan chat webhook is not configured");
			return false;
		}

		HttpUrl url = HttpUrl.parse(endpoint);
		if (url == null || !isAllowedScheme(url))
		{
			logInvalidConfigOnce(signature, "Clan chat webhook endpoint is invalid");
			return false;
		}

		lastInvalidConfigKey = null;
		return true;
	}

	private void logInvalidConfigOnce(String signature, String message)
	{
		if (!signature.equals(lastInvalidConfigKey))
		{
			lastInvalidConfigKey = signature;
			log.warn(message);
		}
	}

	protected HttpUrl buildWebhookUrl()
	{
		String secret = StringUtils.trimToEmpty(config.secretKey());
		String endpoint = StringUtils.trimToEmpty(config.webhookEndpoint());
		HttpUrl base = HttpUrl.parse(endpoint);
		if (base == null || !isAllowedScheme(base) || secret.isEmpty())
		{
			return null;
		}

		return base.newBuilder()
			.addPathSegment("webhook")
			.addPathSegment(secret)
			.build();
	}

	private boolean isAllowedScheme(HttpUrl url)
	{
		String scheme = url.scheme().toLowerCase(Locale.ENGLISH);
		if ("https".equals(scheme))
		{
			return true;
		}

		if (!"http".equals(scheme))
		{
			return false;
		}

		String host = StringUtils.defaultString(url.host()).toLowerCase(Locale.ENGLISH);
		return "localhost".equals(host)
			|| "127.0.0.1".equals(host)
			|| "::1".equals(host)
			|| "0.0.0.0".equals(host);
	}

	private void rehydrateClient()
	{
		int timeout = Math.max(1, config.requestTimeoutSeconds());
		this.httpClient = baseClient.newBuilder()
			.connectTimeout(timeout, TimeUnit.SECONDS)
			.readTimeout(timeout, TimeUnit.SECONDS)
			.writeTimeout(timeout, TimeUnit.SECONDS)
			.proxySelector(new ConfigProxyServer(config))
			.proxyAuthenticator(new ConfigProxyAuth(config))
			.addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
				.header("User-Agent", TcCrewPlugin.USER_AGENT)
				.build()))
			.build();
	}

	private void cancelScheduledTasks()
	{
		scheduledTasks.forEach(task -> task.cancel(false));
		scheduledTasks.clear();
	}

	private void logQueueDrop(ClanMessageEvent event, String reason)
	{
		long now = System.nanoTime();
		if (now - lastQueueWarningNanos < TimeUnit.SECONDS.toNanos(30))
		{
			return;
		}

		lastQueueWarningNanos = now;
		log.warn("Dropping clan chat webhook event type={} reason={}", event.getSystemMessageType(), reason);
	}

	private boolean isHighPriority(ClanMessageEvent event)
	{
		SystemMessageType type = event.getSystemMessageType();
		return type != SystemMessageType.NORMAL && type != SystemMessageType.UNKNOWN;
	}

	private String buildDuplicateKey(ClanMessageEvent event)
	{
		return String.join("|",
			StringUtils.defaultString(event.getChatMessageType()),
			StringUtils.defaultString(event.getAuthor()),
			StringUtils.defaultString(event.getContent()),
			String.valueOf(event.getTimestamp()));
	}

	private static final class QueuedMessage
	{
		private final ClanMessageEvent event;
		private final boolean highPriority;
		private final int attempt;

		private QueuedMessage(ClanMessageEvent event, boolean highPriority, int attempt)
		{
			this.event = event;
			this.highPriority = highPriority;
			this.attempt = attempt;
		}
	}

	protected Call newCall(Request request)
	{
		return httpClient.newCall(request);
	}
}
