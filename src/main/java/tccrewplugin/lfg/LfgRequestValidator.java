package tccrewplugin.lfg;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import tccrewplugin.lfg.model.LfgActivity;
import tccrewplugin.lfg.model.LfgCategory;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
public class LfgRequestValidator
{
	public ValidationResult validateCreateRequest(
		String categoryKey,
		String activityKey,
		String description,
		Integer maximumPlayers,
		Instant startTime,
		List<LfgCategory> availableCategories
	)
	{
		String normalizedCategory = trim(categoryKey);
		String normalizedActivityKey = trim(activityKey);
		String normalizedDescription = trim(description);

		LfgCategory category = findCategory(normalizedCategory, availableCategories);
		if (category == null)
		{
			return ValidationResult.invalid("Choose a valid category.");
		}
		LfgActivity activity = findActivity(category, normalizedActivityKey);
		if (activity == null)
		{
			return ValidationResult.invalid("Choose a valid activity.");
		}
		String normalizedActivity = trim(activity.getDisplayName());
		if (!isTextAllowed(normalizedActivity) || normalizedActivity.length() < 1 || normalizedActivity.length() > 80)
		{
			return ValidationResult.invalid("Selected activity is invalid.");
		}
		if (!isTextAllowed(normalizedDescription) || normalizedDescription.length() > 300)
		{
			return ValidationResult.invalid("Description must be 300 characters or fewer.");
		}
		if (maximumPlayers != null && (maximumPlayers < 1 || maximumPlayers > 100))
		{
			return ValidationResult.invalid("Maximum players must be between 1 and 100, or blank for unlimited.");
		}
		if (startTime != null && startTime.isBefore(Instant.now().minusSeconds(1)))
		{
			return ValidationResult.invalid("Start time must be now or in the future.");
		}
		return ValidationResult.valid(new ValidationResult(
			true,
			"",
			normalizedCategory,
			normalizedActivityKey,
			normalizedActivity,
			normalizedDescription,
			maximumPlayers,
			startTime
		));
	}

	public ValidationResult validateActionRequest(String groupId)
	{
		if (StringUtils.isBlank(groupId))
		{
			return ValidationResult.invalid("Select a valid group first.");
		}
		return ValidationResult.valid(new ValidationResult(true, "", trim(groupId), "", "", "", null, null));
	}

	private boolean isTextAllowed(String value)
	{
		if (value == null)
		{
			return true;
		}
		for (int i = 0; i < value.length(); i++)
		{
			if (Character.isISOControl(value.charAt(i)))
			{
				return false;
			}
		}
		return true;
	}

	private String trim(String value)
	{
		return value == null ? "" : value.trim().replace('\u00A0', ' ');
	}

	private LfgCategory findCategory(String categoryKey, List<LfgCategory> availableCategories)
	{
		if (StringUtils.isBlank(categoryKey) || availableCategories == null)
		{
			return null;
		}
		for (LfgCategory category : availableCategories)
		{
			if (category != null && category.getKey() != null && categoryKey.equalsIgnoreCase(category.getKey().trim()))
			{
				return category;
			}
		}
		return null;
	}

	private LfgActivity findActivity(LfgCategory category, String activityKey)
	{
		if (category == null || StringUtils.isBlank(activityKey) || category.getActivities() == null)
		{
			return null;
		}
		for (LfgActivity activity : category.getActivities())
		{
			if (activity != null && activity.getKey() != null && activityKey.equalsIgnoreCase(activity.getKey().trim()))
			{
				return activity;
			}
		}
		return null;
	}

	@Getter
	@AllArgsConstructor
	public static final class ValidationResult
	{
		private final boolean valid;
		private final String message;
		private final String categoryKey;
		private final String activityKey;
		private final String activity;
		private final String description;
		private final Integer maximumPlayers;
		private final Instant startTime;

		private static ValidationResult invalid(String message)
		{
			return new ValidationResult(false, message, "", "", "", "", null, null);
		}

		private static ValidationResult valid(ValidationResult result)
		{
			return result;
		}
	}
}
