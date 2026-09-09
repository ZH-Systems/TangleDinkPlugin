package tccrewplugin.lfg.model;

import lombok.Value;

@Value
public class LfgActionRequest
{
	String action;
	String groupId;
	String idempotencyKey;
}
