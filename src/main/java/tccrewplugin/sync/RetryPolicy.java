package tccrewplugin.sync;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class RetryPolicy
{
	private final int maxAttempts;
	private final int baseDelayMillis;

	public RetryPolicy(int maxAttempts, int baseDelayMillis)
	{
		this.maxAttempts = Math.max(0, maxAttempts);
		this.baseDelayMillis = Math.max(0, baseDelayMillis);
	}

	public boolean shouldRetry(int attempt)
	{
		return attempt < maxAttempts;
	}

	public long calculateDelayMillis(int attempt)
	{
		long base = baseDelayMillis <= 0 ? 0L : (long) baseDelayMillis << Math.max(0, attempt - 1);
		long jitter = base == 0L ? 0L : ThreadLocalRandom.current().nextLong(0L, Math.max(1L, base / 4L) + 1L);
		return Math.max(0L, base + jitter);
	}

	public Duration calculateDelay(int attempt)
	{
		return Duration.ofMillis(calculateDelayMillis(attempt));
	}

	public int getMaxAttempts()
	{
		return maxAttempts;
	}
}
