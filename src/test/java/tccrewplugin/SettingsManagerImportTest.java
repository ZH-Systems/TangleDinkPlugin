package tccrewplugin;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettingsManagerImportTest {

    private DinkPluginConfig config;
    private ConfigManager configManager;
    private SettingsManager settingsManager;

    @BeforeEach
    void setUp() {
        config = mock(DinkPluginConfig.class);
        Client client = mock(Client.class);
        ClientThread clientThread = mock(ClientThread.class);
        TcCrewPlugin plugin = mock(TcCrewPlugin.class);
        configManager = mock(ConfigManager.class);

        when(config.filteredNames()).thenReturn("");
        when(config.importPolicy()).thenReturn(EnumSet.noneOf(tccrewplugin.domain.ConfigImportPolicy.class));

        ConfigDescriptor descriptor = new ConfigDescriptor(
            DinkPluginConfig.class.getAnnotation(ConfigGroup.class),
            List.of(),
            Arrays.asList(
                descriptor("filteredNames"),
                descriptor("clanEventEnabled"),
                descriptor("clanEventWebhook"),
                descriptor("clanEventEndTime"),
                descriptor("clanEventSecretCode"),
                descriptor("ignoredNames")
            )
        );
        when(configManager.getConfigDescriptor(config)).thenReturn(descriptor);

        settingsManager = new SettingsManager(new Gson(), client, clientThread, plugin, config, configManager, new OkHttpClient());
    }

    @Test
    void importedClanEventWebhookReplacesOldValue() {
        when(configManager.getConfiguration(eq(SettingsManager.CONFIG_GROUP), eq("clanEventWebhook"), eq(String.class))).thenReturn("old-value");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clanEventWebhook", "new-value");

        settingsManager.applyImportedConfig(map, true);

        verify(configManager).setConfiguration(SettingsManager.CONFIG_GROUP, "clanEventWebhook", "new-value");
    }

    @Test
    void importedClanEventWebhookEmptyStringClearsOldValue() {
        when(configManager.getConfiguration(eq(SettingsManager.CONFIG_GROUP), eq("clanEventWebhook"), eq(String.class))).thenReturn("old-value");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clanEventWebhook", "");

        settingsManager.applyImportedConfig(map, true);

        verify(configManager).setConfiguration(SettingsManager.CONFIG_GROUP, "clanEventWebhook", "");
    }

    @Test
    void importedClanEventEnabledFalseOverwritesTrue() {
        when(configManager.getConfiguration(eq(SettingsManager.CONFIG_GROUP), eq("clanEventEnabled"), eq(boolean.class))).thenReturn(true);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clanEventEnabled", false);

        settingsManager.applyImportedConfig(map, true);

        verify(configManager).setConfiguration(SettingsManager.CONFIG_GROUP, "clanEventEnabled", false);
    }

    @Test
    void importedClanEventEndTimeEmptyStringClearsOldValue() {
        when(configManager.getConfiguration(eq(SettingsManager.CONFIG_GROUP), eq("clanEventEndTime"), eq(String.class))).thenReturn("2026-06-09T22:00:00Z");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clanEventEndTime", "");

        settingsManager.applyImportedConfig(map, true);

        verify(configManager).setConfiguration(SettingsManager.CONFIG_GROUP, "clanEventEndTime", "");
    }

    @Test
    void importedNonWebhookSettingsStillMerge() {
        when(configManager.getConfiguration(eq(SettingsManager.CONFIG_GROUP), eq("ignoredNames"), eq(String.class))).thenReturn("alice");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ignoredNames", "bob");

        settingsManager.applyImportedConfig(map, true);

        verify(configManager).setConfiguration(SettingsManager.CONFIG_GROUP, "ignoredNames", "alice\nbob");
    }

    private static ConfigItemDescriptor descriptor(String methodName) {
        ConfigItem item = mock(ConfigItem.class);
        when(item.keyName()).thenReturn(methodName);
        when(item.hidden()).thenReturn(false);
        when(item.section()).thenReturn("");

        ConfigItemDescriptor descriptor = mock(ConfigItemDescriptor.class);
        when(descriptor.getItem()).thenReturn(item);
        when(descriptor.key()).thenReturn(methodName);
        return descriptor;
    }
}
