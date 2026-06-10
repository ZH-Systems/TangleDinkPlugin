package tccrewplugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import tccrewplugin.domain.RemoteEventConfig;
import tccrewplugin.domain.RemoteEventMigration;
import tccrewplugin.util.ConfigProxyAuth;
import tccrewplugin.util.ConfigProxyServer;
import tccrewplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.CommandExecuted;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class RemoteEventManager {
    private static final String COMMAND = "DinkEvent";
    private static final int REMOTE_CONFIG_VERSION = 1;
    private static final int REMOTE_MIGRATION_VERSION = 1;
    private static final Pattern DELIM = Pattern.compile("[,;\\n]");

    private final Gson gson;
    private final Client client;
    private final TcCrewPlugin plugin;
    private final DinkPluginConfig config;
    private final SettingsManager settingsManager;
    private final ScheduledExecutorService executor;
    private final OkHttpClient httpClient;

    private final AtomicBoolean pollInFlight = new AtomicBoolean();

    private volatile ScheduledFuture<?> pollTask;

    @Inject
    public RemoteEventManager(
        Gson gson,
        Client client,
        TcCrewPlugin plugin,
        DinkPluginConfig config,
        SettingsManager settingsManager,
        ScheduledExecutorService executor,
        OkHttpClient httpClient
    ) {
        this.gson = gson;
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.settingsManager = settingsManager;
        this.executor = executor;
        this.httpClient = httpClient.newBuilder()
            .proxySelector(new ConfigProxyServer(config))
            .proxyAuthenticator(new ConfigProxyAuth(config))
            .build();
    }

    void startUp() {
        reschedulePolling();
    }

    void shutDown() {
        ScheduledFuture<?> task = pollTask;
        pollTask = null;
        if (task != null) {
            task.cancel(false);
        }
        pollInFlight.set(false);
    }

    void onConfigChanged(String key) {
        if ("remoteEventPollEnabled".equals(key)
            || "remoteEventPollIntervalSeconds".equals(key)
            || "remoteEventConfigUrl".equals(key)) {
            reschedulePolling();
        }
    }

    void onCommand(CommandExecuted event) {
        if (!COMMAND.equalsIgnoreCase(event.getCommand())) {
            return;
        }

        String[] args = event.getArguments();
        if (args == null || args.length < 3 || !"event".equalsIgnoreCase(args[0])) {
            plugin.addChatWarning(COMMAND_USAGE);
            return;
        }

        if ("enable".equalsIgnoreCase(args[1])) {
            enableEvent(args[2]);
        } else if ("disable".equalsIgnoreCase(args[1])) {
            disableEvent(args[2]);
        } else {
            plugin.addChatWarning(COMMAND_USAGE);
        }
    }

    private void reschedulePolling() {
        shutDown();
        if (!config.remoteEventPollEnabled()) {
            return;
        }

        long interval = Math.max(10L, config.remoteEventPollIntervalSeconds());
        pollTask = executor.scheduleWithFixedDelay(this::pollRemoteConfig, 0L, interval, TimeUnit.SECONDS);
    }

    private void pollRemoteConfig() {
        if (!pollInFlight.compareAndSet(false, true)) {
            return;
        }

        fetchRemoteConfig(false)
            .thenAccept(this::handleRemoteConfig)
            .exceptionally(t -> {
                log.warn("Failed to poll remote event config", unwrap(t));
                return null;
            })
            .whenComplete((ignored, t) -> pollInFlight.set(false));
    }

    private void handleRemoteConfig(RemoteEventConfig remoteConfig) {
        config.setRemoteEventLastSeenVersion(remoteConfig.getVersion());

        RemoteEventConfig.Event event = remoteConfig.getEvent();
        if (event == null || !event.isActive()) {
            return;
        }

        String eventId = normalizeEventId(event.getEventId());
        if (eventId == null || isApplied(eventId) || eventId.equals(normalizeEventId(config.remoteEventLastPromptedEventId()))) {
            return;
        }

        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        config.setRemoteEventLastPromptedEventId(eventId);
        plugin.addChatWarning(buildPromptMessage(event));
    }

    private void enableEvent(String requestedEventId) {
        applyRemoteEvent(requestedEventId, false);
    }

    private void disableEvent(String requestedEventId) {
        applyRemoteEvent(requestedEventId, true);
    }

    private void applyRemoteEvent(String requestedEventId, boolean disable) {
        String normalizedEventId = normalizeEventId(requestedEventId);
        if (normalizedEventId == null) {
            plugin.addChatWarning(disable ? "Please specify an event ID to disable." : "Please specify an event ID to enable.");
            return;
        }

        if (isApplied(normalizedEventId)) {
            plugin.addChatWarning("That remote clan event has already been applied on this profile.");
            return;
        }

        fetchRemoteConfig(true)
            .thenCompose(remoteConfig -> {
                RemoteEventConfig.Event event = validateActiveEvent(remoteConfig, normalizedEventId);
                String eventName = StringUtils.defaultIfBlank(StringUtils.trimToNull(event.getName()), normalizedEventId);
                return fetchMigration(event, true)
                    .thenAccept(migration -> applyMigration(normalizedEventId, eventName, migration, disable));
            })
            .exceptionally(t -> {
                String message = unwrap(t).getMessage();
                plugin.addChatWarning(StringUtils.defaultIfBlank(message, disable ? "Failed to disable remote clan event." : "Failed to enable remote clan event."));
                return null;
            });
    }

    CompletableFuture<RemoteEventConfig> fetchRemoteConfig(boolean interactive) {
        String url = StringUtils.trimToEmpty(config.remoteEventConfigUrl());
        if (url.isEmpty()) {
            return failedFuture("Remote event config URL is blank.");
        }

        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null || !"https".equalsIgnoreCase(httpUrl.scheme())) {
            return failedFuture("Remote event config URL must be a valid HTTPS URL.");
        }

        return Utils.readJson(httpClient, gson, httpUrl.toString(), new TypeToken<RemoteEventConfig>() {})
            .thenApply(this::validateRemoteConfig)
            .exceptionally(t -> {
                Throwable cause = unwrap(t);
                if (interactive) {
                    throw new CompletionException(new IllegalArgumentException(
                        StringUtils.defaultIfBlank(cause.getMessage(), "Failed to read remote event config."),
                        cause
                    ));
                }
                throw new CompletionException(cause);
            });
    }

    private RemoteEventConfig validateRemoteConfig(RemoteEventConfig remoteConfig) {
        if (remoteConfig == null) {
            throw new IllegalArgumentException("Remote event config response was empty.");
        }

        if (remoteConfig.getVersion() != REMOTE_CONFIG_VERSION) {
            throw new IllegalArgumentException("Unsupported remote event config version: " + remoteConfig.getVersion());
        }

        RemoteEventConfig.Event event = remoteConfig.getEvent();
        if (event == null || !event.isActive()) {
            return remoteConfig;
        }

        String eventId = normalizeEventId(event.getEventId());
        if (eventId == null) {
            throw new IllegalArgumentException("Remote event config is missing an active event ID.");
        }

        if (StringUtils.isBlank(event.getMigrationUrl())) {
            throw new IllegalArgumentException("Remote event config is missing a migration URL for the active event.");
        }

        String expectedCommand = buildExpectedCommand(eventId);
        String configuredCommand = StringUtils.trimToNull(event.getRequiredCommand());
        if (configuredCommand != null && !expectedCommand.equalsIgnoreCase(configuredCommand)) {
            log.warn("Remote event command mismatch for {}. Expected '{}', received '{}'", eventId, expectedCommand, configuredCommand);
        }

        return remoteConfig;
    }

    private RemoteEventConfig.Event validateActiveEvent(RemoteEventConfig remoteConfig, String requestedEventId) {
        RemoteEventConfig.Event event = remoteConfig.getEvent();
        if (event == null || !event.isActive()) {
            throw new IllegalArgumentException("No active remote clan event is currently available.");
        }

        String activeEventId = normalizeEventId(event.getEventId());
        if (!requestedEventId.equals(activeEventId)) {
            throw new IllegalArgumentException("The requested event ID does not match the currently active remote event.");
        }

        return event;
    }

    CompletableFuture<RemoteEventMigration> fetchMigration(RemoteEventConfig.Event event, boolean interactive) {
        String migrationUrl = StringUtils.trimToNull(event.getMigrationUrl());
        HttpUrl url = migrationUrl != null ? HttpUrl.parse(migrationUrl) : null;
        if (url == null || !"https".equalsIgnoreCase(url.scheme())) {
            return failedFuture("The remote event migration URL is invalid.");
        }

        String host = url.host().toLowerCase(Locale.ROOT);
        if (!allowedHosts().contains(host)) {
            log.warn("Rejected remote event migration URL host: {}", host);
            return failedFuture("The remote event migration host is not allowlisted.");
        }

        return Utils.readJson(httpClient, gson, url.toString(), new TypeToken<RemoteEventMigration>() {})
            .thenApply(migration -> validateMigration(event, migration))
            .exceptionally(t -> {
                Throwable cause = unwrap(t);
                if (interactive) {
                    throw new CompletionException(new IllegalArgumentException(
                        StringUtils.defaultIfBlank(cause.getMessage(), "Failed to read remote event migration."),
                        cause
                    ));
                }
                throw new CompletionException(cause);
            });
    }

    RemoteEventMigration validateMigration(RemoteEventConfig.Event event, RemoteEventMigration migration) {
        if (migration == null) {
            throw new IllegalArgumentException("Remote event migration response was empty.");
        }

        if (migration.getVersion() != REMOTE_MIGRATION_VERSION) {
            throw new IllegalArgumentException("Unsupported remote event migration version: " + migration.getVersion());
        }

        String eventId = normalizeEventId(event.getEventId());
        if (!eventId.equals(normalizeEventId(migration.getEventId()))) {
            throw new IllegalArgumentException("Remote event migration event ID does not match the active event.");
        }

        Map<String, Object> migrationConfig = migration.getConfig();
        if (migrationConfig == null || migrationConfig.isEmpty()) {
            throw new IllegalArgumentException("Remote event migration did not contain any config values.");
        }

        return migration;
    }

    private void applyMigration(String eventId, String eventName, RemoteEventMigration migration, boolean disable) {
        Map<String, Object> migrationConfig = new LinkedHashMap<>(migration.getConfig());
        assert migrationConfig != null;

        // Avoid immediately re-importing a nested dynamic config payload that may point at stale data.
        migrationConfig.remove(SettingsManager.DYNAMIC_IMPORT_CONFIG_KEY);
        // Clear any previous event deadline so a fresh migration without clanEventEndTime does not inherit it.
        settingsManager.clearConfigValue("clanEventEndTime");

        if (disable) {
            forceDisabledClanEventConfig(migrationConfig);
        }

        log.debug("Applying remote event migration for {} with config payload: {}", eventId, gson.toJson(migrationConfig));
        settingsManager.applyImportedConfig(migrationConfig, true);

        config.setClanEventEnabled(!disable);
        config.setRemoteEventLastPromptedEventId(eventId);

        LinkedHashSet<String> applied = appliedEvents();
        applied.add(eventId);
        config.setRemoteEventAppliedEventIds(String.join("\n", applied));

        log.debug("Applied remote clan event {}", eventId);
        plugin.addChatSuccess((disable ? "Disabled" : "Enabled") + " remote clan event: " + eventName);
    }

    private void forceDisabledClanEventConfig(Map<String, Object> migrationConfig) {
        migrationConfig.put("clanEventEnabled", false);
        migrationConfig.put("clanEventWebhook", "No Event happening right now");
        migrationConfig.put("clanEventEndTime", "");
        migrationConfig.put("clanEventSecretCode", "");
        migrationConfig.put("killCountEnabled", false);
        migrationConfig.put("minLootValue", 5000000);
    }

    private String buildPromptMessage(RemoteEventConfig.Event event) {
        String eventId = normalizeEventId(event.getEventId());
        assert eventId != null;

        String eventName = StringUtils.defaultIfBlank(StringUtils.trimToNull(event.getName()), eventId);
        String command = buildExpectedCommand(eventId);
        String message = StringUtils.trimToNull(event.getMessage());
        if (message == null) {
            return "A new clan event is available: " + eventName + ". Run " + command + " to activate it.";
        }

        String prompt = message.contains(eventName) ? message : eventName + ": " + message;
        if (prompt.contains(command)) {
            return prompt;
        }

        return prompt + " Run " + command + " to activate it.";
    }

    private String buildExpectedCommand(String eventId) {
        return COMMAND_PREFIX_ENABLE + eventId;
    }

    private Set<String> allowedHosts() {
        LinkedHashSet<String> hosts = new LinkedHashSet<>();
        readDelimited(config.allowedMigrationHosts())
            .map(host -> host.toLowerCase(Locale.ROOT))
            .forEach(hosts::add);
        hosts.add("raw.githubusercontent.com");
        return hosts;
    }

    private boolean isApplied(String eventId) {
        return appliedEvents().contains(eventId);
    }

    private LinkedHashSet<String> appliedEvents() {
        LinkedHashSet<String> applied = new LinkedHashSet<>();
        readDelimited(config.remoteEventAppliedEventIds())
            .map(RemoteEventManager::normalizeEventId)
            .filter(StringUtils::isNotBlank)
            .forEach(applied::add);
        return applied;
    }

    private static Stream<String> readDelimited(String value) {
        if (value == null) {
            return Stream.empty();
        }
        return DELIM.splitAsStream(value)
            .map(String::trim)
            .filter(StringUtils::isNotEmpty);
    }

    private static String normalizeEventId(String eventId) {
        String normalized = StringUtils.trimToNull(eventId);
        return normalized != null ? normalized.toLowerCase(Locale.ROOT) : null;
    }

    private static void validateEndTime(String value) {
        String trimmed = value.trim();
        try {
            Instant.parse(trimmed);
            return;
        } catch (DateTimeException ignored) {
            // try offset/local formats below
        }

        try {
            OffsetDateTime.parse(trimmed).toInstant();
            return;
        } catch (DateTimeException ignored) {
            // try local format below
        }

        try {
            LocalDateTime.parse(trimmed).atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Remote event migration contains an invalid clanEventEndTime.", e);
        }
    }

    private static Throwable unwrap(Throwable t) {
        Throwable current = t;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static <T> CompletableFuture<T> failedFuture(String message) {
        return CompletableFuture.failedFuture(new IllegalArgumentException(message));
    }

    private static final String COMMAND_PREFIX_ENABLE = "::" + COMMAND + " event enable ";
    private static final String COMMAND_PREFIX_DISABLE = "::" + COMMAND + " event disable ";
    private static final String COMMAND_USAGE = "Usage: " + COMMAND_PREFIX_ENABLE + "<eventId> | " + COMMAND_PREFIX_DISABLE + "<eventId>";
}

