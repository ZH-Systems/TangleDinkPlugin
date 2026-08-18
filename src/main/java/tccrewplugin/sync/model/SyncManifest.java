package tccrewplugin.sync.model;

import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Value
public class SyncManifest
{
	int version;
	List<Integer> varbits;
	List<Integer> varps;
	List<Integer> collectionLogItems;

	public SyncManifest(int version, List<Integer> varbits, List<Integer> varps, List<Integer> collectionLogItems)
	{
		this.version = version;
		this.varbits = Collections.unmodifiableList(new ArrayList<>(varbits));
		this.varps = Collections.unmodifiableList(new ArrayList<>(varps));
		this.collectionLogItems = Collections.unmodifiableList(new ArrayList<>(collectionLogItems));
	}
}
