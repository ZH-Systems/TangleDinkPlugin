package tccrewplugin.lfg;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import tccrewplugin.lfg.model.LfgCategory;

import java.time.Instant;
import java.util.Set;

@RequiredArgsConstructor
public class LfgRequestValidator
{
	public ValidationResult validateCreateRequest(
		String categoryKey,
		String activity,
		String description,
		Integer maximumPlayers,
		Instant startTime,
		Set<String> availableCategories
	)
	{
		String normalizedCategory = trim(categoryKey);
		String normalizedActivity = trim(activity);
		String normalizedDescription = trim(description);

		if (StringUtils.isBlank(normalizedCategory) || availableCategories == null || !availableCategories.contains(normalizedCategory.toLowerCase()))
		{
			return ValidationResult.invalid("Choose a valid category.");
		}
		if (!isTextAllowed(normalizedActivity) || normalizedActivity.length() < 1 || normalizedActivity.length() > 80)
		{
			return ValidationResult.invalid("Activity must be between 1 and 80 characters.");
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
		return ValidationResult.valid(new ValidationResult(true, "", trim(groupId), null, null, null, null));
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

	@Getter
	@AllArgsConstructor
	public static final class ValidationResult
	{
		private final boolean valid;
		private final String message;
		private final String categoryKey;
		private final String activity;
		private final String description;
		private final Integer maximumPlayers;
		private final Instant startTime;

		private static ValidationResult invalid(String message)
		{
			return new ValidationResult(false, message, "", "", "", null, null);
		}

		private static ValidationResult valid(ValidationResult result)
		{
			return result;
		}
	}
}
