package tccrewplugin.lfg;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import tccrewplugin.lfg.model.LfgGroupsResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LfgGroupsResponseTest
{
	@Test
	void parsesGroupPayloadWithIsoInstants()
	{
		String json = "{"
			+ "\"groups\":[{"
			+ "\"id\":\"group-1\","
			+ "\"version\":1,"
			+ "\"category\":{\"id\":\"cat-1\",\"key\":\"bossing\",\"displayName\":\"Bosses\",\"description\":\"Boss groups\",\"enabled\":true,\"displayOrder\":10,\"activities\":[]},"
			+ "\"activity\":\"Yama\","
			+ "\"description\":\"Test run\","
			+ "\"startTime\":\"2026-08-05T22:00:00Z\","
			+ "\"maximumPlayers\":2,"
			+ "\"status\":\"OPEN\","
			+ "\"source\":\"DISCORD\","
			+ "\"creator\":{\"playerId\":\"p1\",\"rsn\":\"Example\",\"discordUserId\":\"123\",\"source\":\"DISCORD\",\"joinedAt\":\"2026-08-05T21:50:00Z\"},"
			+ "\"members\":[{\"playerId\":\"p1\",\"rsn\":\"Example\",\"discordUserId\":\"123\",\"source\":\"DISCORD\",\"joinedAt\":\"2026-08-05T21:50:00Z\"}],"
			+ "\"permissions\":{\"canJoin\":false,\"canLeave\":true,\"canClose\":true},"
			+ "\"discordMessageId\":null,"
			+ "\"createdAt\":\"2026-08-05T21:50:00Z\","
			+ "\"updatedAt\":\"2026-08-05T21:55:00Z\","
			+ "\"expiresAt\":\"2026-08-06T04:00:00Z\""
			+ "}],"
			+ "\"message\":\"Loaded groups\""
			+ "}";

		LfgGroupsResponse response = new Gson().fromJson(json, LfgGroupsResponse.class);

		assertNotNull(response);
		assertNotNull(response.getGroups());
		assertEquals(1, response.getGroups().size());
		assertNotNull(response.getGroups().get(0).getStartTime());
		assertNotNull(response.getGroups().get(0).getCreatedAt());
		assertNotNull(response.getGroups().get(0).getMembers().get(0).getJoinedAt());
	}
}
