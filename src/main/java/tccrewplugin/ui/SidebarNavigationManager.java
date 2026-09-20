package tccrewplugin.ui;

import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class SidebarNavigationManager
{
	private final ClientToolbar clientToolbar;
	private final MainPanel mainPanel;
	private NavigationButton navigationButton;

	public SidebarNavigationManager(ClientToolbar clientToolbar, MainPanel mainPanel)
	{
		this.clientToolbar = clientToolbar;
		this.mainPanel = mainPanel;
	}

	public void addNavigationButton()
	{
		if (navigationButton != null)
		{
			return;
		}
		BufferedImage icon = loadIcon();
		navigationButton = NavigationButton.builder()
			.tooltip(tccrewplugin.PluginConstants.PLUGIN_NAME)
			.icon(icon)
			.priority(5)
			.panel(mainPanel)
			.build();
		clientToolbar.addNavigation(navigationButton);
	}

	public void removeNavigationButton()
	{
		if (navigationButton == null)
		{
			return;
		}
		clientToolbar.removeNavigation(navigationButton);
		navigationButton = null;
	}

	private BufferedImage loadIcon()
	{
		try (InputStream iconStream = MainPanel.class.getResourceAsStream("/sidebar_icon.png"))
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
