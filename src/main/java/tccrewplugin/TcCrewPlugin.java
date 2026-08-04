package tccrewplugin;

import com.google.inject.Provides;
import tccrewplugin.clanchat.ClanChatWebhookManager;
import tccrewplugin.lfg.LfgNavigationManager;
import tccrewplugin.lfg.LfgPanel;
import tccrewplugin.lfg.LfgService;
import tccrewplugin.sync.ClogPbSyncManager;
import tccrewplugin.sync.clog.CollectionLogSyncButtonManager;
import tccrewplugin.notifiers.ChatNotifier;
import tccrewplugin.notifiers.ClueNotifier;
import tccrewplugin.notifiers.CollectionNotifier;
import tccrewplugin.notifiers.CombatTaskNotifier;
import tccrewplugin.notifiers.DeathNotifier;
import tccrewplugin.notifiers.DiaryNotifier;
import tccrewplugin.notifiers.ExternalPluginNotifier;
import tccrewplugin.notifiers.GambleNotifier;
import tccrewplugin.notifiers.GrandExchangeNotifier;
import tccrewplugin.notifiers.GroupStorageNotifier;
import tccrewplugin.notifiers.KillCountNotifier;
import tccrewplugin.notifiers.LevelNotifier;
import tccrewplugin.notifiers.LootNotifier;
import tccrewplugin.notifiers.MetaNotifier;
import tccrewplugin.notifiers.PetNotifier;
import tccrewplugin.notifiers.PlayerKillNotifier;
import tccrewplugin.notifiers.QuestNotifier;
import tccrewplugin.notifiers.SlayerNotifier;
import tccrewplugin.notifiers.SpeedrunNotifier;
import tccrewplugin.notifiers.TradeNotifier;
import tccrewplugin.notifiers.LeaguesNotifier;
import tccrewplugin.util.AccountTypeTracker;
import tccrewplugin.util.KillCountService;
import tccrewplugin.util.Utils;
import tccrewplugin.util.WorldTypeTracker;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.AccountHashChanged;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.events.ChatboxInput;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.UsernameChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.events.WorldChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NotificationFired;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.inject.Provider;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@PluginDescriptor(
    name = "Tangle Crew Plugin",
    description = "Discord-compatible webhook notifications for Loot, Death, Levels, CLog, KC, Diary, Quests, and clan chat.",
    tags = { "loot", "logger", "collection", "pet", "death", "xp", "level", "notifications", "discord", "speedrun",
        "diary", "combat achievements", "combat task", "barbarian assault", "high level gambles", "clan chat", "webhook" }
)
public class TcCrewPlugin extends Plugin {
    public static final String USER_AGENT = RuneLite.USER_AGENT + " (TcCrew/1.x)";

    private @Inject ChatMessageManager chatManager;

    private @Inject DinkPluginConfig config;
    private @Inject SettingsManager settingsManager;
    private @Inject ClanEventManager clanEventManager;
    private @Inject ClanChatWebhookManager clanChatWebhookManager;
    private @Inject LfgService lfgService;
    @com.google.inject.Inject(optional = true)
    private Provider<ClientToolbar> clientToolbarProvider;
    private @Inject LfgPanel lfgPanel;
    private LfgNavigationManager lfgNavigationManager;
    private @Inject ClogPbSyncManager clogPbSyncManager;
    private @Inject CollectionLogSyncButtonManager collectionLogSyncButtonManager;
    private @Inject RemoteEventManager remoteEventManager;
    private @Inject ClanEventOverlay clanEventOverlay;
    private @Inject OverlayManager overlayManager;
    private @Inject VersionManager versionManager;
    private @Inject AccountTypeTracker accountTracker;
    private @Inject WorldTypeTracker worldTracker;

    private @Inject KillCountService killCountService;

