package tccrewplugin.clanchat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClanMessageSanitizerTest
{
	static Stream<Arguments> authorCases()
	{
		return Stream.of(
			Arguments.of(null, ""),
			Arguments.of("<img=2> Zach\u00A0Heil", "Zach Heil"),
			Arguments.of("  <img=10>  Zach   Heil  ", "Zach Heil"),
			Arguments.of("<img=2><img=10>Zach", "Zach")
		);
	}

	@ParameterizedTest
	@MethodSource("authorCases")
	void sanitizeAuthor(String input, String expected)
	{
		assertEquals(expected, ClanMessageSanitizer.sanitizeAuthor(input));
	}

	static Stream<Arguments> messageCases()
	{
		return Stream.of(
			Arguments.of(null, ""),
			Arguments.of("Hello\u00A0world", "Hello world"),
			Arguments.of("<lt>hello<gt>", "<hello>"),
			Arguments.of("<img=2> Test <img=10>", "Test"),
			Arguments.of("  multiple   spaces  here  ", "multiple spaces here")
		);
	}

	@ParameterizedTest
	@MethodSource("messageCases")
	void sanitizeMessage(String input, String expected)
	{
		assertEquals(expected, ClanMessageSanitizer.sanitizeMessage(input));
	}

	@Test
	void normalizeWhitespaceReplacesNonBreakingSpaces()
	{
		assertEquals("My Clan", ClanMessageSanitizer.normalizeWhitespace("My\u00A0Clan"));
	}
}
