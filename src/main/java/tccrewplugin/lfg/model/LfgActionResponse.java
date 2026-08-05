package tccrewplugin.lfg.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LfgActionResponse
{
	@SerializedName("success")
	private boolean success;
	@SerializedName("message")
	private String message;
	@SerializedName("group")
	private JsonElement group;
	@SerializedName("error")
	private JsonElement error;

	public String getErrorMessage()
	{
		if (error == null || error.isJsonNull())
		{
			return "";
		}
		if (error.isJsonPrimitive())
		{
			return error.getAsString();
		}
		if (!error.isJsonObject())
		{
			return error.toString();
		}

		JsonObject object = error.getAsJsonObject();
		String code = object.has("code") && !object.get("code").isJsonNull() ? object.get("code").getAsString() : "";
		String message = object.has("message") && !object.get("message").isJsonNull() ? object.get("message").getAsString() : "";
		String details = object.has("details") && !object.get("details").isJsonNull() ? object.get("details").getAsString() : "";
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isNotBlank(code))
		{
			sb.append(code).append(": ");
		}
		sb.append(StringUtils.defaultIfBlank(message, ""));
		if (StringUtils.isNotBlank(details) && !details.equals(message))
		{
			if (sb.length() > 0)
			{
				sb.append(" - ");
			}
			sb.append(details);
		}
		return sb.toString().trim();
	}
}
