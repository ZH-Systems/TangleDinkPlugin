package tccrewplugin.lfg;

import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import tccrewplugin.TangleCrewConfig;
import tccrewplugin.SettingsManager;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

@Singleton
public class LfgNavigationManager
{
	private final ClientToolbar clientToolbar;
	private final LfgPanel panel;
	private final TangleCrewConfig config;
	private NavigationButton button;

	@Inject
	public LfgNavigationManager(ClientToolbar clientToolbar, LfgPanel panel, TangleCrewConfig config)
	{
		this.clientToolbar = clientToolbar;
		this.panel = panel;
		this.config = config;
	}

	public void startUp()
	{
		if (config.lfgEnabled())
		{
			addButton();
		}
	}

	public void shutDown()
	{
		removeButton();
	}

	public void onConfigChanged(net.runelite.client.events.ConfigChanged event)
	{
		if (!SettingsManager.CONFIG_GROUP.equals(event.getGroup()) || !"lfgEnabled".equals(event.getKey()))
		{
			return;
		}
		if (Boolean.parseBoolean(event.getNewValue()))
		{
			addButton();
		}
		else
		{
			removeButton();
		}
	}

	private void addButton()
	{
		if (button != null)
		{
			return;
		}
		BufferedImage icon = loadIcon();
		button = NavigationButton.builder()
			.tooltip("Tangle Crew LFG")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(button);
	}

	private void removeButton()
	{
		if (button == null)
		{
			return;
		}
		clientToolbar.removeNavigation(button);
		button = null;
	}

	private BufferedImage loadIcon()
	{
		try (InputStream iconStream = LfgNavigationManager.class.getResourceAsStream("/sidebar_icon.png"))
		{
			if (iconStream == null)
			{
				return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			}
			BufferedImage icon = ImageIO.read(iconStream);
			return icon == null ? new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB) : icon;
		}
		catch (IOException ex)
		{
			return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
	}
}
