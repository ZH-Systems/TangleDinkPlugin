package tccrewplugin.lfg;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import tccrewplugin.lfg.model.LfgCategory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@UtilityClass
public class LfgCategoryFilter
{
	public Set<String> parseAllowList(String value)
	{
		Set<String> allowList = new LinkedHashSet<>();
		if (StringUtils.isBlank(value))
		{
			return allowList;
		}

		for (String token : value.split("[,;\\n]"))
		{
			String trimmed = token == null ? "" : token.trim();
			if (!trimmed.isEmpty())
			{
				allowList.add(trimmed.toLowerCase(Locale.ROOT));
			}
		}
		return allowList;
	}

	public List<LfgCategory> filterCategories(List<LfgCategory> categories, String allowListValue)
	{
		if (categories == null || categories.isEmpty())
		{
			return List.of();
		}

		Set<String> allowList = parseAllowList(allowListValue);
		List<LfgCategory> filtered = new ArrayList<>();
		for (LfgCategory category : categories)
		{
			if (category == null || !category.isEnabled())
			{
				continue;
			}
			if (allowList.isEmpty() || allowList.contains(normalize(category.getKey())))
			{
				filtered.add(category);
			}
		}
		return filtered;
	}

	public boolean matches(String categoryKey, String allowListValue)
	{
		if (StringUtils.isBlank(categoryKey))
		{
			return false;
		}
		Set<String> allowList = parseAllowList(allowListValue);
		return allowList.isEmpty() || allowList.contains(normalize(categoryKey));
	}

	private String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
