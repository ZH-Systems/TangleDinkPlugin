package tccrewplugin.lfg.model;

import com.google.gson.annotations.JsonAdapter;
import lombok.Value;
import tccrewplugin.util.InstantAdapter;

import java.time.Instant;

@Value
public class LfgMember
{
	String playerId;
	String rsn;
	String discordUserId;
	LfgSource source;
	@JsonAdapter(InstantAdapter.class)
	Instant joinedAt;
}
