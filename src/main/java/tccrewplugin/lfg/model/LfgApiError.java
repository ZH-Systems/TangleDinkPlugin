package tccrewplugin.lfg.model;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonAdapter(LfgApiError.Adapter.class)
public class LfgApiError
{
	private String code;
	private String message;
	private String details;

	public static final class Adapter implements JsonDeserializer<LfgApiError>
	{
		@Override
		public LfgApiError deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			if (json == null || json.isJsonNull())
			{
				return null;
			}

			if (json.isJsonPrimitive())
			{
				JsonPrimitive primitive = json.getAsJsonPrimitive();
				if (primitive.isString())
				{
					String value = primitive.getAsString();
					return new LfgApiError("ERROR", value, null);
				}
				return new LfgApiError("ERROR", primitive.toString(), null);
			}

			JsonObject object = json.getAsJsonObject();
			String code = object.has("code") && !object.get("code").isJsonNull() ? object.get("code").getAsString() : null;
			String message = object.has("message") && !object.get("message").isJsonNull() ? object.get("message").getAsString() : null;
			String details = object.has("details") && !object.get("details").isJsonNull() ? object.get("details").getAsString() : null;
			return new LfgApiError(code, message, details);
		}
	}
}
