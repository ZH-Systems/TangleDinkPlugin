package tccrewplugin.lfg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LfgMember
{
	private String playerId;
	private String rsn;
	private String discordUserId;
	private LfgSource source;
	private Instant joinedAt;
}
