package tccrewplugin.clanchat;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanChannel;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.RuneScapeProfileType;
import okhttp3.OkHttpClient;
import tccrewplugin.PluginConstants;
import tccrewplugin.TangleDinkConfig;
import tccrewplugin.api.ApiResult;
import tccrewplugin.api.ClanWebhookAuthenticationMode;
import tccrewplugin.clanchat.model.ClanMessageRecord;
import tccrewplugin.clanchat.model.ClanMessageType;
import tccrewplugin.clanchat.model.ClanWebhookEventType;
import tccrewplugin.clanchat.model.ClanWebhookPayload;
import tccrewplugin.clanchat.model.ClanWebhookPayload.Clan;
import tccrewplugin.clanchat.model.ClanWebhookPayload.Message;
import tccrewplugin.clanchat.model.ClanWebhookPayload.Player;
import tccrewplugin.clanchat.model.ClanWebhookStatus;
import tccrewplugin.util.TextSanitizer;

import javax.inject.Singleton;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Singleton
public class ClanChatService
{
	private final Client client;
	private final ClientThread clientThread;
	private final TangleDinkConfig config;
	private final Gson gson;
	private final ScheduledExecutorService executor;
	private final ClanMessageClassifier classifier = new ClanMessageClassifier();
	private final ClanMessageFilter filter = new ClanMessageFilter();
	private volatile ClanMessageQueue queue;
	private final ClanMessageSanitizer sanitizer = new ClanMessageSanitizer();
	private final ClanWebhookDispatcher dispatcher;
	private final ClanMemberResolver clanMemberResolver = new ClanMemberResolver();
	private final AtomicBoolean shutdown = new AtomicBoolean();
	private final AtomicBoolean processing = new AtomicBoolean();
	private final AtomicReference<String> currentClanName = new AtomicReference<>();
	private final Deque<ClanMessageRecord> localHistory = new ArrayDeque<>();

	private volatile Instant lastCapturedMessageTime;
	private volatile Instant lastDeliveryAttempt;
	private volatile Instant lastSuccessfulDelivery;
	private volatile Integer lastHttpStatus;
	private volatile String lastError;
	private final AtomicInteger deliveredCount = new AtomicInteger();
	private final AtomicInteger filteredCount = new AtomicInteger();
	private final AtomicInteger failedCount = new AtomicInteger();
	private final AtomicInteger droppedCount = new AtomicInteger();
	private final AtomicInteger retriedCount = new AtomicInteger();
	private volatile boolean deliveriesPaused;

