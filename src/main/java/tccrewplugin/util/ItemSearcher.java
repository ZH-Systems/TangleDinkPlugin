package tccrewplugin.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * This thread-safe singleton holds a mapping of item names to their item id, using the RuneLite API.
 * <p>
 * Unlike {@link net.runelite.client.game.ItemManager#search(String)}, this mapping supports untradable items.
 */
@Slf4j
@Singleton
public class ItemSearcher {
    private final Map<String, Integer> itemIdByName = Collections.synchronizedMap(new HashMap<>(16384));
    private @Inject OkHttpClient httpClient;
    private @Inject Gson gson;

    /**
     * @param name the exact in-game name of an item
     * @return the id associated with the item name, or null if not found
     */
    @Nullable
    public Integer findItemId(@NotNull String name) {
        return itemIdByName.get(name);
    }

    /**
     * Begins the initialization process for {@link #itemIdByName}
     * by querying item names and noted item ids from the RuneLite API,
     * before passing them to {@link #populate(Map, Set)}
     *
     * @implNote This operation does not block the current thread,
     * by utilizing OkHttp's thread pool and Java's Fork-Join common pool.
     */
    @Inject
    void init() {
        queryNamesById()
            .thenAcceptBothAsync(
                queryNotedItemIds().exceptionally(e -> {
                    log.error("Failed to read noted items", e);
                    return Collections.emptySet();
                }),
                this::populate
            )
            .exceptionally(e -> {
                log.error("Failed to read item names", e);
                return null;
            });
    }

    /**
     * Populates {@link #itemIdByName} with the inverted mappings of {@code namesById},
     * while skipping noted items specified in {@code notedIds}.
     *
     * @param namesById a mapping of item id's to the corresponding in-game name
     * @param notedIds  the id's of noted items
     * @implNote When multiple non-noted item id's have the same in-game name, only the earliest id is saved
     */
    @VisibleForTesting
    void populate(@NotNull Map<Integer, String> namesById, @NotNull Set<Integer> notedIds) {
        namesById.forEach((id, name) -> {
            if (!notedIds.contains(id))
                itemIdByName.putIfAbsent(name, id);
        });

        log.debug("Completed initialization of item cache with {} entries", itemIdByName.size());
    }

    /**
     * @return a mapping of item ids to their in-game names, provided by the RuneLite API
     */
    private CompletableFuture<Map<Integer, String>> queryNamesById() {
        return Utils.readUrl(httpClient, ItemUtils.ITEM_CACHE_BASE_URL + "names.json", reader -> {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            Map<Integer, String> namesById = new HashMap<>();
            if (json != null) {
                json.entrySet().forEach(entry -> namesById.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString()));
            }
            return namesById;
        });
    }

    /**
     * @return a set of id's of noted items, provided by the RuneLite API
     */
    private CompletableFuture<Set<Integer>> queryNotedItemIds() {
        return Utils.readUrl(httpClient, ItemUtils.ITEM_CACHE_BASE_URL + "notes.json", reader -> {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            Set<Integer> notedIds = new java.util.HashSet<>();
            if (json != null) {
                json.keySet().forEach(key -> notedIds.add(Integer.parseInt(key)));
            }
            return notedIds;
        });
    }
}

