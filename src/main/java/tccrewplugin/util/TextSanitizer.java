package tccrewplugin.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TextSanitizer
{
	private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
	private static final Pattern CONTROL_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
	private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\bhttps?://[^\\s]+");
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	private TextSanitizer()
	{
	}

	public static String stripTags(String text)
	{
		if (text == null)
		{
			return "";
		}

		String stripped = TAG_PATTERN.matcher(text).replaceAll("");
		stripped = CONTROL_PATTERN.matcher(stripped).replaceAll("");
		return WHITESPACE_PATTERN.matcher(stripped).replaceAll(" ").trim();
	}

	public static String redactUrls(String text)
	{
		if (text == null)
		{
			return "";
		}

		return URL_PATTERN.matcher(text).replaceAll("[url]");
	}

	public static String normalizeClanName(String text)
	{
		return normalizeRuneScapeName(text);
	}

	public static String normalizeRuneScapeName(String text)
	{
		String stripped = stripTags(text);
		if (stripped.isEmpty())
		{
			return "";
		}

		return WHITESPACE_PATTERN.matcher(stripped.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
	}

	public static String safePreview(String text, int maxLength)
	{
		String cleaned = stripTags(text);
		if (cleaned.length() <= maxLength)
		{
			return cleaned;
		}

		return cleaned.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
	}
}
