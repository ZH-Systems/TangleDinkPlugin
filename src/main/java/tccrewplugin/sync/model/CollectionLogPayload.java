package tccrewplugin.sync.model;

import java.util.Base64;
import java.util.BitSet;
import java.util.Objects;

public final class CollectionLogPayload
{
	private final int mappingVersion;
	private final int itemCount;
	private final int ownedCount;
	private final String slots;

	public CollectionLogPayload(int mappingVersion, int itemCount, int ownedCount, String slots)
	{
		this.mappingVersion = mappingVersion;
		this.itemCount = itemCount;
		this.ownedCount = ownedCount;
		this.slots = slots == null ? "" : slots;
	}

	public int getMappingVersion()
	{
		return mappingVersion;
	}

	public int getItemCount()
	{
		return itemCount;
	}

	public int getOwnedCount()
	{
		return ownedCount;
	}

	public String getSlots()
	{
		return slots;
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

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof CollectionLogPayload))
		{
			return false;
		}
		CollectionLogPayload that = (CollectionLogPayload) o;
		return mappingVersion == that.mappingVersion && itemCount == that.itemCount && ownedCount == that.ownedCount && Objects.equals(slots, that.slots);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(mappingVersion, itemCount, ownedCount, slots);
	}

	@Override
	public String toString()
	{
		return "CollectionLogPayload{" +
			"mappingVersion=" + mappingVersion +
			", itemCount=" + itemCount +
			", ownedCount=" + ownedCount +
			'}';
	}
}
