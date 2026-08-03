package tccrewplugin.clanchat;

import com.google.common.base.Ticker;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClanMessageDeduplicatorTest
{
	@Test
	void suppressesDuplicateWithinWindow()
	{
		FakeTicker ticker = new FakeTicker();
		ClanMessageDeduplicator deduplicator = new ClanMessageDeduplicator(ticker);

		assertTrue(deduplicator.recordIfNew("key"));
		assertFalse(deduplicator.recordIfNew("key"));

		ticker.advance(6, TimeUnit.SECONDS);
		assertTrue(deduplicator.recordIfNew("key"));
	}

	private static final class FakeTicker extends Ticker
	{
		private long nanos;

		@Override
		public long read()
		{
			return nanos;
		}

		private void advance(long time, TimeUnit unit)
		{
			nanos += unit.toNanos(time);
		}
	}
}
