package tccrewplugin.clanchat;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.regex.Pattern;

@UtilityClass
public class ClanMessageSanitizer
{
	private static final Pattern IMG_TAG = Pattern.compile("<img=\\d+>");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	public String sanitizeAuthor(String rawAuthor)
	{
		if (rawAuthor == null)
		{
			return "";
		}

		String sanitized = rawAuthor
			.replace((char) 160, ' ');
		sanitized = IMG_TAG.matcher(sanitized).replaceAll("");
		sanitized = WHITESPACE.matcher(sanitized).replaceAll(" ");
		return sanitized.trim();
	}

	public String sanitizeMessage(String message)
	{
		if (message == null)
		{
			return "";
		}

		String sanitized = message
			.replace((char) 160, ' ')
			.replace("<lt>", "<")
			.replace("<gt>", ">");
		sanitized = IMG_TAG.matcher(sanitized).replaceAll("");
		sanitized = WHITESPACE.matcher(sanitized).replaceAll(" ");
		return sanitized.trim();
	}

	public String normalizeWhitespace(String value)
	{
		if (value == null)
		{
			return "";
		}

		return WHITESPACE.matcher(value.replace((char) 160, ' ')).replaceAll(" ").trim();
	}

	public String lower(String value)
	{
		return value == null ? "" : value.toLowerCase(Locale.ENGLISH);
	}
}
