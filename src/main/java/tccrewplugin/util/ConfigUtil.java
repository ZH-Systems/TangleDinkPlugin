package tccrewplugin.util;

import com.google.gson.Gson;
import lombok.experimental.UtilityClass;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.util.ColorUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@UtilityClass
public class ConfigUtil {

    private final Pattern DELIM = Pattern.compile("[,;\\n]");

    public Stream<String> readDelimited(String value) {
        if (value == null) return Stream.empty();
        return DELIM.splitAsStream(value)
            .map(String::trim)
            .filter(StringUtils::isNotEmpty);
    }

    public boolean isPluginDisabled(ConfigManager configManager, String simpleLowerClassName) {
        return "false".equals(configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, simpleLowerClassName));
    }

    @Nullable
    public String toRawConfigValue(@NotNull Gson gson, @NotNull Object in) {
        if (in instanceof String) {
            return (String) in;
        }
        if (in instanceof Number || in instanceof Boolean || in instanceof Character) {
            return String.valueOf(in);
        }
        if (in instanceof Enum<?>) {
            return ((Enum<?>) in).name();
        }
        if (in instanceof Color) {
            return ColorUtil.colorToHexCode((Color) in);
        }
        if (in instanceof Instant || in instanceof Duration) {
            return in.toString();
        }
        if (in instanceof Collection<?>) {
            return gson.toJson(in);
        }
        return gson.toJson(in);
    }

    public boolean isSettingsOpen(@NotNull Client client) {
        Widget widget = client.getWidget(InterfaceID.Settings.UNIVERSE);
        return widget != null && !widget.isHidden();
    }

    public boolean isKillCountFilterInvalid(int varbitValue) {
        // spam filter must be disabled for kill count chat message
        return varbitValue > 0;
    }

    public boolean isCollectionLogInvalid(int varbitValue) {
        // collection log notifier requires chat or pop-up notification
        return varbitValue == 0;
    }

    public boolean isRepeatPopupInvalid(int varbitValue) {
        // we discourage repeat notifications for combat task notifier if unintentional
        return varbitValue > 0;
    }

    public boolean isPetLootInvalid(int varbitValue) {
        // LOOT_DROP_NOTIFICATIONS and UNTRADEABLE_LOOT_DROPS must both be set to 1 for reliable pet name parsing
        return varbitValue < 1;
    }
}

