package tccrewplugin.sync.clog;

import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tccrewplugin.sync.model.CollectionLogSnapshot;
import tccrewplugin.sync.model.CollectionLogState;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CollectionLogReaderTest
{
    @Test
    void readsCollectionLogWidgetsAndCounts()
    {
        Client client = Mockito.mock(Client.class);
        Widget collectionLog = Mockito.mock(Widget.class);
        Widget container = Mockito.mock(Widget.class);
        Widget tabs = Mockito.mock(Widget.class);
        Widget entry = Mockito.mock(Widget.class);
        Widget header = Mockito.mock(Widget.class);
        Widget itemsContainer = Mockito.mock(Widget.class);
        Widget itemWidget = Mockito.mock(Widget.class);
        Widget bossContainer = Mockito.mock(Widget.class);
        Widget raidContainer = Mockito.mock(Widget.class);
        ItemComposition itemComposition = Mockito.mock(ItemComposition.class);

        when(client.getWidget(WidgetInfo.COLLECTION_LOG)).thenReturn(collectionLog);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY)).thenReturn(entry);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_HEADER)).thenReturn(header);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_ITEMS)).thenReturn(itemsContainer);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_TABS)).thenReturn(tabs);
        when(client.getWidget(InterfaceID.Collection.BOSS_CONTAINER)).thenReturn(bossContainer);
        when(client.getWidget(InterfaceID.Collection.RAID_CONTAINER)).thenReturn(raidContainer);
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT)).thenReturn(742);
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX)).thenReturn(1692);
        when(client.getItemDefinition(12922)).thenReturn(itemComposition);
        when(itemComposition.getName()).thenReturn("Tanzanite fang");

        when(collectionLog.getId()).thenReturn(99);
        when(collectionLog.getChildren()).thenReturn(new Widget[] { container });
        when(collectionLog.getStaticChildren()).thenReturn(null);
        when(collectionLog.getDynamicChildren()).thenReturn(null);
        when(collectionLog.getNestedChildren()).thenReturn(null);

        when(container.getId()).thenReturn(100);
        when(container.getChildren()).thenReturn(new Widget[] { tabs, entry, header, itemsContainer });
        when(container.getStaticChildren()).thenReturn(null);
        when(container.getDynamicChildren()).thenReturn(null);
        when(container.getNestedChildren()).thenReturn(new Widget[] { tabs, entry, header, itemsContainer });

        when(tabs.getId()).thenReturn(101);
        when(tabs.getText()).thenReturn("Bosses");
        when(tabs.getName()).thenReturn("Bosses");
        when(tabs.getChildren()).thenReturn(new Widget[] { Mockito.mock(Widget.class) });
        when(tabs.getStaticChildren()).thenReturn(null);
        when(tabs.getDynamicChildren()).thenReturn(null);
        when(tabs.getNestedChildren()).thenReturn(null);

        when(entry.getId()).thenReturn(102);
        when(entry.getText()).thenReturn("Zulrah");
        when(entry.getName()).thenReturn("Zulrah");
        when(entry.getChildren()).thenReturn(null);
        when(entry.getStaticChildren()).thenReturn(null);
        when(entry.getDynamicChildren()).thenReturn(null);
        when(entry.getNestedChildren()).thenReturn(null);

        when(header.getId()).thenReturn(103);
        when(header.getText()).thenReturn("Zulrah");
        when(header.getName()).thenReturn("Zulrah");
        when(header.getChildren()).thenReturn(null);
        when(header.getStaticChildren()).thenReturn(null);
        when(header.getDynamicChildren()).thenReturn(null);
        when(header.getNestedChildren()).thenReturn(null);

        when(itemsContainer.getId()).thenReturn(104);
        when(itemsContainer.getChildren()).thenReturn(new Widget[] { itemWidget });
        when(itemsContainer.getStaticChildren()).thenReturn(null);
        when(itemsContainer.getDynamicChildren()).thenReturn(null);
        when(itemsContainer.getNestedChildren()).thenReturn(new Widget[] { itemWidget });

        when(itemWidget.getId()).thenReturn(201);
        when(itemWidget.getItemId()).thenReturn(12922);
        when(itemWidget.getItemQuantity()).thenReturn(3);
        when(itemWidget.getName()).thenReturn("");
        when(itemWidget.getText()).thenReturn("");
        when(itemWidget.getChildren()).thenReturn(null);
        when(itemWidget.getStaticChildren()).thenReturn(null);
        when(itemWidget.getDynamicChildren()).thenReturn(null);
        when(itemWidget.getNestedChildren()).thenReturn(null);

        when(bossContainer.getId()).thenReturn(105);
        when(bossContainer.getChildren()).thenReturn(new Widget[] { itemWidget });
        when(bossContainer.getStaticChildren()).thenReturn(null);
        when(bossContainer.getDynamicChildren()).thenReturn(null);
        when(bossContainer.getNestedChildren()).thenReturn(null);

        when(raidContainer.getId()).thenReturn(106);
        when(raidContainer.getChildren()).thenReturn(null);
        when(raidContainer.getStaticChildren()).thenReturn(null);
        when(raidContainer.getDynamicChildren()).thenReturn(null);
        when(raidContainer.getNestedChildren()).thenReturn(null);

        CollectionLogReader reader = new CollectionLogReader();
        CollectionLogSnapshot snapshot = reader.read(client, Instant.parse("2026-08-03T03:15:00Z"));

        assertSame(CollectionLogState.PARTIAL, snapshot.getState());
        assertEquals(742, snapshot.getObtainedSlots());
        assertEquals(1692, snapshot.getKnownTotalSlots());
        assertTrue(snapshot.getObservedSlots() >= 1);
        assertTrue(snapshot.getObservedCategoryCount() >= 1);
        assertTrue(snapshot.getExpectedCategoryCount() >= 1);
        assertTrue(snapshot.getItems().stream().anyMatch(item ->
            item.getItemId() == 12922
                && "Tanzanite fang".equals(item.getItemName())
                && item.getQuantity() == 3
                && "Bosses".equals(item.getCategory())
                && "Zulrah".equals(item.getSubcategory())));
        assertNotNull(snapshot.getCapturedAt());
    }

    @Test
    void returnsNotLoadedWhenCollectionLogIsClosed()
    {
        Client client = Mockito.mock(Client.class);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG)).thenReturn(null);

        CollectionLogReader reader = new CollectionLogReader();
        CollectionLogSnapshot snapshot = reader.read(client, Instant.parse("2026-08-03T03:15:00Z"));

        assertSame(CollectionLogState.NOT_LOADED, snapshot.getState());
        assertTrue(snapshot.getItems().isEmpty());
    }

    @Test
    void readsTextOnlyVisibleEntries()
    {
        Client client = Mockito.mock(Client.class);
        Widget collectionLog = Mockito.mock(Widget.class);
        Widget header = Mockito.mock(Widget.class);
        Widget tabs = Mockito.mock(Widget.class);
        Widget itemsContainer = Mockito.mock(Widget.class);
        Widget itemWidget = Mockito.mock(Widget.class);

        when(client.getWidget(WidgetInfo.COLLECTION_LOG)).thenReturn(collectionLog);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_HEADER)).thenReturn(header);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_ITEMS)).thenReturn(itemsContainer);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_TABS)).thenReturn(tabs);
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT)).thenReturn(12);
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX)).thenReturn(99);

        when(collectionLog.getChildren()).thenReturn(new Widget[] { itemsContainer });
        when(collectionLog.getStaticChildren()).thenReturn(null);
        when(collectionLog.getDynamicChildren()).thenReturn(null);
        when(collectionLog.getNestedChildren()).thenReturn(null);

        when(header.getText()).thenReturn("Kraken");
        when(header.getChildren()).thenReturn(null);
        when(header.getStaticChildren()).thenReturn(null);
        when(header.getDynamicChildren()).thenReturn(null);
        when(header.getNestedChildren()).thenReturn(null);

        when(tabs.getText()).thenReturn("Bosses");
        when(tabs.getChildren()).thenReturn(new Widget[] { Mockito.mock(Widget.class) });
        when(tabs.getStaticChildren()).thenReturn(null);
        when(tabs.getDynamicChildren()).thenReturn(null);
        when(tabs.getNestedChildren()).thenReturn(null);

        when(itemsContainer.getChildren()).thenReturn(new Widget[] { itemWidget });
        when(itemsContainer.getStaticChildren()).thenReturn(null);
        when(itemsContainer.getDynamicChildren()).thenReturn(null);
        when(itemsContainer.getNestedChildren()).thenReturn(null);

        when(itemWidget.getId()).thenReturn(301);
        when(itemWidget.getItemId()).thenReturn(0);
        when(itemWidget.getItemQuantity()).thenReturn(2);
        when(itemWidget.getText()).thenReturn("Kraken tentacle");
        when(itemWidget.getName()).thenReturn("Kraken tentacle");
        when(itemWidget.getChildren()).thenReturn(null);
        when(itemWidget.getStaticChildren()).thenReturn(null);
        when(itemWidget.getDynamicChildren()).thenReturn(null);
        when(itemWidget.getNestedChildren()).thenReturn(null);

        CollectionLogReader reader = new CollectionLogReader();
        CollectionLogSnapshot snapshot = reader.read(client, Instant.parse("2026-08-03T03:15:00Z"));

        assertSame(CollectionLogState.PARTIAL, snapshot.getState());
        assertTrue(snapshot.getItems().stream().anyMatch(item ->
            item.getItemId() == 0
                && "Kraken tentacle".equals(item.getItemName())
                && item.getQuantity() == 2
                && "Bosses".equals(item.getCategory())
                && "Kraken".equals(item.getSubcategory())));
    }

    @Test
    void capturesModelWidgetsFromCollectionLogTree()
    {
        Client client = Mockito.mock(Client.class);
        Widget collectionLog = Mockito.mock(Widget.class);
        Widget header = Mockito.mock(Widget.class);
        Widget tabs = Mockito.mock(Widget.class);
        Widget itemsContainer = Mockito.mock(Widget.class);
        Widget modelWidget = Mockito.mock(Widget.class);
        ItemComposition itemComposition = Mockito.mock(ItemComposition.class);

        when(client.getWidget(WidgetInfo.COLLECTION_LOG)).thenReturn(collectionLog);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_HEADER)).thenReturn(header);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_ITEMS)).thenReturn(itemsContainer);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_TABS)).thenReturn(tabs);
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT)).thenReturn(5);
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX)).thenReturn(10);
        when(client.getItemDefinition(12922)).thenReturn(itemComposition);
        when(itemComposition.getName()).thenReturn("Tanzanite fang");

        when(collectionLog.getChildren()).thenReturn(new Widget[] { itemsContainer });
        when(collectionLog.getStaticChildren()).thenReturn(null);
        when(collectionLog.getDynamicChildren()).thenReturn(null);
        when(collectionLog.getNestedChildren()).thenReturn(null);

        when(header.getText()).thenReturn("Zulrah");
        when(header.getChildren()).thenReturn(null);
        when(header.getStaticChildren()).thenReturn(null);
        when(header.getDynamicChildren()).thenReturn(null);
        when(header.getNestedChildren()).thenReturn(null);

        when(tabs.getText()).thenReturn("Bosses");
        when(tabs.getChildren()).thenReturn(new Widget[] { Mockito.mock(Widget.class) });
        when(tabs.getStaticChildren()).thenReturn(null);
        when(tabs.getDynamicChildren()).thenReturn(null);
        when(tabs.getNestedChildren()).thenReturn(null);

        when(itemsContainer.getChildren()).thenReturn(new Widget[] { modelWidget });
        when(itemsContainer.getStaticChildren()).thenReturn(null);
        when(itemsContainer.getDynamicChildren()).thenReturn(null);
        when(itemsContainer.getNestedChildren()).thenReturn(null);

        when(modelWidget.getType()).thenReturn(net.runelite.api.widgets.WidgetType.MODEL);
        when(modelWidget.getItemId()).thenReturn(0);
        when(modelWidget.getModelId()).thenReturn(12922);
        when(modelWidget.getItemQuantity()).thenReturn(1);
        when(modelWidget.getText()).thenReturn("");
        when(modelWidget.getName()).thenReturn("");
        when(modelWidget.getChildren()).thenReturn(null);
        when(modelWidget.getStaticChildren()).thenReturn(null);
        when(modelWidget.getDynamicChildren()).thenReturn(null);
        when(modelWidget.getNestedChildren()).thenReturn(null);

        CollectionLogReader reader = new CollectionLogReader();
        CollectionLogSnapshot snapshot = reader.read(client, Instant.parse("2026-08-03T03:15:00Z"));

        assertSame(CollectionLogState.PARTIAL, snapshot.getState());
        assertTrue(snapshot.getItems().stream().anyMatch(item ->
            item.getItemId() == 12922
                && "Tanzanite fang".equals(item.getItemName())));
    }
}
