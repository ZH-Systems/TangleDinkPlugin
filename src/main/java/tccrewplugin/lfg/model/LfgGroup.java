package tccrewplugin.lfg.model;

import com.google.gson.annotations.JsonAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tccrewplugin.util.InstantAdapter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LfgGroup
{
	private String id;
	private int version;
	private LfgCategory category;
	private String activity;
	private String description;
	@JsonAdapter(InstantAdapter.class)
	private Instant startTime;
	private Integer maximumPlayers;
	private LfgGroupStatus status;
	private LfgSource source;
	private LfgMember creator;
	private List<LfgMember> members = new ArrayList<>();
	private LfgPermissions permissions;
	private String discordMessageId;
	@JsonAdapter(InstantAdapter.class)
	private Instant createdAt;
	@JsonAdapter(InstantAdapter.class)
	private Instant updatedAt;
	@JsonAdapter(InstantAdapter.class)
	private Instant expiresAt;
}
