package tccrewplugin.lfg;

import org.junit.jupiter.api.Test;
import tccrewplugin.lfg.model.LfgCategory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LfgCategoryFilterTest
{
	@Test
	void blankAllowListKeepsEnabledCategoriesInOrder()
	{
		List<LfgCategory> categories = List.of(
			new LfgCategory("1", "Boss", "Boss", "", true, 20),
			new LfgCategory("2", "Raid", "Raid", "", true, 10),
			new LfgCategory("3", "Other", "Other", "", false, 30)
		);

		List<LfgCategory> filtered = LfgCategoryFilter.filterCategories(categories, "");

		assertEquals(List.of("Boss", "Raid"), filtered.stream().map(LfgCategory::getKey).collect(Collectors.toList()));
	}

	@Test
	void allowListMatchesCaseInsensitivelyAndIgnoresUnknownEntries()
	{
		List<LfgCategory> categories = List.of(
			new LfgCategory("1", "Boss", "Boss", "", true, 10),
			new LfgCategory("2", "Raid", "Raid", "", true, 20),
			new LfgCategory("3", "Other", "Other", "", true, 30)
		);

		List<LfgCategory> filtered = LfgCategoryFilter.filterCategories(categories, "raid, missing, BOSS");

		assertEquals(List.of("Boss", "Raid"), filtered.stream().map(LfgCategory::getKey).collect(Collectors.toList()));
		assertTrue(LfgCategoryFilter.matches("raid", "RAID"));
		assertTrue(LfgCategoryFilter.parseAllowList(" raid ; boss ").containsAll(Set.of("raid", "boss")));
	}
}
