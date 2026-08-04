package tccrewplugin.lfg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LfgActionResponse
{
	private boolean success;
	private String message;
	private LfgGroup group;
	private LfgApiError error;
}
