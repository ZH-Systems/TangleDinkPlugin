package tccrewplugin.lfg;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

@UtilityClass
public class LfgErrorSanitizer
{
	public String sanitize(String input, Collection<String> secrets)
	{
		if (StringUtils.isBlank(input))
		{
			return "";
		}

		String sanitized = input;
		if (secrets != null)
		{
			for (String secret : secrets)
			{
				if (StringUtils.isNotBlank(secret))
				{
					sanitized = sanitized.replace(secret, "[redacted]");
				}
			}
		}
		return sanitized.trim();
	}

	public String sanitizeThrowable(Throwable throwable, Collection<String> secrets)
	{
		String message = throwable == null ? "unknown error" : throwable.getMessage();
		return sanitize(message, secrets);
	}
}
