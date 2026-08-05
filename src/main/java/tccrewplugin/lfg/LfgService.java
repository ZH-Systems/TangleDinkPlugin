package tccrewplugin.lfg;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import org.apache.commons.lang3.StringUtils;
import tccrewplugin.DinkPluginConfig;
import tccrewplugin.PluginConstants;
import tccrewplugin.SettingsManager;
import tccrewplugin.api.ApiResult;
import tccrewplugin.lfg.model.CreateLfgGroupRequest;
import tccrewplugin.lfg.model.LfgActionRequest;
import tccrewplugin.lfg.model.LfgActionResponse;
import tccrewplugin.lfg.model.LfgCategory;
import tccrewplugin.lfg.model.LfgConfigurationResponse;
import tccrewplugin.lfg.model.LfgGroup;
import tccrewplugin.lfg.model.LfgGroupsResponse;
import tccrewplugin.lfg.model.LfgMember;
import tccrewplugin.lfg.model.LfgSource;
import tccrewplugin.sync.model.PlayerIdentity;
import tccrewplugin.util.Utils;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class LfgService
{
	private final Client client;
	private final ClientThread clientThread;
	private final DinkPluginConfig config;
	private final ConfigManager configManager;
	private final Gson gson;
	private final ChatMessageManager chatMessageManager;
	private final LfgApiClient apiClient;
	private final LfgPlayerIdentityProvider identityProvider;
	private final LfgRequestValidator validator = new LfgRequestValidator();
	private final LfgRefreshManager refreshManager;
	private final AtomicBoolean shutdown = new AtomicBoolean();
	private final AtomicBoolean enabled = new AtomicBoolean();
	private final AtomicBoolean loggedIn = new AtomicBoolean();
	private final AtomicBoolean refreshInFlight = new AtomicBoolean();
	private final AtomicBoolean mutationInFlight = new AtomicBoolean();
	private final AtomicInteger refreshGeneration = new AtomicInteger();
	private final Set<String> announcedGroupKeys = ConcurrentHashMap.newKeySet();
	private volatile LfgPanel panel;
	private volatile List<LfgCategory> allCategories = List.of();
	private volatile List<LfgGroup> allGroups = List.of();
	private volatile PlayerIdentity currentIdentity = new PlayerIdentity("", "");
	private volatile String statusMessage = "Waiting to load groups";
	private volatile String errorMessage = "";

	@Inject
	public LfgService(
		Client client,
		ClientThread clientThread,
		DinkPluginConfig config,
		ConfigManager configManager,
		Gson gson,
		ChatMessageManager chatMessageManager,
		ScheduledExecutorService executor,
		okhttp3.OkHttpClient httpClient
	)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.configManager = configManager;
		this.gson = gson;
		this.chatMessageManager = chatMessageManager;
		this.apiClient = new LfgApiClient(httpClient, gson);
		this.identityProvider = new LfgPlayerIdentityProvider(client);
		this.refreshManager = new LfgRefreshManager(executor);
	}

	public void attachPanel(LfgPanel panel)
	{
		this.panel = panel;
		publishState();
	}

	public void startUp()
	{
		shutdown.set(false);
		enabled.set(config.lfgEnabled());
		clearAnnouncements();
		refreshManager.start(this::refreshNow, config.lfgRefreshIntervalSeconds());
		refreshManager.setVisible(false);
		if (enabled.get() && client.getGameState() == GameState.LOGGED_IN)
		{
			refreshNow();
		}
	}

	public void shutDown()
	{
		shutdown.set(true);
		refreshManager.shutdown();
		refreshInFlight.set(false);
		mutationInFlight.set(false);
		clearAnnouncements();
		allCategories = List.of();
		allGroups = List.of();
		currentIdentity = new PlayerIdentity("", "");
		statusMessage = "Disabled";
		errorMessage = "";
		publishState();
	}

	public void setPanelVisible(boolean visible)
	{
		refreshManager.setVisible(visible && enabled.get() && loggedIn.get());
		if (visible && enabled.get() && loggedIn.get())
		{
			refreshNow();
		}
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			loggedIn.set(true);
			refreshManager.setVisible(panel != null && panel.isShowing());
			if (enabled.get())
			{
				refreshNow();
			}
		}
		else
		{
			loggedIn.set(false);
			currentIdentity = new PlayerIdentity("", "");
			clearAnnouncements();
			refreshManager.setVisible(false);
			statusMessage = "Waiting for login";
			errorMessage = "";
			publishState();
		}
	}

	public void onConfigChanged(net.runelite.client.events.ConfigChanged event)
	{
		if (!SettingsManager.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		String key = event.getKey();
		if ("lfgEnabled".equals(key))
		{
			boolean newEnabled = Boolean.parseBoolean(event.getNewValue());
			enabled.set(newEnabled);
			if (!newEnabled)
			{
				refreshManager.setVisible(false);
				clearAnnouncements();
				allCategories = List.of();
				allGroups = List.of();
				statusMessage = "Disabled";
				errorMessage = "";
				publishState();
				return;
			}
			refreshManager.setVisible(panel != null && panel.isShowing());
			if (loggedIn.get())
			{
				refreshNow();
			}
			return;
		}

		if ("lfgRefreshIntervalSeconds".equals(key))
		{
			refreshManager.updateInterval(config.lfgRefreshIntervalSeconds());
			return;
		}

		if ("lfgVisibleCategories".equals(key) || "lfgShowFullGroups".equals(key) || "lfgShowDiscordGroups".equals(key) || "lfgShowRuneLiteGroups".equals(key))
		{
			publishState();
			return;
		}

		if ("lfgSupabaseUrl".equals(key) || "lfgApiToken".equals(key) || "lfgMasterChannelWebhook".equals(key))
		{
			if (loggedIn.get() && enabled.get())
			{
				refreshNow();
			}
		}
	}

	public void onCommandExecuted(CommandExecuted event)
	{
		String command = event.getCommand() == null ? "" : event.getCommand().toLowerCase(Locale.ROOT);
		if ("lfgrefresh".equals(command) || "lfg".equals(command))
		{
			refreshNow();
			return;
		}
		if ("lfgleave".equals(command))
		{
			performCurrentPlayerAction("leave");
			return;
		}
		if ("lfgclose".equals(command))
		{
			performClosableAction();
		}
	}

	public void refreshNow()
	{
		if (shutdown.get() || !enabled.get() || refreshInFlight.get() || mutationInFlight.get())
		{
			return;
		}
		clientThread.invokeLater(this::refreshFromClientThread);
	}

	public void updateVisibleCategories(String visibleCategories)
	{
		if (shutdown.get())
		{
			return;
		}
		configManager.setConfiguration(SettingsManager.CONFIG_GROUP, "lfgVisibleCategories", visibleCategories == null ? "" : visibleCategories.trim());
	}

	public void createGroup(String categoryKey, String activity, String description, boolean scheduleNow, String startTimeText, Integer maximumPlayers)
	{
		createGroup(categoryKey, activity, description, parseInstant(startTimeText), maximumPlayers);
	}

	public void createGroup(String categoryKey, String activity, String description, Instant startTime, Integer maximumPlayers)
	{
		if (!beginMutation())
		{
			return;
		}
		PlayerIdentity identity = currentIdentity;
		List<LfgCategory> availableCategories = filterCategories();
		LfgRequestValidator.ValidationResult validation;
		try
		{
			validation = validator.validateCreateRequest(categoryKey, activity, description, maximumPlayers, startTime, availableCategories);
		}
		catch (IllegalArgumentException ex)
		{
			finishMutation();
			chatWarning(ex.getMessage());
			setError(ex.getMessage());
			return;
		}
		if (!validation.isValid())
		{
			finishMutation();
			chatWarning(validation.getMessage());
			setError(validation.getMessage());
			return;
		}

		CreateLfgGroupRequest request = new CreateLfgGroupRequest(
			validation.getCategoryKey(),
			validation.getActivity(),
			validation.getDescription(),
			validation.getStartTime() == null ? null : validation.getStartTime().toString(),
			validation.getMaximumPlayers()
		);
		String playerHeader = identityProvider.toHeaderValue(identity);
		String idempotencyKey = UUID.randomUUID().toString();
		setLoading(true);
		apiClient.createGroup(config.lfgSupabaseUrl(), config.lfgApiToken(), playerHeader, PluginConstants.VERSION, idempotencyKey, request)
			.whenComplete((result, throwable) -> handleActionResult("create", "", result, throwable, true));
	}

	public void joinGroup(String groupId)
	{
		requestAction("join", groupId);
	}

	public void leaveGroup(String groupId)
	{
		requestAction("leave", groupId);
	}

	public void closeGroup(String groupId)
	{
		requestAction("close", groupId);
	}

	private void requestAction(String action, String groupId)
	{
		if (!beginMutation())
		{
			return;
		}
		LfgRequestValidator.ValidationResult validation = validator.validateActionRequest(groupId);
		if (!validation.isValid())
		{
			finishMutation();
			chatWarning(validation.getMessage());
			setError(validation.getMessage());
			return;
		}

		LfgActionRequest request = new LfgActionRequest(action, validation.getCategoryKey(), UUID.randomUUID().toString());
		setLoading(true);
		apiClient.actOnGroup(config.lfgSupabaseUrl(), config.lfgApiToken(), identityProvider.toHeaderValue(currentIdentity), PluginConstants.VERSION, request)
			.whenComplete((result, throwable) -> handleActionResult(action, validation.getCategoryKey(), result, throwable, false));
	}

	private void performCurrentPlayerAction(String action)
	{
		String currentPlayer = currentIdentity == null ? "" : currentIdentity.getUsername();
		List<LfgGroup> matches = allGroups.stream()
			.filter(group -> isCurrentPlayerMember(group, currentPlayer) && group.getPermissions() != null && group.getPermissions().isCanLeave())
			.collect(Collectors.toList());
		if (matches.isEmpty())
		{
			chatWarning("No matching group was found for the current player.");
			return;
		}
		if (matches.size() > 1)
		{
			chatWarning("Multiple matching groups were found; please use the sidebar.");
			return;
		}
		requestAction(action, matches.get(0).getId());
	}

	private void performClosableAction()
	{
		List<LfgGroup> matches = allGroups.stream()
			.filter(group -> group != null && group.getPermissions() != null && group.getPermissions().isCanClose())
			.collect(Collectors.toList());
		if (matches.isEmpty())
		{
			chatWarning("No closable group was found.");
			return;
		}
		if (matches.size() > 1)
		{
			chatWarning("Multiple closable groups were found; please use the sidebar.");
			return;
		}
		requestAction("close", matches.get(0).getId());
	}

	private boolean isCurrentPlayerMember(LfgGroup group, String currentPlayer)
	{
		if (group == null || StringUtils.isBlank(currentPlayer) || group.getMembers() == null)
		{
			return false;
		}
		String normalized = currentPlayer.trim().toLowerCase(Locale.ROOT);
		for (LfgMember member : group.getMembers())
		{
			if (member != null && StringUtils.isNotBlank(member.getRsn()) && normalized.equals(member.getRsn().trim().toLowerCase(Locale.ROOT)))
			{
				return true;
			}
		}
		return false;
	}

	private void refreshFromClientThread()
	{
		if (shutdown.get() || !enabled.get() || !loggedIn.get() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (!refreshInFlight.compareAndSet(false, true))
		{
			return;
		}

		int generation = refreshGeneration.incrementAndGet();
		PlayerIdentity identity = identityProvider.resolve();
		currentIdentity = identity;
		String playerHeader = identityProvider.toHeaderValue(identity);
		setLoading(true);
		CompletableFuture<ApiResult<LfgConfigurationResponse>> configFuture = apiClient.fetchConfiguration(config.lfgSupabaseUrl(), config.lfgApiToken(), playerHeader, PluginConstants.VERSION);
		CompletableFuture<ApiResult<LfgGroupsResponse>> groupsFuture = apiClient.fetchGroups(config.lfgSupabaseUrl(), config.lfgApiToken(), playerHeader, PluginConstants.VERSION);

		configFuture.whenComplete((result, throwable) -> handleConfigurationResult(generation, result, throwable));
		groupsFuture.whenComplete((result, throwable) -> handleGroupsResult(generation, result, throwable));
		CompletableFuture.allOf(configFuture, groupsFuture).whenComplete((ignored, throwable) -> {
			if (generation != refreshGeneration.get())
			{
				return;
			}
			refreshInFlight.set(false);
			setLoading(false);
			publishState();
		});
	}

	private void handleConfigurationResult(int generation, ApiResult<LfgConfigurationResponse> result, Throwable throwable)
	{
		if (generation != refreshGeneration.get() || shutdown.get())
		{
			return;
		}

		if (throwable != null)
		{
			setError(LfgErrorSanitizer.sanitizeThrowable(throwable, secrets()));
			chatWarning(errorMessage);
			return;
		}

		if (result != null && result.getStatusCode() == 401)
		{
			String message = linkAccountHint();
			setError(message);
			chatWarning(message);
			return;
		}

		if (result == null || !result.isSuccess())
		{
			String message = sanitizeApiFailure("configuration", result);
			setError(message);
			chatWarning(message);
			return;
		}

		LfgConfigurationResponse response = result.getBody();
		if (response == null)
		{
			allCategories = List.of();
			setError("LFG configuration response was empty.");
			return;
		}

		List<LfgCategory> categories = response.getCategories() == null ? List.of() : new ArrayList<>(response.getCategories());
		categories.sort(Comparator.comparingInt(LfgCategory::getDisplayOrder).thenComparing(category -> StringUtils.defaultString(category.getKey())));
		allCategories = categories;
		List<LfgCategory> visibleCategories = LfgCategoryFilter.filterCategories(categories, config.lfgVisibleCategories());
		log.debug(
			"LFG categories loaded: total={}, visible={}, allowlist='{}'",
			categories.size(),
			visibleCategories.size(),
			StringUtils.defaultIfBlank(config.lfgVisibleCategories(), "<blank>")
		);
		if (categories.isEmpty())
		{
			setError("No LFG categories are available.");
		}
		else
		{
			setError("");
			statusMessage = StringUtils.defaultIfBlank(response.getMessage(), "Loaded categories");
		}
		maybeAnnounceVisibleGroups();
		publishState();
	}

	private void handleGroupsResult(int generation, ApiResult<LfgGroupsResponse> result, Throwable throwable)
	{
		if (generation != refreshGeneration.get() || shutdown.get())
		{
			return;
		}

		if (throwable != null)
		{
			setError(LfgErrorSanitizer.sanitizeThrowable(throwable, secrets()));
			chatWarning(errorMessage);
			return;
		}

		if (result != null && result.getStatusCode() == 401)
		{
			String message = linkAccountHint();
			setError(message);
			chatWarning(message);
			return;
		}

		if (result == null || !result.isSuccess())
		{
			String message = sanitizeApiFailure("groups", result);
			setError(message);
			chatWarning(message);
			return;
		}

		LfgGroupsResponse response = result.getBody();
		if (response == null)
		{
			allGroups = List.of();
			setError("LFG groups response was empty.");
			return;
		}

		allGroups = response.getGroups() == null ? List.of() : new ArrayList<>(response.getGroups());
		if (StringUtils.isNotBlank(response.getMessage()))
		{
			statusMessage = response.getMessage();
		}
		setError("");
		maybeAnnounceVisibleGroups();
		publishState();
	}

	private void handleActionResult(String action, String groupId, ApiResult<LfgActionResponse> result, Throwable throwable, boolean refreshAfter)
	{
		boolean shouldRefresh = false;
		try
		{
			if (throwable != null)
			{
				String message = LfgErrorSanitizer.sanitizeThrowable(throwable, secrets());
				setError(message);
				chatWarning(message);
				return;
			}
			if (result != null && result.getStatusCode() == 401)
			{
				String message = linkAccountHint();
				setError(message);
				chatWarning(message);
				return;
			}

			LfgActionResponse response = result == null ? null : result.getBody();
			if (response != null && !response.isSuccess())
			{
				String message = StringUtils.defaultIfBlank(response.getMessage(), "Action failed.");
				String errorMessage = StringUtils.defaultIfBlank(response.getErrorMessage(), "");
				if (StringUtils.isNotBlank(errorMessage))
				{
					message = message + ": " + errorMessage;
				}
				setError(message);
				chatWarning(message);
				return;
			}
			if (result == null || !result.isSuccess())
			{
				String message = sanitizeApiFailure(action, result);
				setError(message);
				chatWarning(message);
				return;
			}
			if (response != null && StringUtils.isNotBlank(response.getMessage()))
			{
				statusMessage = response.getMessage();
			}
			if (StringUtils.equalsAnyIgnoreCase(action, "leave", "close") && StringUtils.isNotBlank(groupId))
			{
				allGroups = allGroups.stream()
					.filter(group -> group != null && !groupId.equals(group.getId()))
					.collect(Collectors.toList());
			}
			setError("");
			chatSuccess(StringUtils.defaultIfBlank(action, "Action") + " completed.");
			shouldRefresh = true;
		}
		finally
		{
			finishMutation();
			if (shouldRefresh)
			{
				refreshNow();
			}
			else
			{
				publishState();
			}
		}
	}

	private boolean beginMutation()
	{
		if (shutdown.get() || !enabled.get() || mutationInFlight.get())
		{
			return false;
		}
		return mutationInFlight.compareAndSet(false, true);
	}

	private void finishMutation()
	{
		mutationInFlight.set(false);
		setLoading(false);
	}

	private void setLoading(boolean loading)
	{
		LfgPanel currentPanel = panel;
		if (currentPanel != null)
		{
			currentPanel.setBusy(loading);
		}
	}

	private void setError(String message)
	{
		errorMessage = message == null ? "" : message;
		publishState();
	}

	private void publishState()
	{
		LfgPanel currentPanel = panel;
		if (currentPanel == null)
		{
			return;
		}

		List<LfgCategory> categories = filterCategories();
		List<LfgGroup> groups = filterGroups(categories);
		PlayerIdentity identity = currentIdentity;
		String allowList = config.lfgVisibleCategories();
		String error = errorMessage;
		String status = statusMessage;
		boolean busy = refreshInFlight.get() || mutationInFlight.get();
		SwingUtilities.invokeLater(() -> currentPanel.updateState(categories, groups, identity, status, error, allowList, config.lfgShowFullGroups(), config.lfgShowDiscordGroups(), config.lfgShowRuneLiteGroups(), busy));
	}

	private List<LfgCategory> filterCategories()
	{
		return LfgCategoryFilter.filterCategories(allCategories, config.lfgVisibleCategories());
	}

	private List<LfgGroup> filterGroups(List<LfgCategory> categories)
	{
		if (allGroups.isEmpty())
		{
			return List.of();
		}

		Set<String> categoryKeys = categories.stream()
			.map(category -> category.getKey() == null ? "" : category.getKey().trim().toLowerCase(Locale.ROOT))
			.collect(Collectors.toCollection(HashSet::new));
		List<LfgGroup> filtered = new ArrayList<>();
		for (LfgGroup group : allGroups)
		{
			if (group == null || group.getCategory() == null || StringUtils.isBlank(group.getCategory().getKey()))
			{
				continue;
			}
			String key = group.getCategory().getKey().trim().toLowerCase(Locale.ROOT);
			if (!categoryKeys.contains(key))
			{
				continue;
			}
			if (!config.lfgShowDiscordGroups() && group.getSource() == LfgSource.DISCORD)
			{
				continue;
			}
			if (!config.lfgShowRuneLiteGroups() && group.getSource() == LfgSource.RUNELITE)
			{
				continue;
			}
			if (!config.lfgShowFullGroups() && group.getStatus() == tccrewplugin.lfg.model.LfgGroupStatus.FULL)
			{
				continue;
			}
			if (group.getStatus() == tccrewplugin.lfg.model.LfgGroupStatus.CLOSED
				|| group.getStatus() == tccrewplugin.lfg.model.LfgGroupStatus.CANCELLED
				|| group.getStatus() == tccrewplugin.lfg.model.LfgGroupStatus.EXPIRED)
			{
				continue;
			}
			filtered.add(group);
		}
		return filtered;
	}

	private String sanitizeApiFailure(String operation, ApiResult<?> result)
	{
		String error = result == null ? "unknown error" : result.getError();
		String message = StringUtils.defaultIfBlank(error, "HTTP " + (result == null ? -1 : result.getStatusCode()));
		return operation + " failed: " + LfgErrorSanitizer.sanitize(message, secrets());
	}

	private String linkAccountHint()
	{
		return "Link your RuneScape account in Discord with /lfg link-account rsn:<your-rsn>.";
	}

	private void chatSuccess(String message)
	{
		if (config.lfgShowChatMessages())
		{
			queueChat("Success", message);
		}
		log.debug("LFG success: {}", message);
	}

	private void chatWarning(String message)
	{
		if (StringUtils.isBlank(message))
		{
			return;
		}
		if (config.lfgShowChatMessages())
		{
			queueChat("Warning", message);
		}
		log.debug("LFG warning: {}", message);
	}

	private void queueChat(String category, String message)
	{
		String formatted = String.format("[%s] %s: %s", "Tangle Crew Plugin", category, message);
		chatMessageManager.queue(
			QueuedMessage.builder()
				.type(net.runelite.api.ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(formatted)
				.build()
		);
	}

	private void clearAnnouncements()
	{
		announcedGroupKeys.clear();
	}

	private void maybeAnnounceVisibleGroups()
	{
		if (shutdown.get() || !enabled.get() || !loggedIn.get() || !config.lfgShowChatMessages())
		{
			return;
		}

		announceVisibleGroups(filterGroups(filterCategories()), currentIdentity);
	}

	void announceVisibleGroups(List<LfgGroup> groups, PlayerIdentity identity)
	{
		if (shutdown.get() || !enabled.get() || !loggedIn.get() || !config.lfgShowChatMessages())
		{
			return;
		}
		if (groups == null || groups.isEmpty() || identity == null || StringUtils.isBlank(identity.getUsername()))
		{
			return;
		}

		for (LfgGroup group : groups)
		{
			if (group == null || !isJoinableActiveGroup(group) || isOwnedByCurrentPlayer(group, identity))
			{
				continue;
			}

			String announcementKey = buildAnnouncementKey(group);
			if (!announcedGroupKeys.add(announcementKey))
			{
				continue;
			}

			String message = buildAnnouncementMessage(group);
			queueChat("LFG", message);
			log.debug("LFG announcement: {}", message);
		}
	}

	private boolean isJoinableActiveGroup(LfgGroup group)
	{
		if (group == null || group.getStatus() == null || group.getPermissions() == null)
		{
			return false;
		}
		if (!group.getPermissions().isCanJoin())
		{
			return false;
		}
		return group.getStatus() == tccrewplugin.lfg.model.LfgGroupStatus.OPEN
			|| group.getStatus() == tccrewplugin.lfg.model.LfgGroupStatus.FULL;
	}

	private boolean isOwnedByCurrentPlayer(LfgGroup group, PlayerIdentity identity)
	{
		if (group == null || identity == null || StringUtils.isBlank(identity.getUsername()))
		{
			return false;
		}

		String current = identity.getUsername().trim().toLowerCase(Locale.ROOT);
		if (group.getCreator() != null && StringUtils.isNotBlank(group.getCreator().getRsn()))
		{
			String creator = group.getCreator().getRsn().trim().toLowerCase(Locale.ROOT);
			if (current.equals(creator))
			{
				return true;
			}
		}
		return isCurrentPlayerMember(group, identity.getUsername());
	}

	private String buildAnnouncementKey(LfgGroup group)
	{
		return StringUtils.defaultString(group.getId()) + ":" + group.getVersion();
	}

	private String buildAnnouncementMessage(LfgGroup group)
	{
		String category = group.getCategory() == null ? "LFG" : StringUtils.defaultIfBlank(group.getCategory().getDisplayName(), "LFG");
		String activity = Utils.truncate(Utils.sanitize(StringUtils.defaultString(group.getActivity())), 60);
		String creator = group.getCreator() == null ? "" : Utils.sanitize(StringUtils.defaultString(group.getCreator().getRsn()));
		String players = formatPlayers(group);
		StringBuilder sb = new StringBuilder();
		sb.append(category).append(": ").append(activity).append(" (").append(players).append(")");
		if (StringUtils.isNotBlank(creator))
		{
			sb.append(" by ").append(creator);
		}
		return sb.toString();
	}

	private String formatPlayers(LfgGroup group)
	{
		int members = group.getMembers() == null ? 0 : group.getMembers().size();
		Integer maximum = group.getMaximumPlayers();
		if (maximum == null)
		{
			return members + "/unlimited";
		}
		return members + "/" + maximum;
	}

	private Set<String> secrets()
	{
		Set<String> secrets = new HashSet<>();
		if (StringUtils.isNotBlank(config.lfgApiToken()))
		{
			secrets.add(config.lfgApiToken());
		}
		if (StringUtils.isNotBlank(config.lfgMasterChannelWebhook()))
		{
			secrets.add(config.lfgMasterChannelWebhook());
		}
		return secrets;
	}

	private Set<String> getAllowedCategoryKeys()
	{
		return filterCategories().stream()
			.map(category -> category.getKey() == null ? "" : category.getKey().trim().toLowerCase(Locale.ROOT))
			.collect(Collectors.toSet());
	}

	private Instant parseInstant(String value)
	{
		if (StringUtils.isBlank(value))
		{
			return null;
		}
		try
		{
			return Instant.parse(value.trim());
		}
		catch (Exception ex)
		{
			throw new IllegalArgumentException("Start time must be a valid ISO-8601 instant.");
		}
	}
}
