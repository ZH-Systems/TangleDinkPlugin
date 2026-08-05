package tccrewplugin.lfg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LfgCategory
{
	@SerializedName("id")
	private String id;
	@SerializedName("key")
	private String key;
	@SerializedName("displayName")
	private String displayName;
	@SerializedName("description")
	private String description;
	@SerializedName("enabled")
	private boolean enabled;
	@SerializedName("displayOrder")
	private int displayOrder;

	@Override
	public String toString()
	{
		return StringUtils.defaultIfBlank(displayName, StringUtils.defaultIfBlank(key, "Category"));
	}
}
