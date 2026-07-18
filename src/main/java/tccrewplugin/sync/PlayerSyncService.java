package tccrewplugin.sync;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import okhttp3.OkHttpClient;
import tccrewplugin.PluginConstants;
import tccrewplugin.TangleDinkConfig;
import tccrewplugin.api.ApiResult;
import tccrewplugin.api.ManifestClient;
import tccrewplugin.api.PlayerSyncApiClient;
import tccrewplugin.collectionlog.CollectionLogService;
import tccrewplugin.sync.model.PlayerDelta;
import tccrewplugin.sync.model.PlayerIdentity;
import tccrewplugin.sync.model.PlayerSnapshot;
import tccrewplugin.sync.model.PlayerSubmission;
import tccrewplugin.sync.model.SyncManifest;
import tccrewplugin.util.TextSanitizer;

import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Singleton
public class PlayerSyncService
{
	private final net.runelite.api.Client client;
	private final ClientThread clientThread;
	private final TangleDinkConfig config;
	private final Gson gson;
	private final ScheduledExecutorService executor;
	private final ManifestClient manifestClient;
	private final PlayerSyncApiClient syncApiClient;
	private final PlayerSnapshotService snapshotService;
	private final PlayerDeltaService deltaService = new PlayerDeltaService();
	private final SyncStateStore stateStore = new SyncStateStore();
	private final SyncScheduler syncScheduler;
	private final SyncScheduler manifestScheduler;
	private final RetryPolicy retryPolicy = new RetryPolicy(PluginConstants.MAX_RETRY_ATTEMPTS, 2000);
	private final AtomicReference<SyncManifest> manifest = new AtomicReference<>();
	private final AtomicReference<PlayerIdentity> currentIdentity = new AtomicReference<>(new PlayerIdentity("", ""));
	private final AtomicReference<SyncStatusModel> status = new AtomicReference<>(
		new SyncStatusModel(SyncStatus.DISABLED, null, null, null, null, 0, null, 0, null)
	);
	private final AtomicBoolean shutdown = new AtomicBoolean();
	private final AtomicBoolean syncInFlight = new AtomicBoolean();
	private final AtomicBoolean manifestInFlight = new AtomicBoolean();

	private volatile Instant nextRetryTime;
	private volatile int retryAttempt;
	private volatile boolean loggedIn;

