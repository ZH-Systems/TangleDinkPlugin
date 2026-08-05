package tccrewplugin.lfg;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;
import tccrewplugin.lfg.model.LfgActionResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LfgActionResponseTest
{
	@Test
	void parsesStringErrorResponses()
	{
		String json = "{\"success\":false,\"message\":\"create failed\",\"group\":null,\"error\":\"Expected BEGIN_OBJECT but was STRING\"}";
		LfgActionResponse response = new Gson().fromJson(json, LfgActionResponse.class);
		assertNotNull(response);
		assertNotNull(response.getError());
		JsonElement error = response.getError();
		assertEquals("Expected BEGIN_OBJECT but was STRING", error.getAsString());
		assertEquals("Expected BEGIN_OBJECT but was STRING", response.getErrorMessage());
	}
}
