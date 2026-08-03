package tccrewplugin.sync.clog;

import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetType;
import org.apache.commons.lang3.StringUtils;
import tccrewplugin.sync.model.CollectionLogItem;
import tccrewplugin.sync.model.CollectionLogSnapshot;
import tccrewplugin.sync.model.CollectionLogState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CollectionLogReader
{
    private static final int HEADER_TITLE_INDEX = 0;
    private static final int[] CATEGORY_CONTAINER_IDS = {
        InterfaceID.Collection.BOSS_CONTAINER,
        InterfaceID.Collection.RAID_CONTAINER,
        InterfaceID.Collection.CLUE_CONTAINER,
        InterfaceID.Collection.MINIGAME_CONTAINER,
        InterfaceID.Collection.OTHER_CONTAINER
    };
    private static final String[] CATEGORY_NAMES = {
        "Bosses",
        "Raids",
        "Clues",
        "Minigames",
        "Other"
    };

    public CollectionLogSnapshot read(Client client, Instant capturedAt)
    {
        Widget collectionLog = client.getWidget(WidgetInfo.COLLECTION_LOG);
        Widget entry = getWidget(client, WidgetInfo.COLLECTION_LOG_ENTRY, InterfaceID.Collection.LIST);
        Widget header = getWidget(client, WidgetInfo.COLLECTION_LOG_ENTRY_HEADER, InterfaceID.Collection.HEADER_TEXT);
        Widget itemsContainer = getWidget(client, WidgetInfo.COLLECTION_LOG_ENTRY_ITEMS, InterfaceID.Collection.ITEMS_CONTENTS);
        Widget tabs = getWidget(client, WidgetInfo.COLLECTION_LOG_TABS, InterfaceID.Collection.TABS);

        if (collectionLog == null || header == null || itemsContainer == null)
        {
            return new CollectionLogSnapshot(
                CollectionLogState.NOT_LOADED,
                capturedAt.toString(),
                0,
                0,
                null,
                countChildren(tabs),
                countChildren(tabs),
                java.util.Collections.emptyList());
        }

        String categoryName = readCategoryTitle(tabs);
        String bossName = readHeaderTitle(header);
        List<CollectionLogItem> items = readVisibleItems(client, collectionLog, entry, itemsContainer, categoryName, bossName);
        items.addAll(readCategoryContainers(client, header, itemsContainer));

        int obtainedSlots = Math.max(0, client.getVarpValue(VarPlayerID.COLLECTION_COUNT));
        int knownTotalSlotsValue = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
        Integer knownTotalSlots = knownTotalSlotsValue > 0 ? knownTotalSlotsValue : null;

        CollectionLogState state = knownTotalSlots != null && obtainedSlots >= knownTotalSlots
            ? CollectionLogState.COMPLETE
            : CollectionLogState.PARTIAL;

        int categoryCount = countChildren(tabs);

        int observedSlots = knownTotalSlots != null
            ? knownTotalSlots
            : items.size();

        return new CollectionLogSnapshot(
            state,
            capturedAt.toString(),
            obtainedSlots,
            observedSlots,
            knownTotalSlots,
            categoryCount,
            categoryCount,
            items);
    }

    private List<CollectionLogItem> readVisibleItems(Client client, Widget collectionLog, Widget entry, Widget itemsContainer, String categoryName, String bossName)
    {
        Map<Integer, CollectionLogItem> items = new LinkedHashMap<>();
        collectVisibleItems(client, collectionLog, categoryName, bossName, false, items);
        collectVisibleItems(client, entry, categoryName, bossName, false, items);
        collectVisibleItems(client, itemsContainer, categoryName, bossName, true, items);
        List<CollectionLogItem> result = new ArrayList<>(items.values());
        result.sort(Comparator.comparing(item -> StringUtils.defaultString(item.getItemName()).toLowerCase(Locale.ROOT)));
        return result;
    }

    private List<CollectionLogItem> readCategoryContainers(Client client, Widget header, Widget itemsContainer)
    {
        Map<Integer, CollectionLogItem> items = new LinkedHashMap<>();
        String bossName = readHeaderTitle(header);

        for (int i = 0; i < CATEGORY_CONTAINER_IDS.length; i++)
        {
            Widget container = getWidget(client, CATEGORY_CONTAINER_IDS[i]);
            if (container == null)
            {
                continue;
            }

            collectVisibleItems(client, container, CATEGORY_NAMES[i], bossName, false, items);
            collectVisibleItems(client, container.getChildren(), CATEGORY_NAMES[i], bossName, false, items);
            collectVisibleItems(client, container.getStaticChildren(), CATEGORY_NAMES[i], bossName, false, items);
            collectVisibleItems(client, container.getDynamicChildren(), CATEGORY_NAMES[i], bossName, false, items);
            collectVisibleItems(client, container.getNestedChildren(), CATEGORY_NAMES[i], bossName, false, items);
        }

        if (items.isEmpty() && itemsContainer != null)
        {
            collectVisibleItems(client, itemsContainer, "Collection Log", bossName, true, items);
        }

        List<CollectionLogItem> result = new ArrayList<>(items.values());
        result.sort(Comparator
            .comparing((CollectionLogItem item) -> StringUtils.defaultString(item.getCategory()).toLowerCase(Locale.ROOT))
            .thenComparing(item -> StringUtils.defaultString(item.getSubcategory()).toLowerCase(Locale.ROOT))
            .thenComparing(item -> StringUtils.defaultString(item.getItemName()).toLowerCase(Locale.ROOT)));
        return result;
    }

    private void collectVisibleItems(Client client, Widget widget, String categoryName, String bossName, boolean allowTextFallback, Map<Integer, CollectionLogItem> items)
    {
        if (widget == null)
        {
            return;
        }

        captureWidget(client, widget, categoryName, bossName, allowTextFallback, items);

        collectVisibleItems(client, widget.getChildren(), categoryName, bossName, allowTextFallback, items);
        collectVisibleItems(client, widget.getStaticChildren(), categoryName, bossName, allowTextFallback, items);
        collectVisibleItems(client, widget.getDynamicChildren(), categoryName, bossName, allowTextFallback, items);
        collectVisibleItems(client, widget.getNestedChildren(), categoryName, bossName, allowTextFallback, items);
    }

    private void captureWidget(Client client, Widget widget, String categoryName, String bossName, boolean allowTextFallback, Map<Integer, CollectionLogItem> items)
    {
        if (widget == null)
        {
            return;
        }

        int itemId = widget.getItemId();
        if (itemId <= 0 && widget.getType() == WidgetType.MODEL)
        {
            int modelId = widget.getModelId();
            if (modelId > 0)
            {
                itemId = modelId;
            }
        }

        String itemName = null;
        if (itemId > 0)
        {
            ItemComposition definition;
            try
            {
                definition = client.getItemDefinition(itemId);
            }
            catch (RuntimeException ex)
            {
                definition = null;
            }

            itemName = definition == null ? null : normalize(definition.getName());
        }

        if (StringUtils.isBlank(itemName) && allowTextFallback)
        {
            itemName = normalize(firstNonBlank(widget.getText(), widget.getName()));
        }

        if (StringUtils.isBlank(itemName))
        {
            return;
        }

        int quantity = Math.max(1, parseQuantity(widget));
        boolean obtained = widget.getOpacity() == 0;
        int dedupeKey = java.util.Objects.hash(
            itemId,
            StringUtils.defaultString(itemName).toLowerCase(Locale.ROOT),
            quantity,
            StringUtils.defaultString(categoryName).toLowerCase(Locale.ROOT),
            StringUtils.defaultString(bossName).toLowerCase(Locale.ROOT));

        items.putIfAbsent(dedupeKey, new CollectionLogItem(
            itemId > 0 ? itemId : 0,
            itemName,
            quantity,
            obtained,
            categoryName,
            bossName));
    }

    private int parseQuantity(Widget widget)
    {
        int quantity = widget.getItemQuantity();
        if (quantity > 0)
        {
            return quantity;
        }

        String text = normalize(firstNonBlank(widget.getText(), widget.getName()));
        if (StringUtils.isBlank(text))
        {
            return 1;
        }

        String digitsOnly = text.replace(",", "").replaceAll("(?i)^x", "").trim();
        if (digitsOnly.matches("^\\d+$"))
        {
            try
            {
                return Math.max(1, Integer.parseInt(digitsOnly));
            }
            catch (NumberFormatException ex)
            {
                return 1;
            }
        }

        return 1;
    }

    private String firstNonBlank(String first, String second)
    {
        return StringUtils.isNotBlank(first) ? first : second;
    }

    private Widget getWidget(Client client, WidgetInfo widgetInfo, int componentId)
    {
        Widget widget = client.getWidget(widgetInfo);
        if (widget != null)
        {
            return widget;
        }

        return client.getWidget(componentId);
    }

    private Widget getWidget(Client client, int componentId)
    {
        return client.getWidget(componentId);
    }

    private String readCategoryTitle(Widget tabs)
    {
        if (tabs == null)
        {
            return "Collection Log";
        }

        String text = readWidgetText(tabs);
        return StringUtils.isBlank(text) ? "Collection Log" : text;
    }

    private String readWidgetText(Widget widget)
    {
        if (widget == null)
        {
            return "";
        }

        Widget child = widget.getChild(HEADER_TITLE_INDEX);
        String text = child == null ? null : child.getText();
        if (StringUtils.isBlank(text))
        {
            text = widget.getText();
        }
        if (StringUtils.isBlank(text))
        {
            text = widget.getName();
        }
        return normalize(text);
    }

    private String readHeaderTitle(Widget header)
    {
        String text = readWidgetText(header);
        return StringUtils.isBlank(text) ? "Collection Log" : text;
    }

    private void collectVisibleItems(Client client, Widget[] widgets, String categoryName, String bossName, boolean allowTextFallback, Map<Integer, CollectionLogItem> items)
    {
        if (widgets == null)
        {
            return;
        }

        for (Widget child : widgets)
        {
            collectVisibleItems(client, child, categoryName, bossName, allowTextFallback, items);
        }
    }

    private int countChildren(Widget widget)
    {
        if (widget == null)
        {
            return 0;
        }

        Widget[] children = widget.getChildren();
        if (children != null)
        {
            return children.length;
        }

        children = widget.getStaticChildren();
        if (children != null)
        {
            return children.length;
        }

        children = widget.getDynamicChildren();
        if (children != null)
        {
            return children.length;
        }

        children = widget.getNestedChildren();
        return children == null ? 0 : children.length;
    }

    private String normalize(String value)
    {
        return StringUtils.normalizeSpace(value == null ? "" : value.replace((char) 160, ' '));
    }
}
