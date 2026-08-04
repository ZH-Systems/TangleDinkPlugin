package tccrewplugin.lfg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LfgPermissions
{
	private boolean canJoin;
	private boolean canLeave;
	private boolean canClose;
}
