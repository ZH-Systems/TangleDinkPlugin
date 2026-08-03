package tccrewplugin.sync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RetryPolicyTest
{
	@Test
	public void delayStaysWithinReasonableRange()
	{
		RetryPolicy policy = new RetryPolicy(3, 1000);
		long delay = policy.calculateDelayMillis(2);
		assertTrue(delay >= 2000);
		assertTrue(delay <= 2500);
	}

	@Test
	public void shouldRetryHonorsMaxAttempts()
	{
		RetryPolicy policy = new RetryPolicy(2, 1000);
		assertTrue(policy.shouldRetry(0));
		assertTrue(policy.shouldRetry(1));
		assertTrue(!policy.shouldRetry(2));
	}
}
