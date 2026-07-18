package tccrewplugin.collectionlog;

import org.junit.jupiter.api.Test;
import tccrewplugin.sync.model.CollectionLogPayload;

import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionLogCodecTest
{
	@Test
	public void codecRoundTripsBitset()
	{
		BitSet bits = new BitSet();
		bits.set(1);
		bits.set(7);
		CollectionLogCodec codec = new CollectionLogCodec();
		assertEquals(bits, codec.decode(codec.encode(bits)));
	}

	@Test
	public void mappingIsDeterministic()
	{
		CollectionLogItemMapper mapper = new CollectionLogItemMapper();
		CollectionLogItemMapper.Mapping mapping = mapper.build(List.of(10, 20), List.of(20, 5, 15));
		assertEquals(List.of(10, 20, 5, 15), mapping.getOrderedItems());
		assertEquals(3, mapping.getIndexByItemId().get(15));
	}

	@Test
	public void payloadEqualityDependsOnValues()
	{
		CollectionLogPayload a = new CollectionLogPayload(1, 2, 3, "AQ==");
		CollectionLogPayload b = new CollectionLogPayload(1, 2, 3, "AQ==");
		assertTrue(a.equals(b));
	}
}