    private @Inject CollectionNotifier collectionNotifier;
    private @Inject PetNotifier petNotifier;
    private @Inject LevelNotifier levelNotifier;
    private @Inject LootNotifier lootNotifier;
    private @Inject DeathNotifier deathNotifier;
    private @Inject SlayerNotifier slayerNotifier;
    private @Inject QuestNotifier questNotifier;
    private @Inject ClueNotifier clueNotifier;
    private @Inject SpeedrunNotifier speedrunNotifier;
    private @Inject LeaguesNotifier leaguesNotifier;
    private @Inject KillCountNotifier killCountNotifier;
    private @Inject CombatTaskNotifier combatTaskNotifier;
    private @Inject DiaryNotifier diaryNotifier;
    private @Inject GambleNotifier gambleNotifier;
    private @Inject PlayerKillNotifier pkNotifier;
    private @Inject GroupStorageNotifier groupStorageNotifier;
    private @Inject GrandExchangeNotifier grandExchangeNotifier;
    private @Inject MetaNotifier metaNotifier;
    private @Inject TradeNotifier tradeNotifier;
    private @Inject ChatNotifier chatNotifier;
    private @Inject ExternalPluginNotifier externalNotifier;

    private final AtomicReference<GameState> gameState = new AtomicReference<>();

    private Map<String, Runnable> configDisabledTasks;

    @Inject
    protected void init() {
        // clear out state that could be stale if notifier is enabled again
        this.configDisabledTasks = Map.of(
            "collectionLogEnabled", collectionNotifier::reset,
            "diaryEnabled", diaryNotifier::reset,
            "levelEnabled", levelNotifier::reset,
            "speedrunEnabled", speedrunNotifier::reset
        );
    }

    @Override
    protected void startUp() {
        log.debug("Started up Dink");
        settingsManager.init();
        clanEventManager.init();
        clanChatWebhookManager.startUp();
        lfgService.attachPanel(lfgPanel);
        if (lfgNavigationManager == null && clientToolbarProvider != null)
        {
            lfgNavigationManager = new LfgNavigationManager(clientToolbarProvider.get(), lfgPanel, config);
        }
        lfgService.startUp();
        if (lfgNavigationManager != null)
        {
            lfgNavigationManager.startUp();
        }
        clogPbSyncManager.start();
        collectionLogSyncButtonManager.startUp();
        remoteEventManager.startUp();
        overlayManager.add(clanEventOverlay);
        versionManager.onStart();
        accountTracker.init();
        worldTracker.init();
        lootNotifier.init();
        deathNotifier.init();
        chatNotifier.init();
        leaguesNotifier.init();
    }

    @Override
    protected void shutDown() {
        log.debug("Shutting down Dink");
        this.resetNotifiers();
        overlayManager.remove(clanEventOverlay);
        collectionLogSyncButtonManager.shutDown();
        if (lfgNavigationManager != null) {
            lfgNavigationManager.shutDown();
        }
        lfgService.shutDown();
        clanChatWebhookManager.shutDown();
        clogPbSyncManager.shutdown();
        remoteEventManager.shutDown();
        gameState.lazySet(null);
        accountTracker.clear();
        worldTracker.clear();
        metaNotifier.reset();
    }

    void resetNotifiers() {
        collectionNotifier.reset();
        petNotifier.reset();
        clueNotifier.reset();
        diaryNotifier.reset();
        levelNotifier.reset();
        deathNotifier.reset();
        slayerNotifier.reset();
        killCountNotifier.reset();
        groupStorageNotifier.reset();
        speedrunNotifier.reset();
        tradeNotifier.reset();
        chatNotifier.reset();
    }

