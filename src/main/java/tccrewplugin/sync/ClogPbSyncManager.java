package tccrewplugin.sync;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.ScriptEvent;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.EnumComposition;
import net.runelite.api.StructComposition;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ChatboxInput;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.StringUtils;
import tccrewplugin.DinkPluginConfig;
import tccrewplugin.SettingsManager;
import tccrewplugin.TcCrewPlugin;
import tccrewplugin.domain.AccountType;
import tccrewplugin.sync.model.CollectionLogItem;
import tccrewplugin.sync.model.CollectionLogSnapshot;
import tccrewplugin.sync.model.CollectionLogState;
import tccrewplugin.sync.model.PersonalBestRecord;
import tccrewplugin.sync.model.PersonalBestSummary;
import tccrewplugin.sync.model.SyncAccountType;
import tccrewplugin.sync.model.SyncClientMetadata;
import tccrewplugin.sync.model.SyncPayload;
import tccrewplugin.sync.model.SyncPlayer;
import tccrewplugin.sync.clog.CollectionLogReader;
import tccrewplugin.sync.clog.CollectionLogSyncTrigger;
import tccrewplugin.sync.pb.PersonalBestTimeParser;
import tccrewplugin.sync.webhook.ClogPbWebhookClient;
import tccrewplugin.sync.webhook.UploadOutcome;
import tccrewplugin.sync.webhook.UploadPriority;
import tccrewplugin.util.AccountTypeTracker;
import tccrewplugin.util.ItemSearcher;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ClogPbSyncManager
{
    private static final String PERSONAL_BEST_GROUP = "personalbest";
    private static final Pattern COLLECTION_LOG_PATTERN = Pattern.compile("New item added to your collection log: (?<item>.+)");
    private static final int COLLECTION_LOG_TRAVERSE_SCRIPT = 2240;
    private static final int COLLECTION_LOG_ITEM_SCRIPT = 4100;
    private static final int COLLECTION_LOG_ROOT_ENUM = 2102;
    private static final int COLLECTION_LOG_SUBTAB_ENUM_PARAM = 683;
    private static final int COLLECTION_LOG_ITEM_ENUM_PARAM = 690;
    private static final int COLLECTION_LOG_REPLACEMENT_ENUM = 3721;
    private static final String[] TOP_LEVEL_CATEGORY_FALLBACK_NAMES = {
        "Bosses",
        "Raids",
        "Clues",
        "Minigames",
        "Other"
    };
    private static final int MAX_CAPTURE_ATTEMPTS = 3;
    private static final long CAPTURE_DELAY_MS = TimeUnit.SECONDS.toMillis(1);
    private static final long TRAVERSAL_QUIET_PERIOD_MS = TimeUnit.SECONDS.toMillis(2);
    private static final long MANUAL_COMMAND_DEBOUNCE_MS = TimeUnit.SECONDS.toMillis(3);
    private static final int CAPTURE_QUIET_TICKS = 2;
    private static final int CAPTURE_TIMEOUT_TICKS = 20;

    private final Client client;
    private final ClientThread clientThread;
    private final ConfigManager configManager;
    private final ChatMessageManager chatMessageManager;
    private final ScheduledExecutorService executor;
    private final Gson gson;
    private final DinkPluginConfig config;
    private final TcCrewPlugin plugin;
    private final ClogPbWebhookClient webhookClient;
    private final AccountTypeTracker accountTypeTracker;
    private final ItemSearcher itemSearcher;
    private final CollectionLogReader collectionLogReader;

    private final Cache<String, Boolean> recentUploads = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.SECONDS)
        .maximumSize(500)
        .build();

    private final AtomicReference<CollectionLogSnapshot> latestCollectionLog = new AtomicReference<>();
    private final AtomicReference<String> lastCollectionLogHash = new AtomicReference<>();
    private final AtomicReference<String> lastQueuedCollectionLogHash = new AtomicReference<>();
    private final AtomicReference<String> lastPersonalBestsHash = new AtomicReference<>();
    private final AtomicReference<String> lastQueuedPersonalBestsHash = new AtomicReference<>();
    private final AtomicReference<PersonalBestSummary> lastPersonalBestSummary = new AtomicReference<>(new PersonalBestSummary(0, 0, 0, 0));
    private final AtomicBoolean capturePending = new AtomicBoolean();
    private final AtomicBoolean autoCollectionUploadInFlight = new AtomicBoolean();
    private final AtomicBoolean autoPersonalBestUploadInFlight = new AtomicBoolean();
    private final AtomicBoolean manualCollectionSyncRequested = new AtomicBoolean();
    private final AtomicBoolean collectionLogTraversalActive = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean shuttingDown = new AtomicBoolean();

    private final Map<String, PersonalBestRecord> lastKnownPersonalBests = new LinkedHashMap<>();
    private final Map<String, Integer> seenItemIds = new LinkedHashMap<>();
    private final Map<String, CollectionLogItem> observedItems = new LinkedHashMap<>();
    private final Set<Integer> capturedCollectionLogItemIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<Integer, CollectionLogItem> capturedCollectionLogItems = new LinkedHashMap<>();
    private final Map<Integer, CollectionLogMetadata> collectionLogItemDefinitions = new LinkedHashMap<>();
    private final Deque<ManualCollectionNavigationTarget> manualCollectionNavigationQueue = new ArrayDeque<>();
    private final Set<String> manualCollectionNavigationVisited = new HashSet<>();
    private final Set<Integer> seenCategories = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<Integer> seenTabs = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<Integer> collectionLogItemCatalogue = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private volatile int lastCollectionCount = -1;
    private volatile int lastCollectionCountMax = -1;
    private volatile int lastCategoryVarbit = Integer.MIN_VALUE;
    private volatile int lastTabVarbit = Integer.MIN_VALUE;
    private volatile int lastCollectionNotificationVarbit = -1;
    private volatile Instant lastRelevantVarbitChange;
    private volatile Instant lastCaptureAt;
    private volatile Instant lastUploadAt;
    private volatile String pendingManualCollectionSyncCommand;
    private volatile Instant lastCollectionLogItemEventAt;
    private volatile int collectionLogCaptureStartedTick = -1;
    private volatile int lastCollectionLogItemTick = -1;
    private volatile boolean collectionLogTraversalCompleted;
    private volatile boolean manualCollectionNavigationActive;
    private volatile boolean manualCollectionNavigationClickInFlight;
    private volatile String manualCollectionNavigationCurrentTabLabel;
    private volatile int manualCollectionCompletionAttempts;
    private volatile ScheduledFuture<?> captureFuture;
    private volatile ScheduledFuture<?> manualCollectionCompleteFuture;

    private static final class CollectionLogMetadata
    {
        private final String category;
        private final String subcategory;

        private CollectionLogMetadata(String category, String subcategory)
        {
            this.category = category;
            this.subcategory = subcategory;
        }
    }

    private static final class ManualCollectionNavigationTarget
    {
        private final ManualCollectionNavigationKind kind;
        private final int widgetId;
        private final String option;
        private final String target;
        private final String visitKey;

        private ManualCollectionNavigationTarget(ManualCollectionNavigationKind kind, int widgetId, String option, String target, String visitKey)
        {
            this.kind = kind;
            this.widgetId = widgetId;
            this.option = option;
            this.target = target;
            this.visitKey = visitKey;
        }
    }

    private enum ManualCollectionNavigationKind
    {
        TAB,
        ENTRY
    }

    @Inject
    public ClogPbSyncManager(
        Client client,
        ClientThread clientThread,
        ConfigManager configManager,
        ChatMessageManager chatMessageManager,
        ScheduledExecutorService executor,
        Gson gson,
        DinkPluginConfig config,
        TcCrewPlugin plugin,
        ClogPbWebhookClient webhookClient,
        AccountTypeTracker accountTypeTracker,
        ItemSearcher itemSearcher,
        CollectionLogReader collectionLogReader)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.configManager = configManager;
        this.chatMessageManager = chatMessageManager;
        this.executor = executor;
        this.gson = gson;
        this.config = config;
        this.plugin = plugin;
        this.webhookClient = webhookClient;
        this.accountTypeTracker = accountTypeTracker;
        this.itemSearcher = itemSearcher;
        this.collectionLogReader = collectionLogReader;
    }

    public void start()
    {
        if (started.getAndSet(true))
        {
            return;
        }

        webhookClient.start();
        clientThread.invokeLater(() ->
        {
            if (shuttingDown.get())
            {
                return;
            }

            refreshVarbitState();
            refreshLocalPersonalBests();
            loadCollectionLogItemCatalogue();
        });
    }

    public void shutdown()
    {
        shuttingDown.set(true);
        started.set(false);
        cancelCapture();
        webhookClient.shutdown();
        synchronized (lastKnownPersonalBests)
        {
            lastKnownPersonalBests.clear();
        }
        observedItems.clear();
        capturedCollectionLogItems.clear();
        collectionLogItemDefinitions.clear();
        seenCategories.clear();
        seenTabs.clear();
        collectionLogItemCatalogue.clear();
        latestCollectionLog.set(null);
        lastCollectionLogHash.set(null);
        lastQueuedCollectionLogHash.set(null);
        lastPersonalBestsHash.set(null);
        lastQueuedPersonalBestsHash.set(null);
        autoCollectionUploadInFlight.set(false);
        autoPersonalBestUploadInFlight.set(false);
    }

    public void onConfigChanged(ConfigChanged event)
    {
        if (PERSONAL_BEST_GROUP.equals(event.getGroup()))
        {
            if (config.clogPbSyncEnabled() && config.pbSyncEnabled() && config.clogPbAutoUploadPersonalBests())
            {
                onPersonalBestConfigChanged(event.getKey());
            }
            return;
        }

        if (!SettingsManager.CONFIG_GROUP.equals(event.getGroup()))
        {
            return;
        }
    }

    @Subscribe
    public void onChatboxInput(ChatboxInput input)
    {
        String value = input.getValue();
        if (value == null)
        {
            return;
        }

        String command = value.trim().toLowerCase(Locale.ROOT);
        if (command.isEmpty())
        {
            return;
        }

        if (command.startsWith("!clogsync"))
        {
            input.consume();
            onManualClogSync();
        }
        else if (command.startsWith("!clogstatus"))
        {
            input.consume();
            onManualClogStatus();
        }
        else if (command.startsWith("!pball"))
        {
            input.consume();
            onManualPbAll();
        }
        else if (command.startsWith("!syncall"))
        {
            input.consume();
            onManualSyncAll();
        }
    }

    public void onGameStateChanged(GameStateChanged event)
    {
        GameState state = event.getGameState();
        if (state == GameState.LOGGED_IN)
        {
            refreshVarbitState();
            refreshLocalPersonalBests();
            loadCollectionLogItemCatalogue();
            requestCapture("game state logged in");
        }
        else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
        {
            clearAccountSpecificState();
            cancelCapture();
        }
    }

    public void onVarbitChanged(VarbitChanged event)
    {
        if (!config.clogPbSyncEnabled() || !config.clogSyncEnabled())
        {
            return;
        }

        int collectionCount = client.getVarpValue(VarPlayerID.COLLECTION_COUNT);
        int collectionCountMax = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
        int category = client.getVarbitValue(VarbitID.COLLECTION_LAST_CATEGORY);
        int tab = client.getVarbitValue(VarbitID.COLLECTION_LAST_TAB);
        int notifyNewItem = client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM);

        boolean changed = false;
        if (collectionCount != lastCollectionCount)
        {
            lastCollectionCount = collectionCount;
            changed = true;
        }
        if (collectionCountMax != lastCollectionCountMax)
        {
            lastCollectionCountMax = collectionCountMax;
            changed = true;
        }
        if (category != lastCategoryVarbit)
        {
            lastCategoryVarbit = category;
            seenCategories.add(category);
            changed = true;
        }
        if (tab != lastTabVarbit)
        {
            lastTabVarbit = tab;
            seenTabs.add(tab);
            changed = true;
        }
        if (notifyNewItem != lastCollectionNotificationVarbit)
        {
            lastCollectionNotificationVarbit = notifyNewItem;
            changed = true;
        }

        if (changed)
        {
            lastRelevantVarbitChange = Instant.now();
            if (config.clogPbAutoUploadCollectionLog())
            {
                requestCapture("collection varbit changed");
            }
        }
    }

    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == InterfaceID.COLLECTION_OVERVIEW && config.clogPbAutoUploadCollectionLog())
        {
            requestCapture("collection widget loaded");
        }
    }

    public void onWidgetClosed(WidgetClosed event)
    {
        if (event.getGroupId() == InterfaceID.COLLECTION_OVERVIEW && config.clogPbAutoUploadCollectionLog())
        {
            requestCapture("collection widget closed");
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!collectionLogTraversalActive.get())
        {
            return;
        }

        if (!manualCollectionSyncRequested.get())
        {
            return;
        }

        int currentTick = client.getTickCount();
        if (collectionLogCaptureStartedTick >= 0 && currentTick - collectionLogCaptureStartedTick >= CAPTURE_TIMEOUT_TICKS)
        {
            failCollectionLogCapture("Collection Log capture timed out.");
            return;
        }

        if (lastCollectionLogItemTick == -1)
        {
            if (collectionLogTraversalCompleted && collectionLogCaptureStartedTick >= 0 && currentTick - collectionLogCaptureStartedTick >= CAPTURE_QUIET_TICKS)
            {
                completeCollectionLogCapture();
            }
            return;
        }

        if (currentTick - lastCollectionLogItemTick < CAPTURE_QUIET_TICKS)
        {
            return;
        }

        completeCollectionLogCapture();
    }

    public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() != COLLECTION_LOG_TRAVERSE_SCRIPT
            && event.getScriptId() != ScriptID.COLLECTION_DRAW_LIST)
        {
            return;
        }

        if (collectionLogTraversalActive.get())
        {
            collectionLogTraversalCompleted = true;
            if (lastCollectionLogItemTick == -1)
            {
                lastCollectionLogItemTick = client.getTickCount();
            }
            if (log.isDebugEnabled())
            {
                log.debug("Collection log traversal script {} completed", event.getScriptId());
            }
            scheduleManualCompletion();
        }

        if (capturePending.get())
        {
            if (captureFuture != null)
            {
                captureFuture.cancel(false);
                captureFuture = null;
            }
            attemptCapture(1);
        }
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event)
    {
        if (!collectionLogTraversalActive.get())
        {
            return;
        }

        int scriptId = event.getScriptId();
        ScriptEvent scriptEvent = event.getScriptEvent();
        if (scriptEvent == null)
        {
            return;
        }

        Object[] arguments = scriptEvent.getArguments();
        if (arguments == null || arguments.length == 0)
        {
            return;
        }

        int added = 0;
        boolean matched = false;
        for (Object argument : arguments)
        {
            if (!(argument instanceof Integer))
            {
                continue;
            }

            int itemId = (Integer) argument;
            if (itemId <= 0 || !collectionLogItemCatalogue.contains(itemId))
            {
                continue;
            }

            matched = true;
            if (captureCollectionLogItem(itemId))
            {
                added++;
            }
        }

        if (matched)
        {
            lastCollectionLogItemTick = client.getTickCount();
            if (log.isDebugEnabled())
            {
                log.debug("Captured {} collection-log item ids from script {}", added, scriptId);
            }
        }
        else if (log.isDebugEnabled()
            && scriptId != COLLECTION_LOG_ITEM_SCRIPT
            && scriptId != COLLECTION_LOG_TRAVERSE_SCRIPT
            && scriptId != ScriptID.COLLECTION_DRAW_LIST)
        {
            log.debug("Ignoring unexpected collection-log script signature {} with {} arguments", scriptId, arguments.length);
        }
    }

    public void onChatMessage(ChatMessage message)
    {
        if (!config.clogPbSyncEnabled() || !config.clogSyncEnabled())
        {
            return;
        }

        if (message.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }

        Matcher matcher = COLLECTION_LOG_PATTERN.matcher(message.getMessage());
        if (!matcher.find())
        {
            return;
        }

        String itemName = normalizeWhitespace(matcher.group("item"));
        if (StringUtils.isBlank(itemName))
        {
            return;
        }

        Integer itemId = itemSearcher.findItemId(itemName);
        CollectionLogItem item = new CollectionLogItem(itemId, itemName, 1, true, "Collection Log", null);
        observedItems.put(itemName.toLowerCase(Locale.ROOT), item);
        if (config.clogPbAutoUploadCollectionLog())
        {
            requestCapture("collection log message");
        }
    }

    public void onManualClogSync()
    {
        executeManual("!clogsync", () -> requestManualCollectionLogSync(CollectionLogSyncTrigger.CHAT_COMMAND));
    }

    public void onManualClogStatus()
    {
        executeManual("!clogstatus", this::showStatus);
    }

    public void onManualPbAll()
    {
        executeManual("!pball", () -> syncPersonalBests("!pball", true));
    }

    public void onManualSyncAll()
    {
        executeManual("!syncall", () -> syncAll("!syncall", true));
    }

    public void onCollectionLogOverlaySync()
    {
        executeManual("!clogsync", () -> requestManualCollectionLogSync(CollectionLogSyncTrigger.OVERLAY));
    }

    public boolean requestManualCollectionLogSync(CollectionLogSyncTrigger trigger)
    {
        if (shuttingDown.get())
        {
            return false;
        }

        if (!isLoggedIn())
        {
            plugin.addChatWarning("Clog/PB Sync: you must be logged in.");
            return false;
        }

        if (collectionLogTraversalActive.get() || manualCollectionSyncRequested.get())
        {
            if (!isStaleManualCollectionCapture())
            {
                plugin.addChatWarning("Clog/PB Sync: collection log sync is already in progress.");
                return false;
            }

            if (log.isDebugEnabled())
            {
                log.debug("Resetting stale manual collection-log capture before starting a new sync");
            }

            collectionLogTraversalActive.set(false);
            manualCollectionSyncRequested.set(false);
            collectionLogTraversalCompleted = false;
            pendingManualCollectionSyncCommand = null;
            resetManualCollectionCapture();
        }

        capturePending.set(false);
        if (captureFuture != null)
        {
            captureFuture.cancel(false);
            captureFuture = null;
        }
        pendingManualCollectionSyncCommand = "!clogsync";
        manualCollectionSyncRequested.set(true);
        collectionLogTraversalActive.set(true);
        collectionLogTraversalCompleted = false;
        manualCollectionNavigationActive = false;
        manualCollectionNavigationClickInFlight = false;
        manualCollectionNavigationCurrentTabLabel = null;
        manualCollectionNavigationQueue.clear();
        manualCollectionNavigationVisited.clear();
        resetManualCollectionCapture();

        clientThread.invokeLater(() ->
        {
            if (shuttingDown.get() || !isLoggedIn())
            {
                collectionLogTraversalActive.set(false);
                manualCollectionSyncRequested.set(false);
                pendingManualCollectionSyncCommand = null;
                return;
            }

            if (!isCollectionLogOpen())
            {
                collectionLogTraversalActive.set(false);
                manualCollectionSyncRequested.set(false);
                pendingManualCollectionSyncCommand = null;
                plugin.addChatWarning("Clog/PB Sync: collection log is not loaded. Open the Collection Log first.");
                return;
            }

            loadCollectionLogItemCatalogue();
            collectionLogCaptureStartedTick = client.getTickCount();
            lastCollectionLogItemTick = collectionLogCaptureStartedTick;
            client.menuAction(-1, InterfaceID.Collection.SEARCH_TOGGLE, MenuAction.CC_OP, 1, -1, "Search", null);
            client.runScript(COLLECTION_LOG_TRAVERSE_SCRIPT);
            if (log.isDebugEnabled())
            {
                log.debug("Started manual collection log capture via {}", trigger);
            }
        });
        return true;
    }

    private boolean isStaleManualCollectionCapture()
    {
        if (collectionLogCaptureStartedTick < 0)
        {
            return false;
        }

        int tickCount = client.getTickCount();
        return tickCount - collectionLogCaptureStartedTick >= CAPTURE_TIMEOUT_TICKS;
    }

    private void executeManual(String command, Runnable runnable)
    {
        clientThread.invokeLater(() ->
        {
            if (!isLoggedIn())
            {
                plugin.addChatWarning("Clog/PB Sync: you must be logged in.");
                return;
            }

            if (isManualCommandDebounced(command))
            {
                plugin.addChatWarning("Clog/PB Sync: please wait before running that command again.");
                return;
            }

            runnable.run();
        });
    }

    private boolean isManualCommandDebounced(String command)
    {
        String key = "manual:" + command.toLowerCase(Locale.ROOT);
        return recentUploads.asMap().putIfAbsent(key, Boolean.TRUE) != null;
    }

    private void requestCapture(String reason)
    {
        if (!capturePending.compareAndSet(false, true))
        {
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug("Queued collection capture due to {}", reason);
        }

        scheduleCapture(1);
    }

    private void rescheduleCapture(String reason)
    {
        if (!capturePending.get())
        {
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug("Rescheduled collection capture due to {}", reason);
        }

        scheduleCapture(1);
    }

    private void scheduleCapture(int attempt)
    {
        if (captureFuture != null)
        {
            captureFuture.cancel(false);
        }

        captureFuture = executor.schedule(() -> clientThread.invokeLater(() -> attemptCapture(attempt)), CAPTURE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void attemptCapture(int attempt)
    {
        if (shuttingDown.get() || !isLoggedIn())
        {
            capturePending.set(false);
            return;
        }

        if (collectionLogTraversalActive.get())
        {
            long quietForMs = lastCollectionLogItemEventAt == null
                ? 0L
                : Math.max(0L, Instant.now().toEpochMilli() - lastCollectionLogItemEventAt.toEpochMilli());

            if ((lastCollectionLogItemEventAt == null || quietForMs < TRAVERSAL_QUIET_PERIOD_MS) && attempt < MAX_CAPTURE_ATTEMPTS)
            {
                captureFuture = executor.schedule(
                    () -> clientThread.invokeLater(() -> attemptCapture(attempt + 1)),
                    CAPTURE_DELAY_MS,
                    TimeUnit.MILLISECONDS);
                return;
            }
        }

        if (!isCaptureStable() && attempt < MAX_CAPTURE_ATTEMPTS)
        {
            captureFuture = executor.schedule(() -> clientThread.invokeLater(() -> attemptCapture(attempt + 1)), CAPTURE_DELAY_MS, TimeUnit.MILLISECONDS);
            return;
        }

        CollectionLogSnapshot snapshot = buildCollectionLogSnapshot();
        if (shouldRetryCollectionCapture(snapshot, attempt))
        {
            captureFuture = executor.schedule(() -> clientThread.invokeLater(() -> attemptCapture(attempt + 1)), CAPTURE_DELAY_MS, TimeUnit.MILLISECONDS);
            return;
        }

        capturePending.set(false);
        collectionLogTraversalActive.set(false);
        latestCollectionLog.set(snapshot);
        lastCaptureAt = Instant.now();

        if (manualCollectionSyncRequested.getAndSet(false))
        {
            String command = pendingManualCollectionSyncCommand;
            pendingManualCollectionSyncCommand = null;
            syncCollectionLog(command == null ? "!clogsync" : command, snapshot);
            return;
        }

        if (config.clogPbAutoUploadCollectionLog())
        {
            uploadCollectionLog(snapshot, false, "!clogsync");
        }
    }

    private boolean shouldRetryCollectionCapture(CollectionLogSnapshot snapshot, int attempt)
    {
        if (attempt >= MAX_CAPTURE_ATTEMPTS)
        {
            return false;
        }

        return isCollectionLogOpen()
            && snapshot != null
            && snapshot.getItems().isEmpty();
    }

    private boolean isCaptureStable()
    {
        return client.getGameState() == GameState.LOGGED_IN;
    }

    private boolean isCollectionLogOpen()
    {
        return client.getWidget(InterfaceID.Collection.UNIVERSE) != null
            || client.getWidget(InterfaceID.Collection.LIST) != null
            || client.getWidget(InterfaceID.Collection.ITEMS_CONTENTS) != null;
    }

    private void syncCollectionLog(String command)
    {
        syncCollectionLog(command, buildCollectionLogSnapshot());
    }

    private void syncManualCollectionLog(String command, CollectionLogSnapshot snapshot)
    {
        if (log.isDebugEnabled())
        {
            log.debug(
                "Submitting manual collection log snapshot for command {} with {} items and state {}",
                command,
                snapshot.getItems().size(),
                snapshot.getState());
        }
        uploadCollectionLog(snapshot, true, command);
    }

    private void seedManualCollectionNavigationTargets()
    {
        if (!manualCollectionNavigationActive)
        {
            return;
        }

        if (manualCollectionNavigationQueue.isEmpty() && manualCollectionNavigationVisited.isEmpty())
        {
            enqueueManualCollectionNavigationTargets(
                discoverManualCollectionNavigationTargets(
                    client.getWidget(InterfaceID.Collection.TABS),
                    ManualCollectionNavigationKind.TAB,
                    "tabs"));
        }
    }

    private void seedManualCollectionEntryTargets()
    {
        if (!manualCollectionNavigationActive)
        {
            return;
        }

        Widget entryContainer = client.getWidget(InterfaceID.Collection.LIST);
        if (entryContainer == null)
        {
            return;
        }

        String context = StringUtils.defaultIfBlank(manualCollectionNavigationCurrentTabLabel, readCurrentCollectionLogTabLabel());
        enqueueManualCollectionNavigationTargetsFirst(
            discoverManualCollectionNavigationTargets(
                entryContainer,
                ManualCollectionNavigationKind.ENTRY,
                context));
    }

    private List<ManualCollectionNavigationTarget> discoverManualCollectionNavigationTargets(Widget root, ManualCollectionNavigationKind kind, String context)
    {
        List<ManualCollectionNavigationTarget> targets = new ArrayList<>();
        if (root == null)
        {
            return targets;
        }

        collectManualCollectionNavigationTargets(root, kind, context, targets, kind == ManualCollectionNavigationKind.ENTRY);
        targets.sort(Comparator
            .comparingInt((ManualCollectionNavigationTarget target) -> client.getWidget(target.widgetId) == null ? Integer.MAX_VALUE : client.getWidget(target.widgetId).getOriginalY())
            .thenComparingInt(target -> client.getWidget(target.widgetId) == null ? Integer.MAX_VALUE : client.getWidget(target.widgetId).getOriginalX())
            .thenComparing(target -> StringUtils.defaultString(target.target).toLowerCase(Locale.ROOT)));
        return targets;
    }

    private void collectManualCollectionNavigationTargets(Widget widget, ManualCollectionNavigationKind kind, String context, List<ManualCollectionNavigationTarget> targets, boolean includeHidden)
    {
        if (widget == null)
        {
            return;
        }

        if (!includeHidden && widget.isHidden())
        {
            return;
        }

        if (isManualCollectionNavigationCandidate(widget, kind))
        {
            String targetText = normalizeWhitespace(firstNonBlank(widget.getText(), widget.getName()));
            String option = firstNonBlank(firstNonBlankAction(widget.getActions()), kind == ManualCollectionNavigationKind.TAB ? "Select" : "Open");
            String visitKey = kind.name() + ":" + StringUtils.defaultString(context).toLowerCase(Locale.ROOT) + ":" + targetText.toLowerCase(Locale.ROOT);
            if (manualCollectionNavigationVisited.add(visitKey))
            {
                targets.add(new ManualCollectionNavigationTarget(kind, widget.getId(), option, targetText, visitKey));
            }
        }

        collectManualCollectionNavigationTargetsShallow(widget.getChildren(), kind, context, targets, includeHidden);
        collectManualCollectionNavigationTargetsShallow(widget.getStaticChildren(), kind, context, targets, includeHidden);
        collectManualCollectionNavigationTargetsShallow(widget.getDynamicChildren(), kind, context, targets, includeHidden);
        collectManualCollectionNavigationTargetsShallow(widget.getNestedChildren(), kind, context, targets, includeHidden);
    }

    private void collectManualCollectionNavigationTargetsShallow(Widget[] widgets, ManualCollectionNavigationKind kind, String context, List<ManualCollectionNavigationTarget> targets, boolean includeHidden)
    {
        if (widgets == null)
        {
            return;
        }

        for (Widget widget : widgets)
        {
            if (widget == null || (!includeHidden && widget.isHidden()))
            {
                continue;
            }

            if (isManualCollectionNavigationCandidate(widget, kind))
            {
                String targetText = normalizeWhitespace(firstNonBlank(widget.getText(), widget.getName()));
                String option = firstNonBlank(firstNonBlankAction(widget.getActions()), kind == ManualCollectionNavigationKind.TAB ? "Select" : "Open");
                String visitKey = kind.name() + ":" + StringUtils.defaultString(context).toLowerCase(Locale.ROOT) + ":" + targetText.toLowerCase(Locale.ROOT);
                if (manualCollectionNavigationVisited.add(visitKey))
                {
                    targets.add(new ManualCollectionNavigationTarget(kind, widget.getId(), option, targetText, visitKey));
                }
            }

            Widget[] children = widget.getChildren();
            if (children != null)
            {
                for (Widget child : children)
                {
                    if (child == null || (!includeHidden && child.isHidden()) || !isManualCollectionNavigationCandidate(child, kind))
                    {
                        continue;
                    }

                    String targetText = normalizeWhitespace(firstNonBlank(child.getText(), child.getName()));
                    String option = firstNonBlank(firstNonBlankAction(child.getActions()), kind == ManualCollectionNavigationKind.TAB ? "Select" : "Open");
                    String visitKey = kind.name() + ":" + StringUtils.defaultString(context).toLowerCase(Locale.ROOT) + ":" + targetText.toLowerCase(Locale.ROOT);
                    if (manualCollectionNavigationVisited.add(visitKey))
                    {
                        targets.add(new ManualCollectionNavigationTarget(kind, child.getId(), option, targetText, visitKey));
                    }
                }
            }
        }
    }

    private boolean isManualCollectionNavigationCandidate(Widget widget, ManualCollectionNavigationKind kind)
    {
        if (widget == null || widget.getItemId() > 0)
        {
            return false;
        }

        String text = normalizeWhitespace(firstNonBlank(widget.getText(), widget.getName()));
        if (StringUtils.isBlank(text))
        {
            return false;
        }

        String[] actions = widget.getActions();
        if (kind == ManualCollectionNavigationKind.ENTRY)
        {
            return widget.getWidth() > 0 && widget.getHeight() > 0;
        }

        if (actions == null)
        {
            return false;
        }

        for (String action : actions)
        {
            if (StringUtils.isNotBlank(action))
            {
                return true;
            }
        }

        return false;
    }

    private String firstNonBlankAction(String[] actions)
    {
        if (actions == null)
        {
            return null;
        }

        for (String action : actions)
        {
            if (StringUtils.isNotBlank(action))
            {
                return action;
            }
        }

        return null;
    }

    private void enqueueManualCollectionNavigationTargets(List<ManualCollectionNavigationTarget> targets)
    {
        if (targets == null || targets.isEmpty())
        {
            return;
        }

        for (ManualCollectionNavigationTarget target : targets)
        {
            if (target != null)
            {
                manualCollectionNavigationQueue.addLast(target);
            }
        }
    }

    private void enqueueManualCollectionNavigationTargetsFirst(List<ManualCollectionNavigationTarget> targets)
    {
        if (targets == null || targets.isEmpty())
        {
            return;
        }

        for (int i = targets.size() - 1; i >= 0; i--)
        {
            ManualCollectionNavigationTarget target = targets.get(i);
            if (target != null)
            {
                manualCollectionNavigationQueue.addFirst(target);
            }
        }
    }

    private void handleManualCollectionNavigationProgress()
    {
        manualCollectionNavigationClickInFlight = false;
        captureVisibleCollectionLogItems();
        seedManualCollectionEntryTargets();
        advanceManualCollectionNavigation();
    }

    private void advanceManualCollectionNavigation()
    {
        if (!manualCollectionNavigationActive || manualCollectionNavigationClickInFlight || !manualCollectionSyncRequested.get())
        {
            return;
        }

        ManualCollectionNavigationTarget nextTarget = pollNextManualCollectionNavigationTarget();
        if (nextTarget == null)
        {
            manualCollectionNavigationActive = false;
            scheduleManualCompletion();
            return;
        }

        manualCollectionNavigationClickInFlight = true;
        if (nextTarget.kind == ManualCollectionNavigationKind.TAB)
        {
            manualCollectionNavigationCurrentTabLabel = nextTarget.target;
        }

        if (log.isDebugEnabled())
        {
            log.debug(
                "Clicking collection log navigation target kind={} option={} target={}",
                nextTarget.kind,
                nextTarget.option,
                nextTarget.target);
        }

        client.menuAction(-1, nextTarget.widgetId, MenuAction.CC_OP, 1, -1, nextTarget.option, nextTarget.target);
    }

    private ManualCollectionNavigationTarget pollNextManualCollectionNavigationTarget()
    {
        while (!manualCollectionNavigationQueue.isEmpty())
        {
            ManualCollectionNavigationTarget next = manualCollectionNavigationQueue.pollFirst();
            if (next != null)
            {
                return next;
            }
        }

        return null;
    }

    private String readCurrentCollectionLogTabLabel()
    {
        Widget tabs = client.getWidget(InterfaceID.Collection.TABS);
        if (tabs == null)
        {
            return null;
        }

        String label = normalizeWhitespace(firstNonBlank(tabs.getText(), tabs.getName()));
        return StringUtils.isBlank(label) ? null : label;
    }

    private void scheduleManualCompletion()
    {
        if (manualCollectionCompleteFuture != null)
        {
            manualCollectionCompleteFuture.cancel(false);
        }

        manualCollectionCompleteFuture = executor.schedule(
            () -> clientThread.invokeLater(() ->
            {
                if (collectionLogTraversalActive.get() && manualCollectionSyncRequested.get())
                {
                    completeCollectionLogCapture();
                }
            }),
            CAPTURE_DELAY_MS,
            TimeUnit.MILLISECONDS);
    }

    private void syncCollectionLog(String command, CollectionLogSnapshot snapshot)
    {
        if (snapshot.getState() == CollectionLogState.NOT_LOADED)
        {
            plugin.addChatWarning("Clog/PB Sync: collection log is not loaded. Open the Collection Log first.");
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug(
                "Submitting collection log snapshot for command {} with {} items and state {}",
                command,
                snapshot.getItems().size(),
                snapshot.getState());
        }
        uploadCollectionLog(snapshot, true, command);
    }

    private CollectionLogSnapshot buildManualCollectionLogSnapshot()
    {
        int obtainedSlots = Math.max(0, lastCollectionCount);
        int knownTotalSlots = collectionLogItemCatalogue.isEmpty()
            ? Math.max(0, lastCollectionCountMax)
            : collectionLogItemCatalogue.size();

        List<CollectionLogItem> items = buildCollectionLogItemsFromIndex();
        items.sort(Comparator.comparing((CollectionLogItem item) -> StringUtils.defaultString(item.getCategory()).toLowerCase(Locale.ROOT))
            .thenComparing(item -> StringUtils.defaultString(item.getSubcategory()).toLowerCase(Locale.ROOT))
            .thenComparing(item -> StringUtils.defaultString(item.getItemName()).toLowerCase(Locale.ROOT)));

        int expectedCategoryCount = countCollectionLogCategoriesInIndex();
        int observedCategoryCount = (int) items.stream()
            .map(CollectionLogItem::getCategory)
            .filter(StringUtils::isNotBlank)
            .map(this::normalizeCollectionLogName)
            .distinct()
            .count();

        int observedSlots = items.size();
        int obtainedSlotsFromItems = (int) items.stream().filter(CollectionLogItem::isObtained).count();
        Set<Integer> unknownCapturedItemIds = capturedCollectionLogItemIds.stream()
            .filter(itemId -> !collectionLogItemDefinitions.containsKey(itemId))
            .collect(Collectors.toSet());
        boolean complete = knownTotalSlots > 0
            && observedSlots == knownTotalSlots
            && observedCategoryCount == expectedCategoryCount
            && unknownCapturedItemIds.isEmpty();

        if (log.isDebugEnabled())
        {
            log.debug(
                "Manual collection log snapshot validation: observedSlots={}, knownTotalSlots={}, observedCategoryCount={}, expectedCategoryCount={}, obtainedSlots={}, unknownCapturedItemIds={}",
                observedSlots,
                knownTotalSlots,
                observedCategoryCount,
                expectedCategoryCount,
                obtainedSlotsFromItems,
                unknownCapturedItemIds.size());
        }

        CollectionLogState state = complete ? CollectionLogState.COMPLETE : CollectionLogState.PARTIAL;

        return new CollectionLogSnapshot(
            state,
            Instant.now().toString(),
            obtainedSlotsFromItems > 0 ? obtainedSlotsFromItems : obtainedSlots,
            observedSlots,
            knownTotalSlots > 0 ? knownTotalSlots : null,
            observedCategoryCount,
            expectedCategoryCount,
            items);
    }

    private List<CollectionLogItem> buildCollectionLogItemsFromIndex()
    {
        List<CollectionLogItem> items = new ArrayList<>();
        if (collectionLogItemDefinitions.isEmpty())
        {
            return items;
        }

        for (Map.Entry<Integer, CollectionLogMetadata> entry : collectionLogItemDefinitions.entrySet())
        {
            int itemId = entry.getKey();
            CollectionLogMetadata metadata = entry.getValue();
            String itemName;
            try
            {
                ItemComposition itemDefinition = client.getItemDefinition(itemId);
                itemName = itemDefinition == null ? null : itemDefinition.getName();
            }
            catch (RuntimeException ex)
            {
                itemName = null;
            }

            itemName = StringUtils.defaultIfBlank(normalizeWhitespace(itemName), "Item " + itemId);
            items.add(new CollectionLogItem(
                itemId,
                itemName,
                1,
                capturedCollectionLogItemIds.contains(itemId),
                metadata == null ? null : metadata.category,
                metadata == null ? null : metadata.subcategory));
        }

        return items;
    }

    private void completeCollectionLogCapture()
    {
        if (!collectionLogTraversalActive.getAndSet(false) || !manualCollectionSyncRequested.getAndSet(false))
        {
            return;
        }

        collectionLogTraversalCompleted = false;
        CollectionLogSnapshot snapshot = buildManualCollectionLogSnapshot();
        if (snapshot.getItems().isEmpty()
            && collectionLogCaptureStartedTick >= 0
            && client.getTickCount() - collectionLogCaptureStartedTick < CAPTURE_TIMEOUT_TICKS
            && manualCollectionCompletionAttempts < MAX_CAPTURE_ATTEMPTS - 1)
        {
            manualCollectionCompletionAttempts++;
            if (log.isDebugEnabled())
            {
                log.debug(
                    "Manual collection log snapshot is empty; retrying completion attempt {}",
                    manualCollectionCompletionAttempts);
            }
            collectionLogTraversalActive.set(true);
            manualCollectionSyncRequested.set(true);
            scheduleManualCompletion();
            return;
        }

        String command = pendingManualCollectionSyncCommand == null ? "!clogsync" : pendingManualCollectionSyncCommand;
        pendingManualCollectionSyncCommand = null;
        resetManualCollectionCapture();
        if (manualCollectionCompleteFuture != null)
        {
            manualCollectionCompleteFuture.cancel(false);
            manualCollectionCompleteFuture = null;
        }
        if (log.isDebugEnabled())
        {
            log.debug(
                "Manual collection log capture complete; handing off to webhook submit with {} items, state={}, obtained={}, total={}",
                snapshot.getItems().size(),
                snapshot.getState(),
                snapshot.getObtainedSlots(),
                snapshot.getKnownTotalSlots());
        }
        syncManualCollectionLog(command, snapshot);
    }

    private void failCollectionLogCapture(String message)
    {
        collectionLogTraversalActive.set(false);
        manualCollectionSyncRequested.set(false);
        pendingManualCollectionSyncCommand = null;
        collectionLogTraversalCompleted = false;
        if (manualCollectionCompleteFuture != null)
        {
            manualCollectionCompleteFuture.cancel(false);
            manualCollectionCompleteFuture = null;
        }
        resetManualCollectionCapture();
        if (config.clogPbShowErrorMessages())
        {
            plugin.addChatWarning("Clog/PB Sync: " + message);
        }
    }

    private void uploadCollectionLog(CollectionLogSnapshot snapshot, boolean manual, String command)
    {
        if (!manual && !autoCollectionUploadInFlight.compareAndSet(false, true))
        {
            return;
        }

        if (webhookClient.hasPendingWork())
        {
            if (manual)
            {
                webhookClient.clearQueuedBacklog();
            }
            else
            {
                autoCollectionUploadInFlight.set(false);
                return;
            }
        }

        String hash = hashOf(snapshot);
        if (!manual && Objects.equals(hash, lastCollectionLogHash.get()))
        {
            autoCollectionUploadInFlight.set(false);
            return;
        }

        if (!manual && Objects.equals(hash, lastQueuedCollectionLogHash.get()))
        {
            autoCollectionUploadInFlight.set(false);
            return;
        }

        lastQueuedCollectionLogHash.set(hash);

        SyncPayload payload = SyncPayload.of(
            "collection_log.snapshot",
            command,
            buildPlayer(),
            buildClientMetadata(),
            snapshot,
            null,
            Collections.emptyList()
        );

        UploadPriority priority = manual ? UploadPriority.HIGH : UploadPriority.LOW;
        if (manual)
        {
            webhookClient.clearQueuedBacklog();
        }

        boolean queued = webhookClient.submit(payload, priority, outcome -> onUploadOutcome("Collection log", outcome, snapshot, snapshot.getObservedSlots()));
        if (!queued)
        {
            autoCollectionUploadInFlight.set(false);
            if (manual)
            {
                plugin.addChatWarning("Clog/PB Sync: upload queue is full.");
            }
            else if (log.isDebugEnabled())
            {
                log.debug("Dropped auto collection-log upload because the queue is full");
            }
        }
        else if (log.isDebugEnabled())
        {
            log.debug(
                "Collection log upload queued for command {} (manual={}, priority={})",
                command,
                manual,
                priority);
        }
        else if (config.clogPbShowQueuedMessages())
        {
            plugin.addChatSuccess("Clog/PB Sync: collection log upload queued.");
        }
    }

    private void syncPersonalBests(String command, boolean manual)
    {
        List<PersonalBestRecord> pbs = readLocalPersonalBests();
        PersonalBestSummary summary = lastPersonalBestSummary.get();
        if (pbs.isEmpty())
        {
            plugin.addChatWarning("Clog/PB Sync: no local personal bests were found.");
            return;
        }

        uploadPersonalBests(command, manual, summary, pbs);
    }

    private void syncAll(String command, boolean manual)
    {
        CollectionLogSnapshot collectionLog = buildCollectionLogSnapshot();
        List<PersonalBestRecord> pbs = readLocalPersonalBests();
        PersonalBestSummary summary = lastPersonalBestSummary.get();

        SyncPayload payload = SyncPayload.of(
            "player_data.snapshot",
            command,
            buildPlayer(),
            buildClientMetadata(),
            collectionLog,
            summary,
            pbs
        );

        if (manual)
        {
            webhookClient.clearQueuedBacklog();
        }

        boolean queued = webhookClient.submit(payload, UploadPriority.HIGH, outcome -> onUploadOutcome("Sync", outcome, collectionLog, pbs.size()));
        if (queued && config.clogPbShowQueuedMessages())
        {
            plugin.addChatSuccess("Clog/PB Sync: combined upload queued.");
        }
    }

    private void uploadPersonalBests(String command, boolean manual, PersonalBestSummary summary, List<PersonalBestRecord> pbs)
    {
        if (!manual && !autoPersonalBestUploadInFlight.compareAndSet(false, true))
        {
            return;
        }

        if (webhookClient.hasPendingWork())
        {
            autoPersonalBestUploadInFlight.set(false);
            return;
        }

        SyncPayload payload = SyncPayload.of(
            "personal_bests.snapshot",
            command,
            buildPlayer(),
            buildClientMetadata(),
            null,
            summary,
            pbs
        );

        String hash = hashOf(payload);
        if (!manual && Objects.equals(hash, lastPersonalBestsHash.get()))
        {
            autoPersonalBestUploadInFlight.set(false);
            return;
        }

        if (!manual && Objects.equals(hash, lastQueuedPersonalBestsHash.get()))
        {
            autoPersonalBestUploadInFlight.set(false);
            return;
        }

        lastQueuedPersonalBestsHash.set(hash);
        UploadPriority priority = manual ? UploadPriority.HIGH : UploadPriority.LOW;
        if (manual)
        {
            webhookClient.clearQueuedBacklog();
        }

        boolean queued = webhookClient.submit(payload, priority, outcome -> onUploadOutcome("Personal bests", outcome, null, pbs.size()));
        if (!queued)
        {
            autoPersonalBestUploadInFlight.set(false);
            if (manual)
            {
                plugin.addChatWarning("Clog/PB Sync: upload queue is full.");
            }
            else if (log.isDebugEnabled())
            {
                log.debug("Dropped auto personal-best upload because the queue is full");
            }
        }
        else if (config.clogPbShowQueuedMessages())
        {
            plugin.addChatSuccess("Clog/PB Sync: PB upload queued.");
        }
    }

    private void onUploadOutcome(String label, UploadOutcome outcome, CollectionLogSnapshot snapshot, int count)
    {
        if (outcome.isSuccess())
        {
            lastUploadAt = Instant.now();
            if ("Collection log".equals(label))
            {
                lastCollectionLogHash.set(lastQueuedCollectionLogHash.get());
                autoCollectionUploadInFlight.set(false);
            }
            else if ("Personal bests".equals(label))
            {
                lastPersonalBestsHash.set(lastQueuedPersonalBestsHash.get());
                autoPersonalBestUploadInFlight.set(false);
            }

            if (config.clogPbShowSuccessMessages())
            {
                plugin.addChatSuccess(formatSuccessMessage(label, snapshot, count));
            }
            return;
        }

        if (outcome.isRetryScheduled())
        {
            return;
        }

        if ("Collection log".equals(label))
        {
            autoCollectionUploadInFlight.set(false);
        }
        else if ("Personal bests".equals(label))
        {
            autoPersonalBestUploadInFlight.set(false);
        }

        if (config.clogPbShowErrorMessages())
        {
            plugin.addChatWarning(String.format("Clog/PB Sync: upload failed%s%s.",
                outcome.getStatusCode() == null ? "" : " with HTTP " + outcome.getStatusCode(),
                outcome.getMessage() == null ? "" : ""));
        }
    }

    private String formatSuccessMessage(String label, CollectionLogSnapshot snapshot, int count)
    {
        if ("Collection log".equals(label))
        {
            if (snapshot != null && snapshot.getKnownTotalSlots() != null)
            {
                return String.format("Clog/PB Sync: uploaded %d/%d obtained collection-log slots.",
                    snapshot.getObtainedSlots(),
                    snapshot.getKnownTotalSlots());
            }
            return String.format("Clog/PB Sync: uploaded %d obtained collection-log slots.",
                snapshot == null ? 0 : snapshot.getObtainedSlots());
        }

        if ("Sync".equals(label))
        {
            String clog = snapshot == null ? "0 obtained collection-log slots" :
                (snapshot.getKnownTotalSlots() == null
                    ? snapshot.getObtainedSlots() + " obtained collection-log slots"
                    : snapshot.getObtainedSlots() + "/" + snapshot.getKnownTotalSlots() + " obtained collection-log slots");
            return String.format("Clog/PB Sync: uploaded %s and %d personal bests.", clog, count);
        }

        return String.format("Clog/PB Sync: uploaded %d personal bests.", count);
    }

    private void showStatus()
    {
        CollectionLogSnapshot snapshot = latestCollectionLog.get();
        int observedSlots = snapshot == null ? 0 : snapshot.getObservedSlots();
        int obtainedSlots = snapshot == null ? Math.max(0, lastCollectionCount) : snapshot.getObtainedSlots();
        String message = String.format(
            "Clog/PB Sync: sync=%s clog=%s cache=%s observed=%d obtained=%d categories=%d tabs=%d complete=%s pending=%s lastVarbit=%s lastCapture=%s lastUpload=%s",
            config.clogPbSyncEnabled(),
            config.clogSyncEnabled(),
            snapshot == null ? "empty" : snapshot.getState().name().toLowerCase(Locale.ROOT),
            observedSlots,
            obtainedSlots,
            seenCategories.size(),
            seenTabs.size(),
            snapshot != null && snapshot.getState() == CollectionLogState.COMPLETE,
            capturePending.get(),
            lastRelevantVarbitChange,
            lastCaptureAt,
            lastUploadAt
        );
        plugin.addChatSuccess(message);
    }

    private CollectionLogSnapshot buildCollectionLogSnapshot()
    {
        if (!isLoggedIn())
        {
            return new CollectionLogSnapshot(CollectionLogState.NOT_LOADED, Instant.now().toString(), 0, 0, null, seenCategories.size(), seenTabs.size(), Collections.emptyList());
        }

        CollectionLogSnapshot widgetSnapshot = collectionLogReader.read(client, Instant.now());
        if (widgetSnapshot.getState() == CollectionLogState.NOT_LOADED && observedItems.isEmpty())
        {
            return widgetSnapshot;
        }

        Map<String, CollectionLogItem> mergedItems = new LinkedHashMap<>();
        for (CollectionLogItem item : widgetSnapshot.getItems())
        {
            String key = itemKey(item);
            mergedItems.putIfAbsent(key, item);
        }

        for (CollectionLogItem item : observedItems.values())
        {
            String key = itemKey(item);
            mergedItems.putIfAbsent(key, item);
        }

        List<CollectionLogItem> items = new ArrayList<>(mergedItems.values());
        items.sort(Comparator.comparing((CollectionLogItem item) -> StringUtils.defaultString(item.getCategory()).toLowerCase(Locale.ROOT))
            .thenComparing(item -> StringUtils.defaultString(item.getSubcategory()).toLowerCase(Locale.ROOT))
            .thenComparing(item -> StringUtils.defaultString(item.getItemName()).toLowerCase(Locale.ROOT)));

        CollectionLogState state = widgetSnapshot.getState();
        if (state == CollectionLogState.NOT_LOADED && !items.isEmpty())
        {
            state = CollectionLogState.PARTIAL;
        }

        return new CollectionLogSnapshot(
            state,
            widgetSnapshot.getCapturedAt(),
            widgetSnapshot.getObtainedSlots(),
            widgetSnapshot.getKnownTotalSlots() != null
                ? widgetSnapshot.getKnownTotalSlots()
                : Math.max(widgetSnapshot.getObservedSlots(), items.size()),
            widgetSnapshot.getKnownTotalSlots(),
            Math.max(widgetSnapshot.getObservedCategoryCount(), seenCategories.size()),
            Math.max(widgetSnapshot.getExpectedCategoryCount(), seenTabs.size()),
            items);
    }

    private List<PersonalBestRecord> readLocalPersonalBests()
    {
        List<PersonalBestRecord> records = new ArrayList<>();
        List<String> keys = configManager.getRSProfileConfigurationKeys(PERSONAL_BEST_GROUP, configManager.getRSProfileKey(), "");
        int known = 0;
        int notLoaded = 0;
        int malformed = 0;
        int unsupported = 0;

        for (String key : keys)
        {
            Object raw = configManager.getRSProfileConfiguration(PERSONAL_BEST_GROUP, key, Object.class);
            if (raw == null)
            {
                notLoaded++;
                continue;
            }

            Long durationMs = PersonalBestTimeParser.parseToMillis(raw);
            if (durationMs == null || durationMs <= 0)
            {
                malformed++;
                continue;
            }

            known++;
            PersonalBestRecord record = new PersonalBestRecord(
                key,
                humanizeKey(key),
                null,
                null,
                durationMs,
                "runelite-local-config"
            );
            records.add(record);
            lastKnownPersonalBests.put(key, record);
        }

        lastPersonalBestSummary.set(new PersonalBestSummary(known, notLoaded, malformed, unsupported));
        return records;
    }

    private void onPersonalBestConfigChanged(String key)
    {
        Object raw = configManager.getRSProfileConfiguration(PERSONAL_BEST_GROUP, key, Object.class);
        Long durationMs = PersonalBestTimeParser.parseToMillis(raw);
        if (durationMs == null || durationMs <= 0)
        {
            return;
        }

        PersonalBestRecord record = new PersonalBestRecord(key, humanizeKey(key), null, null, durationMs, "runelite-local-config");
        synchronized (lastKnownPersonalBests)
        {
            PersonalBestRecord previous = lastKnownPersonalBests.get(key);
            if (previous != null && previous.getDurationMilliseconds() <= durationMs)
            {
                return;
            }
            lastKnownPersonalBests.put(key, record);
        }

        if (config.clogPbAutoUploadPersonalBests())
        {
            syncPersonalBests("!pball", false);
        }
    }

    private SyncPlayer buildPlayer()
    {
        Player player = client.getLocalPlayer();
        String displayName = player == null ? "" : player.getName();
        return new SyncPlayer(displayName, SyncAccountType.from(resolveAccountType()));
    }

    private AccountType resolveAccountType()
    {
        AccountType type = AccountType.get(client.getVarbitValue(VarbitID.IRONMAN));
        return type == null ? AccountType.NORMAL : type;
    }

    private SyncClientMetadata buildClientMetadata()
    {
        return new SyncClientMetadata(net.runelite.client.RuneLiteProperties.getVersion(), TcCrewPlugin.class.getPackage().getImplementationVersion());
    }

    private void refreshVarbitState()
    {
        lastCollectionCount = client.getVarpValue(VarPlayerID.COLLECTION_COUNT);
        lastCollectionCountMax = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
        lastCategoryVarbit = client.getVarbitValue(VarbitID.COLLECTION_LAST_CATEGORY);
        lastTabVarbit = client.getVarbitValue(VarbitID.COLLECTION_LAST_TAB);
        lastCollectionNotificationVarbit = client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM);
        seenCategories.add(lastCategoryVarbit);
        seenTabs.add(lastTabVarbit);
    }

    private void refreshLocalPersonalBests()
    {
        synchronized (lastKnownPersonalBests)
        {
            if (!lastKnownPersonalBests.isEmpty())
            {
                return;
            }
        }
        readLocalPersonalBests();
    }

    private void loadCollectionLogItemCatalogue()
    {
        if (!collectionLogItemCatalogue.isEmpty() || !isLoggedIn())
        {
            return;
        }

        try
        {
            Map<Integer, CollectionLogMetadata> resolved = resolveCollectionLogItemCatalogue();
            collectionLogItemCatalogue.addAll(resolved.keySet());
            collectionLogItemDefinitions.putAll(resolved);
            if (log.isDebugEnabled())
            {
                log.debug("Loaded {} collection log catalogue entries", collectionLogItemCatalogue.size());
            }
        }
        catch (RuntimeException ex)
        {
            log.debug("Unable to load collection log catalogue", ex);
        }
    }

    private Map<Integer, CollectionLogMetadata> resolveCollectionLogItemCatalogue()
    {
        Map<Integer, CollectionLogMetadata> itemDefinitions = new LinkedHashMap<>();

        EnumComposition rootEnum = client.getEnum(COLLECTION_LOG_ROOT_ENUM);
        if (rootEnum == null || rootEnum.getIntVals() == null)
        {
            return itemDefinitions;
        }

        int[] topLevelStructIds = rootEnum.getIntVals();
        String[] topLevelNames = rootEnum.getStringVals();

        for (int topLevelIndex = 0; topLevelIndex < topLevelStructIds.length; topLevelIndex++)
        {
            int topLevelStructId = topLevelStructIds[topLevelIndex];
            if (topLevelStructId <= 0)
            {
                continue;
            }

            String categoryName = normalizeCollectionLogName(getEnumString(topLevelNames, topLevelIndex,
                topLevelIndex >= 0 && topLevelIndex < TOP_LEVEL_CATEGORY_FALLBACK_NAMES.length
                    ? TOP_LEVEL_CATEGORY_FALLBACK_NAMES[topLevelIndex]
                    : "Collection Log"));
            StructComposition topLevelStruct = client.getStructComposition(topLevelStructId);
            if (topLevelStruct == null)
            {
                continue;
            }

            int subtabEnumId = topLevelStruct.getIntValue(COLLECTION_LOG_SUBTAB_ENUM_PARAM);
            if (subtabEnumId <= 0)
            {
                continue;
            }

            EnumComposition subtabEnum = client.getEnum(subtabEnumId);
            if (subtabEnum == null || subtabEnum.getIntVals() == null)
            {
                continue;
            }

            int[] subtabStructIds = subtabEnum.getIntVals();
            String[] subtabNames = subtabEnum.getStringVals();

            for (int subtabIndex = 0; subtabIndex < subtabStructIds.length; subtabIndex++)
            {
                int subtabStructId = subtabStructIds[subtabIndex];
                if (subtabStructId <= 0)
                {
                    continue;
                }

                String subcategoryName = normalizeCollectionLogName(getEnumString(subtabNames, subtabIndex, null));
                StructComposition subtabStruct = client.getStructComposition(subtabStructId);
                if (subtabStruct == null)
                {
                    continue;
                }

                int itemEnumId = subtabStruct.getIntValue(COLLECTION_LOG_ITEM_ENUM_PARAM);
                if (itemEnumId <= 0)
                {
                    continue;
                }

                EnumComposition itemEnum = client.getEnum(itemEnumId);
                if (itemEnum == null || itemEnum.getIntVals() == null)
                {
                    continue;
                }

                for (int itemId : itemEnum.getIntVals())
                {
                    if (itemId > 0)
                    {
                        itemDefinitions.put(itemId, new CollectionLogMetadata(categoryName, subcategoryName));
                    }
                }
            }
        }

        EnumComposition replacements = client.getEnum(COLLECTION_LOG_REPLACEMENT_ENUM);
        if (replacements != null)
        {
            int[] replacedIds = replacements.getKeys();
            int[] replacementIds = replacements.getIntVals();
            if (replacedIds != null && replacementIds != null)
            {
                for (int i = 0; i < Math.min(replacedIds.length, replacementIds.length); i++)
                {
                    int replacedId = replacedIds[i];
                    int replacementId = replacementIds[i];
                    if (replacedId <= 0 || replacementId <= 0)
                    {
                        continue;
                    }

                    CollectionLogMetadata metadata = itemDefinitions.remove(replacedId);
                    if (metadata != null)
                    {
                        itemDefinitions.put(replacementId, metadata);
                    }
                }
            }
        }

        return itemDefinitions;
    }

    private int countCollectionLogCategoriesInIndex()
    {
        return (int) collectionLogItemDefinitions.values().stream()
            .map(metadata -> metadata == null ? null : metadata.category)
            .filter(StringUtils::isNotBlank)
            .map(this::normalizeCollectionLogName)
            .distinct()
            .count();
    }

    private void clearAccountSpecificState()
    {
        observedItems.clear();
        capturedCollectionLogItems.clear();
        capturedCollectionLogItemIds.clear();
        collectionLogItemDefinitions.clear();
        seenCategories.clear();
        seenTabs.clear();
        latestCollectionLog.set(null);
        manualCollectionSyncRequested.set(false);
        pendingManualCollectionSyncCommand = null;
        lastCollectionCount = -1;
        lastCollectionCountMax = -1;
        lastCategoryVarbit = Integer.MIN_VALUE;
        lastTabVarbit = Integer.MIN_VALUE;
        lastCollectionNotificationVarbit = -1;
        autoCollectionUploadInFlight.set(false);
        autoPersonalBestUploadInFlight.set(false);
        collectionLogTraversalActive.set(false);
        collectionLogTraversalCompleted = false;
        manualCollectionCompletionAttempts = 0;
        collectionLogCaptureStartedTick = -1;
        lastCollectionLogItemTick = -1;
        manualCollectionNavigationClickInFlight = false;
        manualCollectionNavigationCurrentTabLabel = null;
        if (manualCollectionCompleteFuture != null)
        {
            manualCollectionCompleteFuture.cancel(false);
            manualCollectionCompleteFuture = null;
        }
    }

    private void cancelCapture()
    {
        capturePending.set(false);
        manualCollectionSyncRequested.set(false);
        collectionLogTraversalActive.set(false);
        manualCollectionNavigationActive = false;
        manualCollectionNavigationClickInFlight = false;
        manualCollectionNavigationCurrentTabLabel = null;
        manualCollectionNavigationQueue.clear();
        manualCollectionNavigationVisited.clear();
        pendingManualCollectionSyncCommand = null;
        resetManualCollectionCapture();
        if (captureFuture != null)
        {
            captureFuture.cancel(false);
            captureFuture = null;
        }
        if (manualCollectionCompleteFuture != null)
        {
            manualCollectionCompleteFuture.cancel(false);
            manualCollectionCompleteFuture = null;
        }
    }

    private void resetManualCollectionCapture()
    {
        capturedCollectionLogItems.clear();
        capturedCollectionLogItemIds.clear();
        collectionLogCaptureStartedTick = -1;
        lastCollectionLogItemTick = -1;
        collectionLogTraversalCompleted = false;
        manualCollectionCompletionAttempts = 0;
        manualCollectionNavigationClickInFlight = false;
        manualCollectionNavigationCurrentTabLabel = null;
    }

    private boolean isLoggedIn()
    {
        return client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
    }

    private String hashOf(Object value)
    {
        return Integer.toHexString(Objects.hashCode(gson.toJson(value)));
    }

    private String normalizeWhitespace(String value)
    {
        return StringUtils.normalizeSpace(value == null ? "" : value);
    }

    private String firstNonBlank(String first, String second)
    {
        return StringUtils.isNotBlank(first) ? first : second;
    }

    private boolean captureCollectionLogItem(int itemId)
    {
        lastCollectionLogItemEventAt = Instant.now();
        if (!capturedCollectionLogItemIds.add(itemId))
        {
            return false;
        }

        String itemName;
        try
        {
            ItemComposition itemDefinition = client.getItemDefinition(itemId);
            itemName = itemDefinition == null ? null : itemDefinition.getName();
        }
        catch (RuntimeException ex)
        {
            itemName = null;
        }

        itemName = StringUtils.defaultIfBlank(normalizeWhitespace(itemName), "Item " + itemId);

        CollectionLogMetadata metadata = collectionLogItemDefinitions.get(itemId);
        String category = metadata == null ? "Collection Log" : metadata.category;
        String subcategory = metadata == null ? null : metadata.subcategory;
        CollectionLogItem item = new CollectionLogItem(itemId, itemName, 1, true, category, subcategory);

        capturedCollectionLogItems.putIfAbsent(itemId, item);
        observedItems.put(itemKey(item), item);
        return true;
    }

    private void captureVisibleCollectionLogItems()
    {
        CollectionLogSnapshot snapshot = collectionLogReader.read(client, Instant.now());
        if (snapshot == null || snapshot.getItems() == null || snapshot.getItems().isEmpty())
        {
            return;
        }

        int added = 0;
        for (CollectionLogItem item : snapshot.getItems())
        {
            if (item == null)
            {
                continue;
            }

            if (item.getItemId() > 0)
            {
                CollectionLogItem resolvedItem = applyCollectionLogMetadata(item);

                if (capturedCollectionLogItemIds.add(item.getItemId()))
                {
                    capturedCollectionLogItems.putIfAbsent(item.getItemId(), resolvedItem);
                    observedItems.put(itemKey(resolvedItem), resolvedItem);
                    added++;
                }
            }
            else if (item.getItemId() <= 0)
            {
                observedItems.putIfAbsent(itemKey(item), item);
                added++;
            }
        }

        if (added > 0 && log.isDebugEnabled())
        {
            log.debug(
                "Captured {} visible collection-log items from traversal snapshot (state={}, observed={}, obtained={})",
                added,
                snapshot.getState(),
                snapshot.getObservedSlots(),
                snapshot.getObtainedSlots());
        }
    }

    private CollectionLogItem applyCollectionLogMetadata(CollectionLogItem item)
    {
        if (item == null || item.getItemId() <= 0)
        {
            return item;
        }

        CollectionLogMetadata metadata = collectionLogItemDefinitions.get(item.getItemId());
        if (metadata == null)
        {
            return item;
        }

        String category = StringUtils.defaultIfBlank(item.getCategory(), metadata.category);
        if (StringUtils.equalsIgnoreCase(category, "Collection Log") && StringUtils.isNotBlank(metadata.category))
        {
            category = metadata.category;
        }
        String subcategory = StringUtils.defaultIfBlank(item.getSubcategory(), metadata.subcategory);
        if (StringUtils.equals(category, item.getCategory()) && StringUtils.equals(subcategory, item.getSubcategory()))
        {
            return item;
        }

        return new CollectionLogItem(
            item.getItemId(),
            item.getItemName(),
            item.getQuantity(),
            item.isObtained(),
            category,
            subcategory);
    }

    private String humanizeKey(String key)
    {
        if (key == null || key.isEmpty())
        {
            return "";
        }

        String[] parts = key.split("[_\\-]");
        return java.util.Arrays.stream(parts)
            .filter(StringUtils::isNotBlank)
            .map(part ->
            {
                String lower = part.toLowerCase(Locale.ROOT);
                return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
            })
            .collect(Collectors.joining(" "));
    }

    private String itemKey(CollectionLogItem item)
    {
        if (item == null)
        {
            return "";
        }

        return String.format("%s:%s:%s:%s:%s",
            item.getItemId(),
            StringUtils.defaultString(item.getItemName()).toLowerCase(Locale.ROOT),
            item.getQuantity(),
            StringUtils.defaultString(item.getCategory()).toLowerCase(Locale.ROOT),
            StringUtils.defaultString(item.getSubcategory()).toLowerCase(Locale.ROOT));
    }

    private String getEnumString(String[] values, int index, String fallback)
    {
        if (values == null || index < 0 || index >= values.length)
        {
            return fallback;
        }

        String value = values[index];
        return StringUtils.isBlank(value) ? fallback : value;
    }

    private String normalizeCollectionLogName(String value)
    {
        return StringUtils.normalizeSpace(value == null ? "" : value.replace((char) 160, ' '));
    }
}
