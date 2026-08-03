package tccrewplugin.sync.clog;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import org.apache.commons.lang3.StringUtils;
import tccrewplugin.DinkPluginConfig;
import tccrewplugin.SettingsManager;
import tccrewplugin.sync.ClogPbSyncManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Singleton
public class CollectionLogSyncButtonManager
{
	private static final int COLLECTION_LOG_SETUP_SCRIPT = 7797;
	private static final int FONT_COLOUR_NORMAL = 0xD6D6D6;
	private static final int FONT_COLOUR_HOVER = 0xFFFFFF;
	private static final int CLOSE_BUTTON_OFFSET = 28;
	private static final int BUTTON_WIDTH = 71;
	private static final int BUTTON_OFFSET = CLOSE_BUTTON_OFFSET + 5;
	private static final int CORNER_SIZE = 9;
	private static final String BUTTON_LABEL = "Sync Clog";
	private static final String BUTTON_ACTION = "Sync your collection log";
	private static final String ROOT_NAME = "TcCrew Collection Log Sync Button";

	private static final int[] SPRITE_IDS_INACTIVE = {
		net.runelite.api.SpriteID.DIALOG_BACKGROUND,
		net.runelite.api.SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_TOP_LEFT,
		net.runelite.api.SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_TOP_RIGHT,
		net.runelite.api.SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_BOTTOM_LEFT,
		net.runelite.api.SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_BOTTOM_RIGHT,
		net.runelite.api.SpriteID.WORLD_MAP_BUTTON_EDGE_LEFT,
		net.runelite.api.SpriteID.WORLD_MAP_BUTTON_EDGE_TOP,
		net.runelite.api.SpriteID.WORLD_MAP_BUTTON_EDGE_RIGHT,
		net.runelite.api.SpriteID.WORLD_MAP_BUTTON_EDGE_BOTTOM
	};

	private static final int[] SPRITE_IDS_ACTIVE = {
		net.runelite.api.SpriteID.RESIZEABLE_MODE_SIDE_PANEL_BACKGROUND,
		net.runelite.api.SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_TOP_LEFT_HOVERED,
		net.runelite.api.SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_TOP_RIGHT_HOVERED,
		net.runelite.api.SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_BOTTOM_LEFT_HOVERED,
		net.runelite.api.SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_BOTTOM_RIGHT_HOVERED,
		net.runelite.api.SpriteID.EQUIPMENT_BUTTON_EDGE_LEFT_HOVERED,
		net.runelite.api.SpriteID.EQUIPMENT_BUTTON_EDGE_TOP_HOVERED,
		net.runelite.api.SpriteID.EQUIPMENT_BUTTON_EDGE_RIGHT_HOVERED,
		net.runelite.api.SpriteID.EQUIPMENT_BUTTON_EDGE_BOTTOM_HOVERED
	};

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final DinkPluginConfig config;
	private final ClogPbSyncManager syncManager;
	private final List<Widget> createdWidgets = new ArrayList<>();

	private Widget rootWidget;
	private Widget topBarWidget;
	private boolean started;
	private boolean buttonPresent;
	private int originalTopBarWidth = -1;

	@Inject
	public CollectionLogSyncButtonManager(
		Client client,
		ClientThread clientThread,
		EventBus eventBus,
		DinkPluginConfig config,
		ClogPbSyncManager syncManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.config = config;
		this.syncManager = syncManager;
	}

	public void startUp()
	{
		if (started)
		{
			return;
		}

		started = true;
		eventBus.register(this);
		clientThread.invokeLater(this::tryAddButton);
	}

