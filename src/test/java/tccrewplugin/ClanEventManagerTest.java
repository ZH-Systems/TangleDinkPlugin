package tccrewplugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClanEventManagerTest {

    private DinkPluginConfig config;
    private TcCrewPlugin plugin;
    private ClanEventManager manager;

    @BeforeEach
    void setUp() {
        config = Mockito.mock(DinkPluginConfig.class);
        plugin = Mockito.mock(TcCrewPlugin.class);
        manager = new ClanEventManager(config, plugin);
    }

    @Test
    void testActiveEventWithWebhook() {
        when(config.clanEventEnabled()).thenReturn(true);
        when(config.clanEventWebhook()).thenReturn("https://example.com/event");
        when(config.clanEventEndTime()).thenReturn(Instant.now().plusSeconds(60).toString());

        assertTrue(manager.isActive());
        assertNotNull(manager.getActiveWebhookOverride());
    }

    @Test
    void testExpiredEventDisablesConfig() {
        when(config.clanEventEnabled()).thenReturn(true);
        when(config.clanEventEndTime()).thenReturn(Instant.now().minusSeconds(60).toString());

        assertFalse(manager.isActive());
        manager.onTick();

        verify(config).setClanEventEnabled(false);
        verify(plugin).addChatSuccess("Clan event ended; Dink webhooks have reverted to their normal configuration.");
    }

    @Test
    void testFutureEventDoesNotDisableConfig() {
        when(config.clanEventEnabled()).thenReturn(true);
        when(config.clanEventEndTime()).thenReturn(Instant.now().plusSeconds(60).toString());

        manager.onTick();

        verify(config, never()).setClanEventEnabled(false);
    }
}