    @Provides
    DinkPluginConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DinkPluginConfig.class);
    }

    @Subscribe
    public void onAccountHashChanged(AccountHashChanged event) {
        accountTracker.onAccountChange();
        grandExchangeNotifier.onAccountChange();
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted event) {
        settingsManager.onCommand(event);
        remoteEventManager.onCommand(event);
        chatNotifier.onCommand(event);
        lfgService.onCommandExecuted(event);
    }

    @Subscribe
    public void onChatboxInput(ChatboxInput event) {
        clogPbSyncManager.onChatboxInput(event);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!SettingsManager.CONFIG_GROUP.equals(event.getGroup())) {
            clogPbSyncManager.onConfigChanged(event);
            return;
        }

        settingsManager.onConfigChanged(event);
        clanEventManager.onConfigChanged(event.getKey());
        clanChatWebhookManager.onConfigChanged(event.getKey());
        lfgService.onConfigChanged(event);
        if (lfgNavigationManager != null) {
            lfgNavigationManager.onConfigChanged(event);
        }
        clogPbSyncManager.onConfigChanged(event);
        remoteEventManager.onConfigChanged(event.getKey());
        accountTracker.onConfig(event.getKey());
        worldTracker.onConfig(event.getKey());
        lootNotifier.onConfigChanged(event.getKey(), event.getNewValue());
        deathNotifier.onConfigChanged(event.getKey(), event.getNewValue());
        chatNotifier.onConfig(event.getKey());

        if ("false".equals(event.getNewValue())) {
            Runnable task = configDisabledTasks.get(event.getKey());
            if (task != null) task.run();
        }
    }

    @Subscribe
    public void onUsernameChanged(UsernameChanged usernameChanged) {
        levelNotifier.reset();
        killCountService.reset();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        GameState newState = gameStateChanged.getGameState();
        if (newState == GameState.LOADING) {
            // an intermediate state that is irrelevant for our notifiers; ignore
            return;
        }

        GameState previousState = gameState.getAndSet(newState);
        if (previousState == newState) {
            // no real change occurred (just momentarily went through LOADING); ignore
            return;
        }

        versionManager.onGameState(previousState, newState);
        settingsManager.onGameState(previousState, newState);
        lfgService.onGameStateChanged(gameStateChanged);
        clogPbSyncManager.onGameStateChanged(gameStateChanged);
        collectionNotifier.onGameState(newState);
        levelNotifier.onGameStateChanged(gameStateChanged);
        diaryNotifier.onGameState(gameStateChanged);
        grandExchangeNotifier.onGameStateChange(gameStateChanged);
        metaNotifier.onGameState(previousState, newState);
    }

    @Subscribe
    public void onStatChanged(StatChanged statChange) {
        levelNotifier.onStatChanged(statChange);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        settingsManager.onTick();
        clanEventManager.onTick();
        accountTracker.onTick();
        worldTracker.onTick();
        collectionNotifier.onTick();
        petNotifier.onTick();
        clueNotifier.onTick();
        slayerNotifier.onTick();
        levelNotifier.onTick();
        combatTaskNotifier.onTick();
        diaryNotifier.onTick();
        killCountNotifier.onTick();
        pkNotifier.onTick();
        grandExchangeNotifier.onTick();
        chatNotifier.onTick();
        metaNotifier.onTick();
    }

    @Subscribe(priority = 1) // run before the base loot tracker plugin
    public void onChatMessage(ChatMessage message) {
        clogPbSyncManager.onChatMessage(message);
        clanChatWebhookManager.onChatMessage(message);
        String chatMessage = Utils.sanitize(message.getMessage());
        String source = message.getName() != null && !message.getName().isEmpty() ? message.getName() : message.getSender();
        chatNotifier.onMessage(message.getType(), source, chatMessage);
        switch (message.getType()) {
            case GAMEMESSAGE:
                if ("runelite".equals(source)) {
                    // filter out plugin-sourced chat messages
                    return;
                }

                collectionNotifier.onChatMessage(chatMessage);
                lootNotifier.onGameMessage(chatMessage);
                petNotifier.onChatMessage(chatMessage);
                killCountService.onGameMessage(chatMessage);
                slayerNotifier.onChatMessage(chatMessage);
                clueNotifier.onChatMessage(chatMessage);
                killCountNotifier.onGameMessage(chatMessage);
                combatTaskNotifier.onGameMessage(chatMessage);
                deathNotifier.onGameMessage(chatMessage);
                speedrunNotifier.onGameMessage(chatMessage);
                leaguesNotifier.onGameMessage(chatMessage);
                break;

            case FRIENDSCHATNOTIFICATION:
                killCountNotifier.onFriendsChatNotification(chatMessage);
                // intentional fallthrough to clan notifications

            case CLAN_MESSAGE:
            case CLAN_GUEST_MESSAGE:
            case CLAN_GIM_MESSAGE:
                petNotifier.onClanNotification(chatMessage);
                break;

            case MESBOX:
                diaryNotifier.onMessageBox(chatMessage);
                gambleNotifier.onMesBoxNotification(chatMessage);
                break;

            case TRADE:
                tradeNotifier.onTradeMessage(chatMessage);
                break;

            default:
                // do nothing
                break;
        }
    }

    @Subscribe(priority = 1) // run before the base GE plugin
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event) {
        grandExchangeNotifier.onOfferChange(event.getSlot(), event.getOffer());
    }

    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged event) {
        clanChatWebhookManager.onClanChannelChanged(event);
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        pkNotifier.onHitsplat(event);
    }

    @Subscribe
    public void onActorDeath(ActorDeath actor) {
        deathNotifier.onActorDeath(actor);
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        deathNotifier.onInteraction(event);
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event) {
        collectionNotifier.onScript(event.getScriptId());
        petNotifier.onScript(event.getScriptId());
        deathNotifier.onScript(event);
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        clogPbSyncManager.onScriptPostFired(event);
    }

    @Subscribe(priority = 1) // run before the base loot tracker plugin
    public void onServerNpcLoot(ServerNpcLoot event) {
        // temporarily only use new event when needed
        int npcId = event.getComposition().getId();
        var name = event.getComposition().getName();
        if (!LootNotifier.SERVER_LOOT_NPC_IDS.contains(npcId) && !name.startsWith("Hallowed Sepulchre")) {
            return;
        }

        killCountService.onServerNpcLoot(event);
        lootNotifier.onServerNpcLoot(event);
    }

    @Subscribe(priority = 1) // run before the base loot tracker plugin
    public void onNpcLootReceived(NpcLootReceived npcLootReceived) {
        if (LootNotifier.SERVER_LOOT_NPC_IDS.contains(npcLootReceived.getNpc().getId())) {
            // handled by ServerNpcLoot, but return just in case
            return;
        }

        killCountService.onNpcKill(npcLootReceived);
        lootNotifier.onNpcLootReceived(npcLootReceived);
    }

    @Subscribe
    public void onPlayerLootReceived(PlayerLootReceived playerLootReceived) {
        killCountService.onPlayerKill(playerLootReceived);
        lootNotifier.onPlayerLootReceived(playerLootReceived);
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event) {
        versionManager.onProfileChange();
    }

    @Subscribe
    public void onLootReceived(LootReceived lootReceived) {
        killCountService.onLoot(lootReceived);
        lootNotifier.onLootReceived(lootReceived);
    }

    @Subscribe
    public void onNotificationFired(NotificationFired event) {
        chatNotifier.onNotification(event);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        settingsManager.onVarbitChanged(event);
        accountTracker.onVarbit(event);
        metaNotifier.onVarbit(event);
        clogPbSyncManager.onVarbitChanged(event);
        collectionNotifier.onVarPlayer(event);
        diaryNotifier.onVarbitChanged(event);
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        metaNotifier.onWidget(event);
        killCountService.onWidget(event);
        clogPbSyncManager.onWidgetLoaded(event);
        questNotifier.onWidgetLoaded(event);
        clueNotifier.onWidgetLoaded(event);
        speedrunNotifier.onWidgetLoaded(event);
        groupStorageNotifier.onWidgetLoad(event);
        killCountNotifier.onWidget(event);
        tradeNotifier.onWidgetLoad(event);
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event) {
        clogPbSyncManager.onWidgetClosed(event);
        groupStorageNotifier.onWidgetClose(event);
        tradeNotifier.onWidgetClose(event);
    }

    @Subscribe
    public void onWorldChanged(WorldChanged event) {
        worldTracker.onWorldChange();
    }

    @Subscribe
    public void onPluginMessage(PluginMessage event) {
        if ("dink".equalsIgnoreCase(event.getNamespace()) && "notify".equalsIgnoreCase(event.getName())) {
            externalNotifier.onNotify(event.getData());
        }
    }

    @Subscribe
    public void onWallObjectSpawned(WallObjectSpawned event) {
        metaNotifier.onWallObjectSpawned(event);
    }

    public void addChatSuccess(String message) {
        addChatMessage("Success", Utils.GREEN, message);
    }

    public void addChatWarning(String message) {
        addChatMessage("Warning", Utils.RED, message);
    }

    void addChatMessage(String category, Color color, String message) {
        String formatted = String.format("[%s] %s: %s",
            ColorUtil.wrapWithColorTag(getName(), Utils.PINK),
            category,
            ColorUtil.wrapWithColorTag(message, color)
        );

        chatManager.queue(
            QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(formatted)
                .build()
        );
    }
}

