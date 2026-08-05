package tccrewplugin.lfg;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import tccrewplugin.lfg.model.LfgConfigurationResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LfgConfigurationResponseTest
{
	@Test
	void parsesExactConfigurationPayload()
	{
		String json = "{" +
			"\"categories\":[" +
			"{\"id\":\"28d8b74e-ef1c-49a5-ac22-108d8b692034\",\"key\":\"boss\",\"displayName\":\"Boss\",\"description\":\"Boss groups\",\"enabled\":true,\"displayOrder\":10,\"activities\":[{\"id\":\"f1\",\"key\":\"yama\",\"displayName\":\"Yama\",\"discordRoleName\":\"Yama\",\"description\":null,\"enabled\":true,\"displayOrder\":10,\"maximumPlayers\":2,\"supportsMass\":false,\"emoji\":\"1381816093336801340\",\"colorHex\":\"#8B0000\",\"source\":\"DISCORD\"}]}," +
			"{\"id\":\"b37e6c7b-bc14-4a92-a492-42ab803da32b\",\"key\":\"raid\",\"displayName\":\"Raid\",\"description\":\"Raid groups\",\"enabled\":true,\"displayOrder\":20,\"activities\":[{\"id\":\"f2\",\"key\":\"tob\",\"displayName\":\"ToB\",\"discordRoleName\":\"ToB\",\"description\":null,\"enabled\":true,\"displayOrder\":20,\"maximumPlayers\":5,\"supportsMass\":false,\"emoji\":\"1381713627568144425\",\"colorHex\":\"#7A0C0C\",\"source\":\"DISCORD\"}]}," +
			"{\"id\":\"a69421dd-2589-4893-ad14-fa7fcd84af5d\",\"key\":\"toa\",\"displayName\":\"Tombs of Amascut\",\"description\":\"TOA-specific groups\",\"enabled\":true,\"displayOrder\":25}," +
			"{\"id\":\"aa8b0e8a-a100-4a3d-9a80-04643327f3e2\",\"key\":\"cox\",\"displayName\":\"Chambers of Xeric\",\"description\":\"COX-specific groups\",\"enabled\":true,\"displayOrder\":26}," +
			"{\"id\":\"2175bb9d-68f3-4927-961a-7420ee750136\",\"key\":\"skilling\",\"displayName\":\"Skilling\",\"description\":\"Skilling groups\",\"enabled\":true,\"displayOrder\":30}," +
			"{\"id\":\"f2b4d2c5-3e31-46e6-a83e-a35606e33524\",\"key\":\"minigame\",\"displayName\":\"Minigame\",\"description\":\"Minigame groups\",\"enabled\":true,\"displayOrder\":40}," +
			"{\"id\":\"89b04f3b-0239-4f95-ae8d-0d71ce9ac674\",\"key\":\"other\",\"displayName\":\"Other\",\"description\":\"Other groups\",\"enabled\":true,\"displayOrder\":50}," +
			"{\"id\":\"84280ece-04f3-42fd-9872-7f62b465812c\",\"key\":\"social\",\"displayName\":\"Social\",\"description\":\"Chill and social groups\",\"enabled\":true,\"displayOrder\":60}" +
			"]}";

		LfgConfigurationResponse response = new Gson().fromJson(json, LfgConfigurationResponse.class);

		assertNotNull(response);
		assertEquals(8, response.getCategories().size());
		assertEquals("boss", response.getCategories().get(0).getKey());
		assertEquals("Raid", response.getCategories().get(1).getDisplayName());
		assertEquals(1, response.getCategories().get(0).getActivities().size());
		assertEquals("yama", response.getCategories().get(0).getActivities().get(0).getKey());
		assertEquals("social", response.getCategories().get(7).getKey());
	}
}
