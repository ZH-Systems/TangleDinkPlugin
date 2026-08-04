package tccrewplugin.lfg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LfgCategory
{
	private String id;
	private String key;
	private String displayName;
	private String description;
	private boolean enabled;
	private int displayOrder;
}