	public ClanChatService(Client client, ClientThread clientThread, TangleDinkConfig config, Gson gson, ScheduledExecutorService executor, OkHttpClient httpClient)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.gson = gson;
		this.executor = executor;
		this.queue = new ClanMessageQueue(config.webhookQueueCapacity());
		this.dispatcher = new ClanWebhookDispatcher(httpClient, gson);
	}

	public void startUp()
	{
		shutdown.set(false);
		deliveriesPaused = !config.clanWebhookEnabled();
	}

	public void shutDown()
	{
		shutdown.set(true);
		queue.clear();
		filter.clear();
		localHistory.clear();
	}

	public void onConfigChanged(String key)
	{
		if ("clanWebhookEnabled".equals(key))
		{
			deliveriesPaused = !config.clanWebhookEnabled();
		}
		else if ("webhookQueueCapacity".equals(key))
		{
			rebuildQueue();
		}
	}

	public void onGameStateChanged(GameState gameState)
	{
		if (gameState != GameState.LOGGED_IN)
		{
			currentClanName.set(null);
		}
	}

	public void onClanChannelChanged(ClanChannel clanChannel)
	{
		currentClanName.set(clanMemberResolver.resolveClanName(clanChannel));
	}

	public void onChatMessage(String chatTypeName, String sender, String senderRank, String message, boolean guest)
	{
		if (shutdown.get())
		{
			return;
		}

		ClanMessageRecord record = classifier.classify(
			chatTypeName,
			sender,
			senderRank,
			currentClanName.get(),
			message,
			client.getGameState() == GameState.LOGGED_IN ? client.getWorld() : null,
			guest,
			Instant.now(),
			false
		);
		lastCapturedMessageTime = record.getOccurredAt();

		ClanMessageFilter.Decision decision = filter.allow(config, record, client.getGameState() == GameState.LOGGED_IN, currentClanName.get());
		if (!decision.isAccepted())
		{
			filteredCount.incrementAndGet();
			appendHistory(record, ClanWebhookEventType.CLAN_CHAT_MESSAGE.name(), ClanMessageType.UNKNOWN, ClanWebhookStatus.FILTERED, null);
			if (config.debugLogging())
			{
				log.debug("Filtered clan message: {}", decision.getReason());
			}
			return;
		}

		if (!queue.offer(record))
		{
			droppedCount.incrementAndGet();
			appendHistory(record, ClanWebhookEventType.CLAN_CHAT_MESSAGE.name(), record.getType(), ClanWebhookStatus.DROPPED, null);
			return;
		}

		appendHistory(record, ClanWebhookEventType.CLAN_CHAT_MESSAGE.name(), record.getType(), ClanWebhookStatus.QUEUED, null);
		processQueue();
	}

	public void enqueueTestWebhook()
	{
		if (!config.clanWebhookEnabled())
		{
			lastError = "clan webhooks disabled";
			return;
		}
		ClanMessageRecord record = classifier.classify("TEST", "Tangle Dink", null, currentClanName.get(), "Webhook test message", client.getWorld(), false, Instant.now(), true);
		if (!queue.offer(record))
		{
			lastError = "webhook queue full";
			return;
		}
		appendHistory(record, ClanWebhookEventType.TEST.name(), record.getType(), ClanWebhookStatus.QUEUED, null);
		processQueue();
	}

	public void pauseDeliveries()
	{
		deliveriesPaused = true;
	}

	public void resumeDeliveries()
	{
		deliveriesPaused = false;
		processQueue();
	}

	public synchronized void clearLocalHistory()
	{
		localHistory.clear();
	}

	public int getQueueSize()
	{
		return queue.size();
	}

	public int getQueueCapacity()
	{
		return queue.capacity();
	}

	public Instant getLastCapturedMessageTime()
	{
		return lastCapturedMessageTime;
	}

	public Instant getLastDeliveryAttempt()
	{
		return lastDeliveryAttempt;
	}

	public Instant getLastSuccessfulDelivery()
	{
		return lastSuccessfulDelivery;
	}

	public Integer getLastHttpStatus()
	{
		return lastHttpStatus;
	}

	public int getDeliveredCount()
	{
		return deliveredCount.get();
	}

	public int getFilteredCount()
	{
		return filteredCount.get();
	}

	public int getFailedCount()
	{
		return failedCount.get();
	}

	public int getDroppedCount()
	{
		return droppedCount.get();
	}

	public int getRetriedCount()
	{
		return retriedCount.get();
	}

	public String getCurrentClanName()
	{
		return currentClanName.get();
	}

	public synchronized List<ClanMessageRecord> getLocalHistory()
	{
		return new ArrayList<>(localHistory);
	}

	public String getLastError()
	{
		return lastError;
	}

	private synchronized void appendHistory(ClanMessageRecord record, String eventType, ClanMessageType type, ClanWebhookStatus status, Integer httpStatus)
	{
		while (localHistory.size() >= PluginConstants.DEFAULT_HISTORY_LIMIT)
		{
			localHistory.removeFirst();
		}
		localHistory.addLast(record);
		lastHttpStatus = httpStatus;
	}

	private void processQueue()
	{
		if (!processing.compareAndSet(false, true))
		{
			return;
		}

		executor.execute(() -> {
			try
			{
				if (deliveriesPaused || shutdown.get())
				{
					return;
				}
				ClanMessageRecord record = queue.poll();
				if (record == null)
				{
					return;
				}
				dispatchRecord(record);
			}
			finally
			{
				processing.set(false);
			}
		});
	}

	private synchronized void rebuildQueue()
	{
		ClanMessageQueue replacement = new ClanMessageQueue(config.webhookQueueCapacity());
		ClanMessageRecord record;
		while ((record = queue.poll()) != null)
		{
			if (!replacement.offer(record))
			{
				break;
			}
		}
		queue = replacement;
	}

	private void dispatchRecord(ClanMessageRecord record)
	{
		lastDeliveryAttempt = Instant.now();
		ClanWebhookPayload payload = toPayload(record);
		ClanWebhookAuthenticationMode mode = ClanWebhookAuthenticationMode.fromConfig(config.clanWebhookAuthenticationMode());
		CompletableFuture<ApiResult<String>> future = dispatcher.send(config.clanWebhookEndpoint(), config.clanWebhookSecret(), mode, payload);
		future.whenComplete((result, throwable) -> clientThread.invokeLater(() -> handleDeliveryResult(record, result, throwable)));
	}

	private void handleDeliveryResult(ClanMessageRecord record, ApiResult<String> result, Throwable throwable)
	{
		if (throwable != null)
		{
			failedCount.incrementAndGet();
			lastError = throwable.getMessage();
			appendHistory(record, ClanWebhookEventType.CLAN_CHAT_MESSAGE.name(), record.getType(), ClanWebhookStatus.FAILED, -1);
			processQueue();
			return;
		}

		lastHttpStatus = result.getStatusCode();
		if (result.isSuccess())
		{
			deliveredCount.incrementAndGet();
			lastSuccessfulDelivery = Instant.now();
			appendHistory(record, ClanWebhookEventType.CLAN_CHAT_MESSAGE.name(), record.getType(), ClanWebhookStatus.SENT, result.getStatusCode());
			processQueue();
			return;
		}

		boolean retryable = result.getStatusCode() == 408 || result.getStatusCode() == 425 || result.getStatusCode() == 429 || result.getStatusCode() >= 500;
		if (retryable && config.queueFailedWebhookMessages())
		{
			retriedCount.incrementAndGet();
			executor.schedule(() -> dispatchRecord(record), 1L, TimeUnit.SECONDS);
		}
		else
		{
			failedCount.incrementAndGet();
		}
		appendHistory(record, ClanWebhookEventType.CLAN_CHAT_MESSAGE.name(), record.getType(), ClanWebhookStatus.FAILED, result.getStatusCode());
		processQueue();
	}

	private ClanWebhookPayload toPayload(ClanMessageRecord record)
	{
		Integer world = config.includeWorldNumber() ? client.getWorld() : null;
		String username = client.getLocalPlayer() == null ? "" : client.getLocalPlayer().getName();
		ClanWebhookPayload.Player player = new Player(username, RuneScapeProfileType.getCurrent(client).name(), world);
		ClanWebhookPayload.Clan clan = new Clan(TextSanitizer.stripTags(currentClanName.get()));
		ClanWebhookPayload.Message message = new Message(
			record.getType().name(),
			config.includeSenderRank() ? record.getSender() : null,
			config.includeSenderRank() ? record.getSenderRank() : null,
			sanitizer.sanitize(record.getText(), config.redactUrls()),
			record.isGuest()
		);
		return new ClanWebhookPayload(
			PluginConstants.DEFAULT_CLAN_WEBHOOK_SCHEMA_VERSION,
			record.getEventId(),
			record.isTest() ? ClanWebhookEventType.TEST.name() : (record.getType() == ClanMessageType.CLAN_BROADCAST || record.getType() == ClanMessageType.GUEST_BROADCAST ? ClanWebhookEventType.CLAN_BROADCAST.name() : ClanWebhookEventType.CLAN_CHAT_MESSAGE.name()),
			config.includeTimestamp() ? Instant.now() : record.getOccurredAt(),
			PluginConstants.VERSION,
			player,
			clan,
			message,
			record.isTest()
		);
	}
}
