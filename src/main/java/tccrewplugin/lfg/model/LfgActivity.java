package tccrewplugin.lfg.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LfgActivity
{
	@SerializedName("id")
	private String id;
	@SerializedName("key")
	private String key;
	@SerializedName("displayName")
	private String displayName;
	@SerializedName("discordRoleName")
	private String discordRoleName;
	@SerializedName("description")
	private String description;
	@SerializedName("enabled")
	private boolean enabled;
	@SerializedName("displayOrder")
	private int displayOrder;
	@SerializedName("maximumPlayers")
	private Integer maximumPlayers;
	@SerializedName("supportsMass")
	private boolean supportsMass;
	@SerializedName("emoji")
	private String emoji;
	@SerializedName("colorHex")
	private String colorHex;
	@SerializedName("source")
	private String source;

	@Override
	public String toString()
	{
		return StringUtils.defaultIfBlank(displayName, StringUtils.defaultIfBlank(key, "Activity"));
	}
}
