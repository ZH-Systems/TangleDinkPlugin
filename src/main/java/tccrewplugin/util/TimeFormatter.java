package tccrewplugin.util;

import java.time.Duration;
import java.time.Instant;

public final class TimeFormatter
{
	private TimeFormatter()
	{
	}

	public static String formatInstant(Instant instant)
	{
		return instant == null ? "never" : instant.toString();
	}

	public static String formatDurationUntil(Instant instant)
	{
		if (instant == null)
		{
			return "n/a";
		}

		Duration duration = Duration.between(Instant.now(), instant);
		if (duration.isNegative() || duration.isZero())
		{
			return "due";
		}

		long seconds = duration.getSeconds();
		long minutes = seconds / 60;
		long remainder = seconds % 60;
		if (minutes == 0)
		{
			return remainder + "s";
		}

		return minutes + "m " + remainder + "s";
	}
}
