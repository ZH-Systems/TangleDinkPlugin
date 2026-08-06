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
import tccrewplugin.domain.ConfigImportPolicy;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
                descriptor("lfgEnabled"),
                descriptor("lfgSupabaseUrl"),
                descriptor("lfgApiToken", true),
                descriptor("lfgVisibleCategories"),
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
        when(configManager.getConfiguration(SettingsManager.CONFIG_GROUP, "clanEventEnabled")).thenReturn("true");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clanEventEnabled", false);

        settingsManager.applyImportedConfig(map, true);

        verify(configManager).setConfiguration(SettingsManager.CONFIG_GROUP, "clanEventEnabled", "false");
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
        when(configManager.getConfiguration(SettingsManager.CONFIG_GROUP, "ignoredNames")).thenReturn("alice");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ignoredNames", "bob");

        settingsManager.applyImportedConfig(map, true);

        verify(configManager).setConfiguration(SettingsManager.CONFIG_GROUP, "ignoredNames", "alice\nbob");
    }

    @Test
    void importedLfgApiTokenDoesNotOverwriteWithoutPolicy() {
        when(configManager.getConfiguration(eq(SettingsManager.CONFIG_GROUP), eq("lfgApiToken"), eq(String.class))).thenReturn("old-token");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("lfgApiToken", "new-token");

        settingsManager.applyImportedConfig(map, true);

        verify(configManager, never()).setConfiguration(SettingsManager.CONFIG_GROUP, "lfgApiToken", "new-token");
    }

    @Test
    void importedLfgApiTokenOverwritesWithWebhookPolicy() {
        when(config.importPolicy()).thenReturn(EnumSet.of(ConfigImportPolicy.OVERWRITE_WEBHOOKS));
        when(configManager.getConfiguration(eq(SettingsManager.CONFIG_GROUP), eq("lfgApiToken"), eq(String.class))).thenReturn("old-token");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("lfgApiToken", "new-token");

        settingsManager.applyImportedConfig(map, true);

        verify(configManager).setConfiguration(SettingsManager.CONFIG_GROUP, "lfgApiToken", "new-token");
    }

    @Test
    void exportOmitsLfgSecretsByDefault() {
        when(configManager.getConfigurationKeys(SettingsManager.CONFIG_GROUP + ".")).thenReturn(List.of(
            SettingsManager.CONFIG_GROUP + ".lfgEnabled",
            SettingsManager.CONFIG_GROUP + ".lfgSupabaseUrl",
            SettingsManager.CONFIG_GROUP + ".lfgApiToken",
            SettingsManager.CONFIG_GROUP + ".lfgVisibleCategories"
        ));
        when(configManager.getConfiguration(SettingsManager.CONFIG_GROUP, "lfgEnabled")).thenReturn("true");
        when(configManager.getConfiguration(SettingsManager.CONFIG_GROUP, "lfgSupabaseUrl")).thenReturn("https://supabase.example");
        when(configManager.getConfiguration(SettingsManager.CONFIG_GROUP, "lfgApiToken")).thenReturn("secret-token");
        when(configManager.getConfiguration(SettingsManager.CONFIG_GROUP, "lfgVisibleCategories")).thenReturn("raid,boss");

        Map<String, Object> exported = settingsManager.buildExportConfigMap(key -> !Set.of("lfgApiToken").contains(key));

        verify(configManager).getConfigurationKeys(SettingsManager.CONFIG_GROUP + ".");
        verify(configManager).getConfiguration(SettingsManager.CONFIG_GROUP, "lfgEnabled");
        verify(configManager).getConfiguration(SettingsManager.CONFIG_GROUP, "lfgSupabaseUrl");
        verify(configManager).getConfiguration(SettingsManager.CONFIG_GROUP, "lfgVisibleCategories");
        verify(configManager, never()).getConfiguration(SettingsManager.CONFIG_GROUP, "lfgApiToken");
        // The caller-side export filter excludes the secret fields; the helper still returns the non-secret LFG values.
        assertEquals("true", exported.get("lfgEnabled"));
        assertEquals("https://supabase.example", exported.get("lfgSupabaseUrl"));
        assertEquals("raid,boss", exported.get("lfgVisibleCategories"));
        assertFalse(exported.containsKey("lfgApiToken"));
    }

    private static ConfigItemDescriptor descriptor(String methodName) {
        return descriptor(methodName, false);
    }

    private static ConfigItemDescriptor descriptor(String methodName, boolean secret) {
        ConfigItem item = mock(ConfigItem.class);
        when(item.keyName()).thenReturn(methodName);
        when(item.hidden()).thenReturn(false);
        when(item.section()).thenReturn("lfg".equals(methodName) || methodName.startsWith("lfg") ? "lfg" : "");
        when(item.secret()).thenReturn(secret);

        Class<?> type = ("clanEventEnabled".equals(methodName) || "lfgEnabled".equals(methodName)) ? boolean.class : String.class;
        return new ConfigItemDescriptor(item, type, null, null, null);
    }
}
