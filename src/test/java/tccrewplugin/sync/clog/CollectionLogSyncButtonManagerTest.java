package tccrewplugin.sync.clog;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import tccrewplugin.DinkPluginConfig;
import tccrewplugin.sync.ClogPbSyncManager;
import tccrewplugin.sync.clog.CollectionLogReader;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectionLogSyncButtonManagerTest
{
	private Client client;
	private ClientThread clientThread;
	private EventBus eventBus;
	private DinkPluginConfig config;
	private ClogPbSyncManager syncManager;

	@BeforeEach
	void setUp()
	{
		client = mock(Client.class);
		clientThread = mock(ClientThread.class);
		eventBus = mock(EventBus.class);
		config = mock(DinkPluginConfig.class);
		syncManager = mock(ClogPbSyncManager.class);
		doAnswer(invocation ->
		{
			Runnable runnable = invocation.getArgument(0);
			runnable.run();
			return null;
		}).when(clientThread).invokeLater(org.mockito.ArgumentMatchers.any(Runnable.class));
		when(config.clogPbSyncEnabled()).thenReturn(true);
		when(config.clogSyncEnabled()).thenReturn(true);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
	}

	@Test
	void missingNativeWidgetsDoNothing()
	{
		when(client.getWidget(WidgetInfo.COLLECTION_LOG)).thenReturn(null);
		when(client.getWidget(InterfaceID.Collection.UNIVERSE)).thenReturn(null);

		CollectionLogSyncButtonManager manager = new CollectionLogSyncButtonManager(client, clientThread, eventBus, config, syncManager);
		manager.startUp();

		verify(eventBus).register(manager);
		verify(client, never()).runScript(anyInt());
		verify(syncManager, never()).requestManualCollectionLogSync(CollectionLogSyncTrigger.NATIVE_BUTTON);
	}

	@Test
	void buttonCreationAndRemovalRestoreTopBarWidth()
	{
		Widget parent = mock(Widget.class, Answers.RETURNS_SELF);
		Widget searchButton = mock(Widget.class, Answers.RETURNS_SELF);
		Widget collectionLogContainer = mock(Widget.class, Answers.RETURNS_SELF);
		Widget topBar = mock(Widget.class, Answers.RETURNS_SELF);
		Widget root = mock(Widget.class, Answers.RETURNS_SELF);
		Widget[] children = new Widget[] { topBar };

		when(parent.getChildren()).thenReturn(children);
		when(parent.getId()).thenReturn(100);
		when(parent.getWidth()).thenReturn(400);
		when(parent.getHeight()).thenReturn(300);
		when(parent.createChild(anyInt(), eq(net.runelite.api.widgets.WidgetType.LAYER))).thenReturn(root);
		when(searchButton.getOriginalHeight()).thenReturn(18);
		when(searchButton.getOriginalY()).thenReturn(3);
		when(searchButton.getYPositionMode()).thenReturn(WidgetPositionMode.ABSOLUTE_TOP);
		when(collectionLogContainer.getChildren()).thenReturn(new Widget[] { topBar });
		when(client.getWidget(InterfaceID.Collection.SEARCH_TOGGLE)).thenReturn(searchButton);
		when(client.getWidget(InterfaceID.Collection.INFINITY)).thenReturn(collectionLogContainer);

		when(topBar.getOriginalWidth()).thenReturn(280, 280);
		when(topBar.getOriginalHeight()).thenReturn(18);
		when(topBar.getOriginalY()).thenReturn(3);
		when(topBar.getYPositionMode()).thenReturn(WidgetPositionMode.ABSOLUTE_TOP);

		Widget[] spriteWidgets = new Widget[9];
		for (int i = 0; i < spriteWidgets.length; i++)
		{
			spriteWidgets[i] = mock(Widget.class, Answers.RETURNS_SELF);
		}
		Widget text = mock(Widget.class, Answers.RETURNS_SELF);
		AtomicReference<Integer> createIndex = new AtomicReference<>(0);
		when(root.createChild(anyInt(), eq(net.runelite.api.widgets.WidgetType.GRAPHIC))).thenAnswer(invocation ->
		{
			int index = createIndex.getAndUpdate(value -> value + 1);
			return spriteWidgets[index];
		});
		when(root.createChild(anyInt(), eq(net.runelite.api.widgets.WidgetType.TEXT))).thenReturn(text);

		when(client.getWidget(WidgetInfo.COLLECTION_LOG)).thenReturn(parent);
		when(client.getWidget(InterfaceID.Collection.UNIVERSE)).thenReturn(parent);

		CollectionLogSyncButtonManager manager = new CollectionLogSyncButtonManager(client, clientThread, eventBus, config, syncManager);
		manager.startUp();
		manager.shutDown();

		verify(eventBus).register(manager);
		verify(eventBus).unregister(manager);
		verify(topBar, atLeastOnce()).setOriginalWidth(280);
		verify(topBar).setOriginalWidth(280 - (71 + (33 - 28)));
		verify(root, atLeast(2)).deleteAllChildren();
		verify(parent, never()).deleteAllChildren();
	}

	@Test
	void configToggleHidesAndShowsTheButton()
	{
		Widget parent = mock(Widget.class, Answers.RETURNS_SELF);
		Widget searchButton = mock(Widget.class, Answers.RETURNS_SELF);
		Widget collectionLogContainer = mock(Widget.class, Answers.RETURNS_SELF);
		Widget topBar = mock(Widget.class, Answers.RETURNS_SELF);
		Widget root = mock(Widget.class, Answers.RETURNS_SELF);
		Widget[] children = new Widget[] { topBar };

		when(parent.getChildren()).thenReturn(children);
		when(parent.getId()).thenReturn(100);
		when(parent.getWidth()).thenReturn(400);
		when(parent.getHeight()).thenReturn(300);
		when(parent.createChild(anyInt(), eq(net.runelite.api.widgets.WidgetType.LAYER))).thenReturn(root);
		when(searchButton.getOriginalHeight()).thenReturn(18);
		when(searchButton.getOriginalY()).thenReturn(3);
		when(searchButton.getYPositionMode()).thenReturn(WidgetPositionMode.ABSOLUTE_TOP);
		when(collectionLogContainer.getChildren()).thenReturn(new Widget[] { topBar });
		when(client.getWidget(InterfaceID.Collection.SEARCH_TOGGLE)).thenReturn(searchButton);
		when(client.getWidget(InterfaceID.Collection.INFINITY)).thenReturn(collectionLogContainer);
		when(topBar.getOriginalWidth()).thenReturn(280, 280);
		when(topBar.getOriginalHeight()).thenReturn(18);
		when(topBar.getOriginalY()).thenReturn(3);
		when(topBar.getYPositionMode()).thenReturn(WidgetPositionMode.ABSOLUTE_TOP);
		when(client.getWidget(WidgetInfo.COLLECTION_LOG)).thenReturn(parent);
		when(client.getWidget(InterfaceID.Collection.UNIVERSE)).thenReturn(parent);

		CollectionLogSyncButtonManager manager = new CollectionLogSyncButtonManager(client, clientThread, eventBus, config, syncManager);
		manager.startUp();
		net.runelite.client.events.ConfigChanged event = new net.runelite.client.events.ConfigChanged();
		event.setGroup("dinkplugin");
		event.setKey("clogSyncEnabled");
		event.setOldValue("true");
		event.setNewValue("false");
		manager.onConfigChanged(event);

		verify(root, atLeastOnce()).deleteAllChildren();
	}
}
