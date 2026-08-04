package tccrewplugin.lfg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LfgActionRequest
{
	private String action;
	private String groupId;
	private String idempotencyKey;
}
