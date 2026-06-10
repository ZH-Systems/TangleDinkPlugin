package tccrewplugin;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.CommandExecuted;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoteEventManagerTest {

    private static final String EVENT_ID = "event-1";
    private static final String MIGRATION_URL = "https://example.com/migration.json";

    private Client client;
    private TcCrewPlugin plugin;
    private DinkPluginConfig config;
    private SettingsManager settingsManager;
    private ScheduledExecutorService executor;
    private RemoteEventConfig remoteConfig;
    private RemoteEventMigration migration;
    private TestRemoteEventManager manager;

    @BeforeEach
    void setUp() {
        client = mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);

        plugin = mock(TcCrewPlugin.class);
        config = mock(DinkPluginConfig.class);
        when(config.remoteEventConfigUrl()).thenReturn("https://example.com/remote-config.json");
        when(config.remoteEventAppliedEventIds()).thenReturn("");
        when(config.remoteEventLastPromptedEventId()).thenReturn("");
        settingsManager = mock(SettingsManager.class);
        executor = mock(ScheduledExecutorService.class);

        remoteConfig = new RemoteEventConfig(
            1,
            new RemoteEventConfig.Event(
                true,
                EVENT_ID,
                "Example Event",
                "::DinkEvent event enable " + EVENT_ID,
                MIGRATION_URL,
                "The event is live"
            )
        );

        Map<String, Object> migrationConfig = new LinkedHashMap<>();
        migrationConfig.put("clanEventEnabled", true);
        migrationConfig.put("clanEventWebhook", "https://fetched.example/webhook");
        migrationConfig.put("clanEventEndTime", "2026-06-09T22:00:00Z");
        migrationConfig.put("clanEventSecretCode", "FETCHED-CODE");
        migrationConfig.put("killCountEnabled", true);
        migrationConfig.put("minLootValue", 12345);
        migrationConfig.put("customKey", "customValue");
        migration = new RemoteEventMigration(1, EVENT_ID, migrationConfig);

        manager = new TestRemoteEventManager(
            new Gson(),
            client,
            plugin,
            config,
            settingsManager,
            executor,
            new OkHttpClient(),
            remoteConfig,
            migration
        );
    }

    @Test
    void enableKeepsFetchedPayloadUnchanged() {
        manager.onCommand(new CommandExecuted("DinkEvent", new String[]{"event", "enable", EVENT_ID}));

        assertEquals(1, manager.remoteConfigFetchCount);
        assertEquals(1, manager.migrationFetchCount);
        assertTrue(manager.remoteConfigInteractive);
        assertTrue(manager.migrationInteractive);
        assertSame(remoteConfig.getEvent(), manager.migrationEvent);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(settingsManager).clearConfigValue("clanEventEndTime");
        verify(settingsManager).applyImportedConfig(captor.capture(), eq(true));
        Map<String, Object> applied = captor.getValue();
        assertEquals(true, applied.get("clanEventEnabled"));
        assertEquals("https://fetched.example/webhook", applied.get("clanEventWebhook"));
        assertEquals("2026-06-09T22:00:00Z", applied.get("clanEventEndTime"));
        assertEquals("FETCHED-CODE", applied.get("clanEventSecretCode"));
        assertEquals(true, applied.get("killCountEnabled"));
        assertEquals(12345, applied.get("minLootValue"));
        assertEquals("customValue", applied.get("customKey"));
        verify(config).setClanEventEnabled(true);
        verify(plugin).addChatSuccess("Enabled remote clan event: Example Event");
        verify(plugin, never()).addChatWarning("Failed to enable remote clan event.");
    }

    @Test
    void disableUsesSameFetchPipelineAndForcesDisabledValues() {
        manager.onCommand(new CommandExecuted("DinkEvent", new String[]{"event", "disable", EVENT_ID}));

        assertEquals(1, manager.remoteConfigFetchCount);
        assertEquals(1, manager.migrationFetchCount);
        assertTrue(manager.remoteConfigInteractive);
        assertTrue(manager.migrationInteractive);
        assertSame(remoteConfig.getEvent(), manager.migrationEvent);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(settingsManager).clearConfigValue("clanEventEndTime");
        verify(settingsManager).applyImportedConfig(captor.capture(), eq(true));
        Map<String, Object> applied = captor.getValue();
        assertEquals(false, applied.get("clanEventEnabled"));
        assertEquals("No Event happening right now", applied.get("clanEventWebhook"));
        assertEquals("", applied.get("clanEventEndTime"));
        assertEquals("", applied.get("clanEventSecretCode"));
        assertEquals(false, applied.get("killCountEnabled"));
        assertEquals(5000000, applied.get("minLootValue"));
        assertEquals("customValue", applied.get("customKey"));
        verify(config).setClanEventEnabled(false);
        verify(plugin).addChatSuccess("Disabled remote clan event: Example Event");
    }

    @Test
    void disableOverridesFetchedPayloadValuesForShutdownKeys() {
        Map<String, Object> fetchedValues = new LinkedHashMap<>();
        fetchedValues.put("clanEventEnabled", true);
        fetchedValues.put("clanEventWebhook", "https://unexpected.example/webhook");
        fetchedValues.put("clanEventEndTime", "2026-06-09T22:00:00Z");
        fetchedValues.put("clanEventSecretCode", "FETCHED-CODE");
        fetchedValues.put("killCountEnabled", true);
        fetchedValues.put("minLootValue", 999999999);
        fetchedValues.put("customKey", "customValue");
        manager = new TestRemoteEventManager(
            new Gson(),
            client,
            plugin,
            config,
            settingsManager,
            executor,
            new OkHttpClient(),
            remoteConfig,
            new RemoteEventMigration(1, EVENT_ID, fetchedValues)
        );

        manager.onCommand(new CommandExecuted("DinkEvent", new String[]{"event", "disable", EVENT_ID}));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(settingsManager).applyImportedConfig(captor.capture(), eq(true));
        Map<String, Object> applied = captor.getValue();
        assertEquals(false, applied.get("clanEventEnabled"));
        assertEquals("No Event happening right now", applied.get("clanEventWebhook"));
        assertEquals("", applied.get("clanEventEndTime"));
        assertEquals("", applied.get("clanEventSecretCode"));
        assertEquals(false, applied.get("killCountEnabled"));
        assertEquals(5000000, applied.get("minLootValue"));
        assertEquals("customValue", applied.get("customKey"));
        assertFalse(Boolean.TRUE.equals(applied.get("clanEventEnabled")));
    }

    private static final class TestRemoteEventManager extends RemoteEventManager {
        private final RemoteEventConfig remoteConfig;
        private final RemoteEventMigration migration;
        private int remoteConfigFetchCount;
        private int migrationFetchCount;
        private boolean remoteConfigInteractive;
        private boolean migrationInteractive;
        private RemoteEventConfig.Event migrationEvent;

        private TestRemoteEventManager(
            Gson gson,
            Client client,
            TcCrewPlugin plugin,
            DinkPluginConfig config,
            SettingsManager settingsManager,
            ScheduledExecutorService executor,
            OkHttpClient httpClient,
            RemoteEventConfig remoteConfig,
            RemoteEventMigration migration
        ) {
            super(gson, client, plugin, config, settingsManager, executor, httpClient);
            this.remoteConfig = remoteConfig;
            this.migration = migration;
        }

        @Override
        CompletableFuture<RemoteEventConfig> fetchRemoteConfig(boolean interactive) {
            remoteConfigFetchCount++;
            remoteConfigInteractive = interactive;
            return CompletableFuture.completedFuture(remoteConfig);
        }

        @Override
        CompletableFuture<RemoteEventMigration> fetchMigration(RemoteEventConfig.Event event, boolean interactive) {
            migrationFetchCount++;
            migrationInteractive = interactive;
            migrationEvent = event;
            return CompletableFuture.completedFuture(migration);
        }
    }
}
