package tccrewplugin.lfg;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LfgErrorSanitizerTest
{
	@Test
	void redactsSecretsFromMessages()
	{
		String sanitized = LfgErrorSanitizer.sanitize("token=abc123 webhook=https://example", List.of("abc123", "https://example"));

		assertFalse(sanitized.contains("abc123"));
		assertFalse(sanitized.contains("https://example"));
		assertTrue(sanitized.contains("[redacted]"));
	}
}
