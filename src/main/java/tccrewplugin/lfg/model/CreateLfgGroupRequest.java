package tccrewplugin.lfg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLfgGroupRequest
{
	private String categoryKey;
	private String activity;
	private String description;
	private String startTime;
	private Integer maximumPlayers;
}
