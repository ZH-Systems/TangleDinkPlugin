package tccrewplugin;

import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DinkPluginConfigTest
{
    @Test
    void clogPbSectionExistsAndIsUsed()
            throws Exception
    {
        ConfigSection section = DinkPluginConfig.class.getDeclaredField("clogPbSyncSection").getAnnotation(ConfigSection.class);
        assertTrue(section != null);
        assertEquals("Clog/PB Sync", section.name());

        Method enabled = DinkPluginConfig.class.getMethod("clogPbSyncEnabled");
        ConfigItem item = enabled.getAnnotation(ConfigItem.class);
        assertTrue(item != null);
        assertEquals("clogPbSync", item.section());
    }
}
