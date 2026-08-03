package tccrewplugin.sync.clog;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.EnumComposition;
import net.runelite.api.Player;
import net.runelite.api.ScriptEvent;
import net.runelite.api.StructComposition;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tccrewplugin.DinkPluginConfig;
import tccrewplugin.TcCrewPlugin;
import tccrewplugin.sync.ClogPbSyncManager;
import tccrewplugin.sync.model.CollectionLogSnapshot;
import tccrewplugin.sync.model.CollectionLogState;
import tccrewplugin.sync.model.SyncPayload;
import tccrewplugin.sync.webhook.ClogPbWebhookClient;
import tccrewplugin.sync.webhook.UploadPriority;
import tccrewplugin.util.AccountTypeTracker;
import tccrewplugin.util.ItemSearcher;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClogPbSyncManagerManualSyncTest
{
    private Client client;
    private ClientThread clientThread;
    private ScheduledExecutorService executor;
    private DinkPluginConfig config;
    private ClogPbSyncManager syncManager;
    private ClogPbWebhookClient webhookClient;
    private CollectionLogReader collectionLogReader;
    private Widget collectionLogWidget;

    @BeforeEach
    void setUp()
    {
        client = mock(Client.class);
        clientThread = mock(ClientThread.class);
        executor = mock(ScheduledExecutorService.class);
        config = mock(DinkPluginConfig.class);
        webhookClient = mock(ClogPbWebhookClient.class);
        collectionLogReader = mock(CollectionLogReader.class);
        collectionLogWidget = mock(Widget.class);

        doAnswer(invocation ->
        {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(clientThread).invokeLater(org.mockito.ArgumentMatchers.<Runnable>any());

        when(config.clogPbSyncEnabled()).thenReturn(true);
        when(config.clogSyncEnabled()).thenReturn(true);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        Player player = mock(Player.class);
        when(client.getLocalPlayer()).thenReturn(player);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG)).thenReturn(collectionLogWidget);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY)).thenReturn(null);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_ITEMS)).thenReturn(null);
        when(client.getWidget(WidgetInfo.COLLECTION_LOG_TABS)).thenReturn(null);
        when(webhookClient.hasPendingWork()).thenReturn(false);
        when(executor.schedule(any(Runnable.class), Mockito.anyLong(), Mockito.eq(TimeUnit.MILLISECONDS)))
            .thenAnswer(invocation -> mock(ScheduledFuture.class));

        EnumComposition rootEnum = mock(EnumComposition.class);
        EnumComposition subtabEnumBosses = mock(EnumComposition.class);
        EnumComposition subtabEnumRaids = mock(EnumComposition.class);
        EnumComposition subtabEnumClues = mock(EnumComposition.class);
        EnumComposition subtabEnumMinigames = mock(EnumComposition.class);
        EnumComposition subtabEnumOther = mock(EnumComposition.class);
        EnumComposition itemEnumBosses = mock(EnumComposition.class);
        EnumComposition itemEnumRaids = mock(EnumComposition.class);
        EnumComposition itemEnumClues = mock(EnumComposition.class);
        EnumComposition itemEnumMinigames = mock(EnumComposition.class);
        EnumComposition itemEnumOther = mock(EnumComposition.class);
        StructComposition topLevelStructBosses = mock(StructComposition.class);
        StructComposition topLevelStructRaids = mock(StructComposition.class);
        StructComposition topLevelStructClues = mock(StructComposition.class);
        StructComposition topLevelStructMinigames = mock(StructComposition.class);
        StructComposition topLevelStructOther = mock(StructComposition.class);
        StructComposition subtabStructBosses = mock(StructComposition.class);
        StructComposition subtabStructRaids = mock(StructComposition.class);
        StructComposition subtabStructClues = mock(StructComposition.class);
        StructComposition subtabStructMinigames = mock(StructComposition.class);
        StructComposition subtabStructOther = mock(StructComposition.class);
        when(client.getEnum(2102)).thenReturn(rootEnum);
        when(client.getStructComposition(1001)).thenReturn(topLevelStructBosses);
        when(client.getStructComposition(1002)).thenReturn(topLevelStructRaids);
        when(client.getStructComposition(1003)).thenReturn(topLevelStructClues);
        when(client.getStructComposition(1004)).thenReturn(topLevelStructMinigames);
        when(client.getStructComposition(1005)).thenReturn(topLevelStructOther);
        when(client.getEnum(2001)).thenReturn(subtabEnumBosses);
        when(client.getEnum(2002)).thenReturn(subtabEnumRaids);
        when(client.getEnum(2003)).thenReturn(subtabEnumClues);
        when(client.getEnum(2004)).thenReturn(subtabEnumMinigames);
        when(client.getEnum(2005)).thenReturn(subtabEnumOther);
        when(client.getStructComposition(3001)).thenReturn(subtabStructBosses);
        when(client.getStructComposition(3002)).thenReturn(subtabStructRaids);
        when(client.getStructComposition(3003)).thenReturn(subtabStructClues);
        when(client.getStructComposition(3004)).thenReturn(subtabStructMinigames);
        when(client.getStructComposition(3005)).thenReturn(subtabStructOther);
        when(client.getEnum(4001)).thenReturn(itemEnumBosses);
        when(client.getEnum(4002)).thenReturn(itemEnumRaids);
        when(client.getEnum(4003)).thenReturn(itemEnumClues);
        when(client.getEnum(4004)).thenReturn(itemEnumMinigames);
        when(client.getEnum(4005)).thenReturn(itemEnumOther);
        when(client.getEnum(3721)).thenReturn(null);
        when(rootEnum.getIntVals()).thenReturn(new int[] { 1001, 1002, 1003, 1004, 1005 });
        when(rootEnum.getStringVals()).thenReturn(new String[] { "Bosses", "Raids", "Clues", "Minigames", "Other" });
        when(topLevelStructBosses.getIntValue(683)).thenReturn(2001);
        when(topLevelStructRaids.getIntValue(683)).thenReturn(2002);
        when(topLevelStructClues.getIntValue(683)).thenReturn(2003);
        when(topLevelStructMinigames.getIntValue(683)).thenReturn(2004);
        when(topLevelStructOther.getIntValue(683)).thenReturn(2005);
        when(subtabEnumBosses.getIntVals()).thenReturn(new int[] { 3001 });
        when(subtabEnumRaids.getIntVals()).thenReturn(new int[] { 3002 });
        when(subtabEnumClues.getIntVals()).thenReturn(new int[] { 3003 });
        when(subtabEnumMinigames.getIntVals()).thenReturn(new int[] { 3004 });
        when(subtabEnumOther.getIntVals()).thenReturn(new int[] { 3005 });
        when(subtabEnumBosses.getStringVals()).thenReturn(new String[] { "Abyssal Sire" });
        when(subtabEnumRaids.getStringVals()).thenReturn(new String[] { "Chambers of Xeric" });
        when(subtabEnumClues.getStringVals()).thenReturn(new String[] { "Beginner Treasure Trails" });
        when(subtabEnumMinigames.getStringVals()).thenReturn(new String[] { "Barbarian Assault" });
        when(subtabEnumOther.getStringVals()).thenReturn(new String[] { "Aerial Fishing" });
        when(subtabStructBosses.getIntValue(690)).thenReturn(4001);
        when(subtabStructRaids.getIntValue(690)).thenReturn(4002);
        when(subtabStructClues.getIntValue(690)).thenReturn(4003);
        when(subtabStructMinigames.getIntValue(690)).thenReturn(4004);
        when(subtabStructOther.getIntValue(690)).thenReturn(4005);
        when(itemEnumBosses.getIntVals()).thenReturn(new int[] { 13265, 4151 });
        when(itemEnumRaids.getIntVals()).thenReturn(new int[] { 21018 });
        when(itemEnumClues.getIntVals()).thenReturn(new int[] { 23309 });
        when(itemEnumMinigames.getIntVals()).thenReturn(new int[] { 10548 });
        when(itemEnumOther.getIntVals()).thenReturn(new int[] { 13261 });

        syncManager = new ClogPbSyncManager(
            client,
            clientThread,
            mock(ConfigManager.class),
            mock(ChatMessageManager.class),
            executor,
            new Gson(),
            config,
            mock(TcCrewPlugin.class),
            webhookClient,
            mock(AccountTypeTracker.class),
            mock(ItemSearcher.class),
            collectionLogReader
        );
    }

    @Test
    void duplicateManualSyncRequestsAreRejectedWhilePending()
    {
        assertTrue(syncManager.requestManualCollectionLogSync(CollectionLogSyncTrigger.NATIVE_BUTTON));
        assertFalse(syncManager.requestManualCollectionLogSync(CollectionLogSyncTrigger.NATIVE_BUTTON));
    }

    @Test
    void pendingWebhookWorkDoesNotBlockManualCaptureStart()
    {
        when(webhookClient.hasPendingWork()).thenReturn(true);
        assertTrue(syncManager.requestManualCollectionLogSync(CollectionLogSyncTrigger.NATIVE_BUTTON));
    }

    @Test
    void manualCaptureSubmitsDedupedItemIdsAfterQuietPeriod()
    {
        when(client.getTickCount()).thenReturn(100, 100, 100, 100, 101, 102);
        when(client.getVarpValue(net.runelite.api.gameval.VarPlayerID.COLLECTION_COUNT)).thenReturn(312);
        when(client.getVarpValue(net.runelite.api.gameval.VarPlayerID.COLLECTION_COUNT_MAX)).thenReturn(1712);

        when(collectionLogReader.read(any(), any())).thenReturn(
            new CollectionLogSnapshot(
                CollectionLogState.PARTIAL,
                Instant.parse("2026-08-03T00:00:00Z").toString(),
                312,
                1712,
                1712,
                5,
                5,
                Collections.emptyList()));

        ArgumentCaptor<SyncPayload> payloadCaptor = ArgumentCaptor.forClass(SyncPayload.class);
        when(webhookClient.submit(payloadCaptor.capture(), eq(UploadPriority.HIGH), any())).thenReturn(true);

        assertTrue(syncManager.requestManualCollectionLogSync(CollectionLogSyncTrigger.NATIVE_BUTTON));

        ScriptEvent scriptEvent = mock(ScriptEvent.class);
        ScriptPreFired preFired = mock(ScriptPreFired.class);
        when(preFired.getScriptId()).thenReturn(4100);
        when(preFired.getScriptEvent()).thenReturn(scriptEvent);

        when(scriptEvent.getArguments()).thenReturn(new Object[] { null, 13265 });
        syncManager.onScriptPreFired(preFired);

        when(scriptEvent.getArguments()).thenReturn(new Object[] { null, 13265 });
        syncManager.onScriptPreFired(preFired);

        when(scriptEvent.getArguments()).thenReturn(new Object[] { null, 4151 });
        syncManager.onScriptPreFired(preFired);

        syncManager.onGameTick(mock(GameTick.class));
        syncManager.onGameTick(mock(GameTick.class));

        verify(webhookClient).submit(any(), eq(UploadPriority.HIGH), any());
        SyncPayload payload = payloadCaptor.getValue();
        CollectionLogSnapshot snapshot = payload.getCollectionLog();
        assertEquals(CollectionLogState.COMPLETE, snapshot.getState());
        assertEquals(6, snapshot.getItems().size());
        assertEquals(2, snapshot.getObtainedSlots());
        assertEquals(5, snapshot.getObservedCategoryCount());
        assertEquals(5, snapshot.getExpectedCategoryCount());
        assertEquals("Abyssal Sire", snapshot.getItems().stream()
            .filter(item -> item.getItemId() == 13265)
            .findFirst()
            .orElseThrow()
            .getSubcategory());
        assertEquals("Bosses", snapshot.getItems().stream()
            .filter(item -> item.getItemId() == 4151)
            .findFirst()
            .orElseThrow()
            .getCategory());
    }

    @Test
    void manualCaptureUsesCacheCategoriesAcrossMultipleTopLevelSections()
    {
        when(client.getTickCount()).thenReturn(200, 201, 202, 203, 204, 205, 206, 207, 208, 209);
        when(client.getVarpValue(net.runelite.api.gameval.VarPlayerID.COLLECTION_COUNT)).thenReturn(5);
        when(client.getVarpValue(net.runelite.api.gameval.VarPlayerID.COLLECTION_COUNT_MAX)).thenReturn(5);
        when(webhookClient.submit(any(), eq(UploadPriority.HIGH), any())).thenReturn(true);

        assertTrue(syncManager.requestManualCollectionLogSync(CollectionLogSyncTrigger.NATIVE_BUTTON));

        ScriptEvent scriptEvent = mock(ScriptEvent.class);
        ScriptPreFired preFired = mock(ScriptPreFired.class);
        when(preFired.getScriptId()).thenReturn(4100);
        when(preFired.getScriptEvent()).thenReturn(scriptEvent);

        when(scriptEvent.getArguments()).thenReturn(new Object[] { null, 13265 });
        syncManager.onScriptPreFired(preFired);
        when(scriptEvent.getArguments()).thenReturn(new Object[] { null, 4151 });
        syncManager.onScriptPreFired(preFired);
        when(scriptEvent.getArguments()).thenReturn(new Object[] { null, 21018 });
        syncManager.onScriptPreFired(preFired);
        when(scriptEvent.getArguments()).thenReturn(new Object[] { null, 23309 });
        syncManager.onScriptPreFired(preFired);
        when(scriptEvent.getArguments()).thenReturn(new Object[] { null, 10548 });
        syncManager.onScriptPreFired(preFired);
        when(scriptEvent.getArguments()).thenReturn(new Object[] { null, 13261 });
        syncManager.onScriptPreFired(preFired);

        syncManager.onGameTick(mock(GameTick.class));
        syncManager.onGameTick(mock(GameTick.class));
        syncManager.onGameTick(mock(GameTick.class));

        ArgumentCaptor<SyncPayload> payloadCaptor = ArgumentCaptor.forClass(SyncPayload.class);
        verify(webhookClient).submit(payloadCaptor.capture(), eq(UploadPriority.HIGH), any());

        CollectionLogSnapshot snapshot = payloadCaptor.getValue().getCollectionLog();
        assertEquals(CollectionLogState.COMPLETE, snapshot.getState());
        assertEquals(5, snapshot.getObservedCategoryCount());
        assertEquals(5, snapshot.getExpectedCategoryCount());
        assertEquals(6, snapshot.getItems().size());
        assertEquals("Bosses", snapshot.getItems().get(0).getCategory());
        assertEquals("Bosses", snapshot.getItems().get(1).getCategory());
        assertEquals("Clues", snapshot.getItems().get(2).getCategory());
        assertEquals("Minigames", snapshot.getItems().get(3).getCategory());
        assertEquals("Other", snapshot.getItems().get(4).getCategory());
        assertEquals("Raids", snapshot.getItems().get(5).getCategory());
    }
}