	public void shutDown()
	{
		if (!started)
		{
			return;
		}

		started = false;
		eventBus.unregister(this);
		clientThread.invokeLater(this::removeButton);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SettingsManager.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("clogPbSyncEnabled".equals(event.getKey())
			|| "clogSyncEnabled".equals(event.getKey()))
		{
			clientThread.invokeLater(() ->
			{
				removeButton();
				tryAddButton();
			});
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::tryAddButton);
		}
		else if (state == GameState.LOGGING_IN
			|| state == GameState.LOGIN_SCREEN
			|| state == GameState.CONNECTION_LOST
			|| state == GameState.HOPPING
			|| state == GameState.UNKNOWN)
		{
			clientThread.invokeLater(this::removeButton);
		}
	}

	@Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() == COLLECTION_LOG_SETUP_SCRIPT)
        {
            clientThread.invokeLater(() ->
			{
				removeButton();
				tryAddButton();
            });
        }
    }

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == WidgetInfo.COLLECTION_LOG.getGroupId())
		{
			clientThread.invokeLater(this::tryAddButton);
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == WidgetInfo.COLLECTION_LOG.getGroupId())
		{
			clientThread.invokeLater(this::removeButton);
		}
	}

	public void onCollectionLogSyncRequested(CollectionLogSyncTrigger trigger)
	{
		clientThread.invokeLater(() ->
		{
			if (!isSyncEnabled())
			{
				return;
			}

			if (!syncManager.requestManualCollectionLogSync(trigger))
			{
				return;
			}
		});
	}

	private boolean isSyncEnabled()
	{
		return config.clogPbSyncEnabled() && config.clogSyncEnabled();
	}

    private void tryAddButton()
    {
        if (!isSyncEnabled() || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
		}

		Widget parent = client.getWidget(InterfaceID.Collection.UNIVERSE);
		if (parent == null)
		{
			if (log.isDebugEnabled())
			{
				log.debug("Collection log parent widget missing; button not shown");
			}
			return;
		}

		Widget searchButton = client.getWidget(InterfaceID.Collection.SEARCH_TOGGLE);
		Widget collectionLogContainer = client.getWidget(InterfaceID.Collection.INFINITY);
		if (searchButton == null || collectionLogContainer == null)
		{
			if (log.isDebugEnabled())
			{
				log.debug("Collection log control widgets missing; button not shown");
			}
			return;
		}

		Widget topBar = getTopBar(collectionLogContainer);
		if (topBar == null)
		{
			if (log.isDebugEnabled())
			{
				log.debug("Collection log draggable top bar widget missing; button not shown");
			}
			return;
		}

		Widget root = getOrCreateRoot(parent);
		if (root == null)
		{
			return;
		}

		if (buttonPresent && hasOwnedChildren(root))
		{
			return;
		}

		buildButton(root, topBar, searchButton);
		buttonPresent = true;
	}

	private Widget getOrCreateRoot(Widget parent)
	{
		if (rootWidget != null
			&& rootWidget.getParentId() == parent.getId()
			&& ROOT_NAME.equals(rootWidget.getName())
			&& rootWidget.getType() == WidgetType.LAYER)
		{
			rootWidget.setHidden(false);
			return rootWidget;
		}

		Widget[] children = parent.getChildren();
		if (children != null)
		{
			for (Widget child : children)
			{
				if (child != null && ROOT_NAME.equals(child.getName()))
				{
					if (child.getType() == WidgetType.LAYER)
					{
						rootWidget = child;
						rootWidget.setHidden(false);
						return rootWidget;
					}

					child.setHidden(true);
				}
			}
		}

		Widget created = parent.createChild(-1, WidgetType.LAYER)
			.setName(ROOT_NAME)
			.setHidden(false)
			.setPos(0, 0)
			.setSize(parent.getWidth(), parent.getHeight());
		created.revalidate();
		rootWidget = created;
		return created;
	}

	private Widget getTopBar(Widget parent)
	{
		Widget[] children = parent.getChildren();
		if (children == null || children.length == 0)
		{
			return null;
		}

		Widget candidate = children[0];
		if (candidate == null)
		{
			return null;
		}

		return candidate;
	}

	private boolean hasOwnedChildren(Widget root)
	{
		Widget[] children = root.getChildren();
		return children != null && children.length > 0;
	}

	private void buildButton(Widget root, Widget topBar, Widget searchButton)
	{
		removeOwnedChildren(root);

		int buttonHeight = searchButton.getOriginalHeight();
		int buttonY = searchButton.getOriginalY();
		int buttonX = BUTTON_OFFSET;
		int topBarShrink = BUTTON_WIDTH + (BUTTON_OFFSET - CLOSE_BUTTON_OFFSET);

		if (originalTopBarWidth < 0)
		{
			originalTopBarWidth = topBar.getOriginalWidth();
		}

		Widget[] sprites = new Widget[9];
		int yPositionMode = searchButton.getYPositionMode();
		sprites[0] = createSprite(root, SPRITE_IDS_INACTIVE[0], buttonX, buttonY, BUTTON_WIDTH, buttonHeight, yPositionMode);
		sprites[1] = createSprite(root, SPRITE_IDS_INACTIVE[1], buttonX + (BUTTON_WIDTH - CORNER_SIZE), buttonY, CORNER_SIZE, CORNER_SIZE, yPositionMode);
		sprites[2] = createSprite(root, SPRITE_IDS_INACTIVE[2], buttonX, buttonY, CORNER_SIZE, CORNER_SIZE, yPositionMode);
		sprites[3] = createSprite(root, SPRITE_IDS_INACTIVE[3], buttonX + (BUTTON_WIDTH - CORNER_SIZE), buttonY + buttonHeight - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, yPositionMode);
		sprites[4] = createSprite(root, SPRITE_IDS_INACTIVE[4], buttonX, buttonY + buttonHeight - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, yPositionMode);
		sprites[5] = createSprite(root, SPRITE_IDS_INACTIVE[5], buttonX + (BUTTON_WIDTH - CORNER_SIZE), buttonY + CORNER_SIZE, 9, 4, yPositionMode);
		sprites[6] = createSprite(root, SPRITE_IDS_INACTIVE[6], buttonX + CORNER_SIZE, buttonY, BUTTON_WIDTH - (CORNER_SIZE * 2), CORNER_SIZE, yPositionMode);
		sprites[7] = createSprite(root, SPRITE_IDS_INACTIVE[7], buttonX, buttonY + CORNER_SIZE, 9, 4, yPositionMode);
		sprites[8] = createSprite(root, SPRITE_IDS_INACTIVE[8], buttonX + CORNER_SIZE, buttonY + buttonHeight - 9, BUTTON_WIDTH - (CORNER_SIZE * 2), 9, yPositionMode);

		Widget text = root.createChild(-1, WidgetType.TEXT)
			.setText(BUTTON_LABEL)
			.setTextColor(FONT_COLOUR_NORMAL)
			.setFontId(net.runelite.api.FontID.PLAIN_11)
			.setTextShadowed(true)
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT)
			.setYPositionMode(topBar.getYPositionMode())
			.setXTextAlignment(WidgetTextAlignment.CENTER)
			.setYTextAlignment(WidgetTextAlignment.CENTER)
			.setHasListener(true)
			.setPos(buttonX, buttonY)
			.setSize(BUTTON_WIDTH, buttonHeight);
		text.setAction(0, BUTTON_ACTION);
		text.setOnOpListener((JavaScriptCallback) event -> onButtonClick());
		text.setOnMouseOverListener((JavaScriptCallback) event ->
		{
			swapSprites(sprites, SPRITE_IDS_ACTIVE);
			text.setTextColor(FONT_COLOUR_HOVER);
		});
		text.setOnMouseLeaveListener((JavaScriptCallback) event ->
		{
			swapSprites(sprites, SPRITE_IDS_INACTIVE);
			text.setTextColor(FONT_COLOUR_NORMAL);
		});
		text.revalidate();
		createdWidgets.add(text);
		for (Widget sprite : sprites)
		{
			createdWidgets.add(sprite);
		}

		if (topBar.getOriginalWidth() > topBarShrink)
		{
			topBar.setOriginalWidth(topBar.getOriginalWidth() - topBarShrink);
			topBar.revalidate();
		}
		root.revalidate();
		topBarWidget = topBar;
	}

	private Widget createSprite(Widget root, int spriteId, int x, int y, int width, int height, int yPositionMode)
	{
		Widget widget = root.createChild(-1, WidgetType.GRAPHIC)
			.setSpriteId(spriteId)
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT)
			.setYPositionMode(yPositionMode)
			.setPos(x, y)
			.setSize(width, height);
		widget.revalidate();
		return widget;
	}

	private void swapSprites(Widget[] widgets, int[] spriteIds)
	{
		for (int i = 0; i < widgets.length && i < spriteIds.length; i++)
		{
			Widget widget = widgets[i];
			if (widget != null)
			{
				widget.setSpriteId(spriteIds[i]);
			}
		}
		if (rootWidget != null)
		{
			rootWidget.revalidate();
		}
	}

    private void onButtonClick()
    {
        if (!isSyncEnabled())
        {
            return;
        }

        syncManager.requestManualCollectionLogSync(CollectionLogSyncTrigger.NATIVE_BUTTON);
    }

	private void removeButton()
	{
		buttonPresent = false;

		if (rootWidget != null)
		{
			removeOwnedChildren(rootWidget);
			rootWidget.setHidden(true);
			rootWidget.revalidate();
		}

		if (topBarWidget != null && originalTopBarWidth >= 0)
		{
			topBarWidget.setOriginalWidth(originalTopBarWidth);
			topBarWidget.revalidate();
		}

		rootWidget = null;
		topBarWidget = null;
		originalTopBarWidth = -1;
		createdWidgets.clear();
	}

	private void removeOwnedChildren(Widget root)
	{
		if (root != null)
		{
			root.deleteAllChildren();
			root.revalidate();
		}
		createdWidgets.clear();
	}

	private boolean isLoggedIn()
	{
		return client.getGameState() == GameState.LOGGED_IN;
	}
}
