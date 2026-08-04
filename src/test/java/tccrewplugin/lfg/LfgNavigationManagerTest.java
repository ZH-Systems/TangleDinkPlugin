package tccrewplugin.lfg;

import net.runelite.client.ui.ClientToolbar;
import org.junit.jupiter.api.Test;
import tccrewplugin.DinkPluginConfig;

import com.google.inject.util.Providers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LfgNavigationManagerTest
{
	@Test
	void doesNotDuplicateNavigationButton()
	{
		ClientToolbar toolbar = mock(ClientToolbar.class);
		LfgPanel panel = mock(LfgPanel.class);
		DinkPluginConfig config = mock(DinkPluginConfig.class);
		when(config.lfgEnabled()).thenReturn(true);

		LfgNavigationManager manager = new LfgNavigationManager(toolbar, panel, config);
		manager.startUp();
		manager.startUp();

		verify(toolbar, times(1)).addNavigation(any());
	}

	@Test
	void removesNavigationButtonOnShutdown()
	{
		ClientToolbar toolbar = mock(ClientToolbar.class);
		LfgPanel panel = mock(LfgPanel.class);
		DinkPluginConfig config = mock(DinkPluginConfig.class);
		when(config.lfgEnabled()).thenReturn(true);

		LfgNavigationManager manager = new LfgNavigationManager(toolbar, panel, config);
		manager.startUp();
		manager.shutDown();

		verify(toolbar).removeNavigation(any());
	}
}
