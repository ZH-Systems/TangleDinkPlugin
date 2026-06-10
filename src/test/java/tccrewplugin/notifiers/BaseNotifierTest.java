package tccrewplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import tccrewplugin.message.NotificationBody;
import tccrewplugin.message.NotificationType;
import tccrewplugin.message.templating.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.mockito.Mockito.when;

class BaseNotifierTest extends MockedNotifierTest {

    private static final String OVERRIDE_WEBHOOK_URL = "https://example.com/normal";
    private static final String CLAN_EVENT_WEBHOOK_URL = "https://example.com/clan-event";

    @Bind
    @InjectMocks
    TestNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    @Test
    void testClanEventWebhookOverridesNotifierWebhook() {
        when(config.clanEventEnabled()).thenReturn(true);
        when(config.clanEventWebhook()).thenReturn(CLAN_EVENT_WEBHOOK_URL);
        when(config.clanEventEndTime()).thenReturn("");

        NotificationBody<?> body = notifier.send();

        verifyCreateMessage(CLAN_EVENT_WEBHOOK_URL, false, body);
    }

    @Test
    void testBlankClanEventWebhookPreservesExistingRouting() {
        when(config.clanEventEnabled()).thenReturn(true);
        when(config.clanEventWebhook()).thenReturn("");
        when(config.clanEventEndTime()).thenReturn("");

        NotificationBody<?> body = notifier.send();

        verifyCreateMessage(OVERRIDE_WEBHOOK_URL, false, body);
    }

    static class TestNotifier extends BaseNotifier {

        @Override
        protected String getWebhookUrl() {
            return OVERRIDE_WEBHOOK_URL;
        }

        NotificationBody<?> send() {
            NotificationBody<?> body = NotificationBody.builder()
                .type(NotificationType.LEVEL)
                .text(Template.builder().template("Clan event test").build())
                .build();
            createMessage(false, body);
            return body;
        }
    }
}

