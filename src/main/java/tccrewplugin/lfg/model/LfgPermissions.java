package tccrewplugin.lfg.model;

import lombok.Value;

@Value
public class LfgPermissions
{
	boolean canJoin;
	boolean canLeave;
	boolean canClose;
}
