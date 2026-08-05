package tccrewplugin.lfg.model;

import com.google.gson.annotations.JsonAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tccrewplugin.util.InstantAdapter;

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
	@JsonAdapter(InstantAdapter.class)
	private Instant joinedAt;
}
