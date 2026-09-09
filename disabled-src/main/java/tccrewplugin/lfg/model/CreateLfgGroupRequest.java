package tccrewplugin.lfg.model;

import lombok.Value;

@Value
public class CreateLfgGroupRequest
{
	String categoryKey;
	String activity;
	String description;
	String startTime;
	Integer maximumPlayers;
}
