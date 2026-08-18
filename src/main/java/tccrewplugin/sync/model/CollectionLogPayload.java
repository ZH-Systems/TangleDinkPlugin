package tccrewplugin.sync.model;

import lombok.Value;

import java.util.Base64;
import java.util.BitSet;

@Value
public class CollectionLogPayload
{
	int mappingVersion;
	int itemCount;
	int ownedCount;
	String slots;

	public CollectionLogPayload(int mappingVersion, int itemCount, int ownedCount, String slots)
	{
		this.mappingVersion = mappingVersion;
		this.itemCount = itemCount;
		this.ownedCount = ownedCount;
		this.slots = slots == null ? "" : slots;
	}

	public static String encode(BitSet bits)
	{
		if (bits == null)
		{
			return "";
		}
		return Base64.getEncoder().encodeToString(bits.toByteArray());
	}

	public static BitSet decode(String encoded)
	{
		if (encoded == null || encoded.isEmpty())
		{
			return new BitSet();
		}
		return BitSet.valueOf(Base64.getDecoder().decode(encoded));
	}
}
