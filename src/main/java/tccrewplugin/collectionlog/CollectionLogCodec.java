package tccrewplugin.collectionlog;

import tccrewplugin.sync.model.CollectionLogPayload;

import java.util.BitSet;

public class CollectionLogCodec
{
	public String encode(BitSet bits)
	{
		return CollectionLogPayload.encode(bits);
	}

	public BitSet decode(String encoded)
	{
		return CollectionLogPayload.decode(encoded);
	}
}
