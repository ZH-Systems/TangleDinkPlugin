package tccrewplugin.clanchat;

import tccrewplugin.util.TextSanitizer;

public class ClanMessageSanitizer
{
	public String sanitize(String text, boolean redactUrls)
	{
		String cleaned = TextSanitizer.stripTags(text);
		return redactUrls ? TextSanitizer.redactUrls(cleaned) : cleaned;
	}
}
