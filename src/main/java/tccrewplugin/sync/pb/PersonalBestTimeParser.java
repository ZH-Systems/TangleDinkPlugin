package tccrewplugin.sync.pb;

public final class PersonalBestTimeParser
{
    private static final long TICK_MILLIS = 600L;

    private PersonalBestTimeParser()
    {
    }

    public static Long parseToMillis(Object raw)
    {
        if (raw instanceof Number)
        {
            double seconds = ((Number) raw).doubleValue();
            return seconds > 0 ? Math.round(seconds * 1000.0d) : null;
        }

        if (!(raw instanceof String))
        {
            return null;
        }

        String value = ((String) raw).trim();
        if (value.isEmpty())
        {
            return null;
        }

        try
        {
            String lower = value.toLowerCase();
            if (lower.endsWith("ticks") || lower.endsWith("tick") || lower.endsWith("t"))
            {
                String trimmed = lower.replaceAll("\\s*(ticks?|t)$", "");
                long ticks = Long.parseLong(trimmed.trim());
                return ticks > 0 ? ticks * TICK_MILLIS : null;
            }

            if (lower.endsWith("ms"))
            {
                long millis = Long.parseLong(lower.substring(0, lower.length() - 2).trim());
                return millis > 0 ? millis : null;
            }

            if (lower.matches("\\d+h\\s+\\d+m\\s+\\d+(?:\\.\\d+)?s"))
            {
                String[] parts = lower.split("\\s+");
                long hours = parseTimeComponent(parts[0], 'h');
                long minutes = parseTimeComponent(parts[1], 'm');
                double seconds = Double.parseDouble(parts[2].substring(0, parts[2].length() - 1));
                long millis = hours * 3600_000L + minutes * 60_000L + Math.round(seconds * 1000.0d);
                return millis > 0 ? millis : null;
            }

            if (value.contains(":"))
            {
                String[] parts = value.split(":");
                if (parts.length == 2)
                {
                    long minutes = Long.parseLong(parts[0]);
                    double seconds = Double.parseDouble(parts[1]);
                    long millis = minutes * 60_000L + Math.round(seconds * 1000.0d);
                    return millis > 0 ? millis : null;
                }

                if (parts.length == 3)
                {
                    long hours = Long.parseLong(parts[0]);
                    long minutes = Long.parseLong(parts[1]);
                    double seconds = Double.parseDouble(parts[2]);
                    long millis = hours * 3600_000L + minutes * 60_000L + Math.round(seconds * 1000.0d);
                    return millis > 0 ? millis : null;
                }
            }

            double seconds = Double.parseDouble(value);
            return seconds > 0 ? Math.round(seconds * 1000.0d) : null;
        }
        catch (RuntimeException ex)
        {
            return null;
        }
    }

    private static long parseTimeComponent(String value, char suffix)
    {
        String cleaned = value.replace(String.valueOf(suffix), "").trim();
        return Long.parseLong(cleaned);
    }
}
