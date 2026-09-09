package tccrewplugin.lfg.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@Value
public class LfgActionResponse
{
	@SerializedName("success")
	boolean success;
	@SerializedName("message")
	String message;
	@SerializedName("group")
	JsonElement group;
	@SerializedName("error")
	JsonElement error;

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