	public PlayerSyncService(net.runelite.api.Client client, ClientThread clientThread, TangleDinkConfig config, Gson gson, ScheduledExecutorService executor, OkHttpClient httpClient, CollectionLogService collectionLogService)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.gson = gson;
		this.executor = executor;
		this.manifestClient = new ManifestClient(httpClient, gson);
		this.syncApiClient = new PlayerSyncApiClient(httpClient, gson);
		this.snapshotService = new PlayerSnapshotService(client, collectionLogService);
		this.syncScheduler = new SyncScheduler(executor);
		this.manifestScheduler = new SyncScheduler(executor);
	}

	public void startUp()
	{
		shutdown.set(false);
		stateStore.clear();
		refreshManifest();
		rescheduleSync();
		scheduleManifestRefresh();
	}

	public void shutDown()
	{
		shutdown.set(true);
		syncScheduler.cancelAll();
		manifestScheduler.cancelAll();
		syncInFlight.set(false);
		manifestInFlight.set(false);
	}

	public void onGameStateChanged(GameState gameState)
	{
		loggedIn = gameState == GameState.LOGGED_IN;
		if (!loggedIn)
		{
			currentIdentity.set(new PlayerIdentity("", ""));
			updateStatus(SyncStatus.WAITING_FOR_LOGIN, null, null, null, 0, getManifestVersion(), retryAttempt, nextRetryTime);
		}
		else
		{
			rescheduleSync();
		}
	}

	public void onConfigChanged(String key)
	{
		if ("automaticSyncEnabled".equals(key) || "syncIntervalSeconds".equals(key))
		{
			rescheduleSync();
		}
		else if ("apiBaseUrl".equals(key) || "apiToken".equals(key))
		{
			rescheduleSync();
		}
		else if ("syncSkills".equals(key) || "syncVarbits".equals(key) || "syncVarps".equals(key) || "syncCollectionLog".equals(key))
		{
			requestSync();
		}
	}

	public void reloadManifest()
	{
		refreshManifest();
	}

	public void requestSync()
	{
		if (shutdown.get())
		{
			return;
		}
		if (!loggedIn)
		{
			updateStatus(SyncStatus.WAITING_FOR_LOGIN, null, null, null, 0, getManifestVersion(), retryAttempt, nextRetryTime);
			return;
		}
		if (manifest.get() == null)
		{
			updateStatus(SyncStatus.WAITING_FOR_MANIFEST, null, null, null, 0, null, retryAttempt, nextRetryTime);
			return;
		}
		if (!syncInFlight.compareAndSet(false, true))
		{
			return;
		}

		updateStatus(SyncStatus.READING_CLIENT_STATE, Instant.now(), null, null, 0, getManifestVersion(), retryAttempt, nextRetryTime);
		clientThread.invokeLater(() -> {
			try
			{
				PlayerIdentity identity = new PlayerIdentity(resolveUsername(), resolveProfileType());
				currentIdentity.set(identity);
				PlayerSnapshot snapshot = snapshotService.capture(
					manifest.get(),
					config.syncSkills(),
					config.syncVarbits(),
					config.syncVarps(),
					config.syncCollectionLog()
				);
				PlayerSnapshot previous = stateStore.get(identity);
				PlayerDelta delta = deltaService.diff(previous, snapshot);
				int pendingFieldCount = delta.getVarbits().size() + delta.getVarps().size() + delta.getRealLevels().size() + (delta.getCollectionLog() == null ? 0 : 1);
				if (pendingFieldCount == 0)
				{
					updateStatus(SyncStatus.SUCCESS, snapshot.getCapturedAt(), snapshot.getCapturedAt(), 200, 0, getManifestVersion(), 0, null);
					syncInFlight.set(false);
					return;
				}
				PlayerSubmission submission = new PlayerSubmission(
					PluginConstants.DEFAULT_PLAYER_SUBMISSION_SCHEMA_VERSION,
					identity.getUsername(),
					identity.getProfileType(),
					PluginConstants.VERSION,
					snapshot.getCapturedAt(),
					new PlayerSubmission.Data(delta.getVarbits(), delta.getVarps(), delta.getRealLevels(), delta.getCollectionLog())
				);
				updateStatus(SyncStatus.SUBMITTING, snapshot.getCapturedAt(), null, null, pendingFieldCount, getManifestVersion(), retryAttempt, nextRetryTime);
				sendSubmission(identity, snapshot, delta, submission, pendingFieldCount);
			}
			catch (RuntimeException ex)
			{
				syncInFlight.set(false);
				updateStatus(SyncStatus.ERROR, Instant.now(), null, null, 0, getManifestVersion(), retryAttempt, nextRetryTime, sanitize(ex));
			}
		});
	}

	private void sendSubmission(PlayerIdentity identity, PlayerSnapshot snapshot, PlayerDelta delta, PlayerSubmission submission, int pendingFieldCount)
	{
		syncApiClient.submit(config.apiBaseUrl(), config.apiToken(), submission).whenComplete((result, throwable) -> {
			if (shutdown.get())
			{
				syncInFlight.set(false);
				return;
			}
			if (throwable != null)
			{
				handleFailure(identity, snapshot, pendingFieldCount, throwable);
			}
			else if (result.isSuccess())
			{
				stateStore.put(identity, snapshot);
				retryAttempt = 0;
				nextRetryTime = null;
				updateStatus(SyncStatus.SUCCESS, snapshot.getCapturedAt(), snapshot.getCapturedAt(), result.getStatusCode(), 0, getManifestVersion(), 0, null);
			}
			else
			{
				handleFailure(identity, snapshot, pendingFieldCount, new IllegalStateException("HTTP " + result.getStatusCode() + (result.getError() == null ? "" : ": " + result.getError())));
				if (result.getStatusCode() == 408 || result.getStatusCode() == 425 || result.getStatusCode() == 429 || result.getStatusCode() >= 500)
				{
					scheduleRetry();
				}
			}
			syncInFlight.set(false);
		});
	}

	private void handleFailure(PlayerIdentity identity, PlayerSnapshot snapshot, int pendingFieldCount, Throwable throwable)
	{
		retryAttempt++;
		nextRetryTime = Instant.now().plusMillis(retryPolicy.calculateDelayMillis(retryAttempt));
		updateStatus(SyncStatus.ERROR, snapshot.getCapturedAt(), null, null, pendingFieldCount, getManifestVersion(), retryAttempt, nextRetryTime, sanitize(throwable));
	}

	private void scheduleRetry()
	{
		if (!retryPolicy.shouldRetry(retryAttempt))
		{
			return;
		}
		long delay = retryPolicy.calculateDelayMillis(retryAttempt);
		nextRetryTime = Instant.now().plusMillis(delay);
		updateStatus(SyncStatus.BACKING_OFF, Instant.now(), null, null, 0, getManifestVersion(), retryAttempt, nextRetryTime);
		syncScheduler.scheduleOnce(this::requestSync, delay, TimeUnit.MILLISECONDS);
	}

	private void refreshManifest()
	{
		if (shutdown.get() || !manifestInFlight.compareAndSet(false, true))
		{
			return;
		}
		manifestClient.fetchManifest(config.apiBaseUrl()).whenComplete((fresh, throwable) -> {
			manifestInFlight.set(false);
			if (throwable != null)
			{
				if (manifest.get() == null)
				{
					updateStatus(SyncStatus.WAITING_FOR_MANIFEST, null, null, null, 0, null, retryAttempt, nextRetryTime, sanitize(throwable));
				}
				return;
			}
			manifest.set(fresh);
			updateStatus(loggedIn ? SyncStatus.IDLE : SyncStatus.WAITING_FOR_LOGIN, null, null, null, 0, fresh.getVersion(), retryAttempt, nextRetryTime);
			if (loggedIn && config.automaticSyncEnabled())
			{
				requestSync();
			}
		});
	}

	private void scheduleManifestRefresh()
	{
		manifestScheduler.schedulePeriodic(this::refreshManifest, PluginConstants.DEFAULT_MANIFEST_REFRESH_SECONDS, PluginConstants.DEFAULT_MANIFEST_REFRESH_SECONDS, TimeUnit.SECONDS);
	}

	private void rescheduleSync()
	{
		syncScheduler.cancelAll();
		if (!config.automaticSyncEnabled())
		{
			updateStatus(SyncStatus.DISABLED, null, null, null, 0, getManifestVersion(), retryAttempt, nextRetryTime);
			return;
		}
		if (!loggedIn)
		{
			updateStatus(SyncStatus.WAITING_FOR_LOGIN, null, null, null, 0, getManifestVersion(), retryAttempt, nextRetryTime);
			return;
		}
		if (manifest.get() == null)
		{
			updateStatus(SyncStatus.WAITING_FOR_MANIFEST, null, null, null, 0, null, retryAttempt, nextRetryTime);
			return;
		}
		int interval = Math.max(PluginConstants.MIN_SYNC_INTERVAL_SECONDS, config.syncIntervalSeconds());
		syncScheduler.schedulePeriodic(this::requestSync, interval, interval, TimeUnit.SECONDS);
		updateStatus(SyncStatus.IDLE, null, null, null, 0, getManifestVersion(), retryAttempt, nextRetryTime);
	}

	private Integer getManifestVersion()
	{
		SyncManifest current = manifest.get();
		return current == null ? null : current.getVersion();
	}

	public SyncStatusModel getStatus()
	{
		return status.get();
	}

	public Integer getManifestVersionValue()
	{
		return getManifestVersion();
	}

	public String getCurrentUsername()
	{
		return currentIdentity.get().getUsername();
	}

	public String getCurrentProfileType()
	{
		return currentIdentity.get().getProfileType();
	}

	public Instant getLastSuccessfulSync()
	{
		return status.get().getLastSuccess();
	}

	private void updateStatus(SyncStatus status, Instant lastAttempt, Instant lastSuccess, Integer lastHttpStatus, int pendingFieldCount, Integer manifestVersion, int retryAttempt, Instant nextRetryTime)
	{
		updateStatus(status, lastAttempt, lastSuccess, lastHttpStatus, pendingFieldCount, manifestVersion, retryAttempt, nextRetryTime, null);
	}

	private void updateStatus(SyncStatus status, Instant lastAttempt, Instant lastSuccess, Integer lastHttpStatus, int pendingFieldCount, Integer manifestVersion, int retryAttempt, Instant nextRetryTime, String error)
	{
		this.status.set(new SyncStatusModel(status, lastAttempt, lastSuccess, lastHttpStatus, error, pendingFieldCount, manifestVersion, retryAttempt, nextRetryTime));
	}

	private String resolveUsername()
	{
		return client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "";
	}

	private String resolveProfileType()
	{
		return net.runelite.client.config.RuneScapeProfileType.getCurrent(client).name();
	}

	private static String sanitize(Throwable throwable)
	{
		String message = throwable == null ? "unknown error" : throwable.getMessage();
		if (message == null || message.isEmpty())
		{
			return "unknown error";
		}
		return TextSanitizer.stripTags(message);
	}
}
