package tccrewplugin.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public final class JsonUtils
{
	private JsonUtils()
	{
	}

	public static <T> T fromJson(Gson gson, String json, Class<T> type)
	{
		try
		{
			return gson.fromJson(json, type);
		}
		catch (JsonSyntaxException ex)
		{
			return null;
		}
	}
}
