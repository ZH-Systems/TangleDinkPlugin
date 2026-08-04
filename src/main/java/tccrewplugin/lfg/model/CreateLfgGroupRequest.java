package tccrewplugin.lfg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLfgGroupRequest
{
	private String categoryKey;
	private String activity;
	private String description;
	private Instant startTime;
	private Integer maximumPlayers;
}
