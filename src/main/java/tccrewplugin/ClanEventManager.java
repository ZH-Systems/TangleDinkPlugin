package tccrewplugin;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Singleton
public class ClanEventManager {

    private final DinkPluginConfig config;
    private final TcCrewPlugin plugin;

    private boolean warnedInvalidEndTime;
    private boolean notifiedEventEnded;

    @Inject
    public ClanEventManager(DinkPluginConfig config, TcCrewPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    public void init() {
        warnedInvalidEndTime = false;
        notifiedEventEnded = false;
    }

    public void onConfigChanged(String key) {
        if ("clanEventEnabled".equals(key) || "clanEventEndTime".equals(key)) {
            warnedInvalidEndTime = false;
            notifiedEventEnded = false;
        }
    }

    public void onTick() {
        if (!config.clanEventEnabled()) {
            notifiedEventEnded = false;
            return;
        }

        Instant endTime = parseEndTime();
        if (endTime != null && !Instant.now().isBefore(endTime)) {
            config.setClanEventEnabled(false);
            if (!notifiedEventEnded) {
                plugin.addChatSuccess("Clan event ended; Dink webhooks have reverted to their normal configuration.");
                notifiedEventEnded = true;
            }
        }
    }

    public boolean isActive() {
        if (!config.clanEventEnabled()) {
            return false;
        }

        Instant endTime = parseEndTime();
        return endTime == null || Instant.now().isBefore(endTime);
    }

    public @Nullable String getActiveWebhookOverride() {
        String webhook = config.clanEventWebhook();
        return isActive() && StringUtils.isNotBlank(webhook) ? webhook : null;
    }

    public @Nullable String getDisplayText() {
        String code = config.clanEventSecretCode();
        if (!isActive() || StringUtils.isBlank(code)) {
            return null;
        }

        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return "Code: " + code.trim() + " | " + date;
    }

    private @Nullable Instant parseEndTime() {
        String value = config.clanEventEndTime();
        if (StringUtils.isBlank(value)) {
            return null;
        }

        String trimmed = value.trim();
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeException ignored) {
            // try offset and local date-time formats below
        }

        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (DateTimeException ignored) {
            // try local date-time format below
        }

        try {
            return LocalDateTime.parse(trimmed).atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeException e) {
            if (!warnedInvalidEndTime) {
                warnedInvalidEndTime = true;
                log.warn("Invalid clan event end time: {}", value, e);
                plugin.addChatWarning("Clan event end time is invalid; the event will remain active until disabled.");
            }
            return null;
        }
    }
}

