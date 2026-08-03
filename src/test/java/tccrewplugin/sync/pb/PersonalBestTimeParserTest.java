package tccrewplugin.sync.pb;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PersonalBestTimeParserTest
{
    static Stream<Arguments> parseCases()
    {
        return Stream.of(
            Arguments.of(58.2d, 58_200L),
            Arguments.of("58.2", 58_200L),
            Arguments.of("1:02", 62_000L),
            Arguments.of("1:02.40", 62_400L),
            Arguments.of("01:02:03", 3_723_000L),
            Arguments.of("1h 2m 3.4s", 3_723_400L),
            Arguments.of("1234ms", 1_234L),
            Arguments.of("5t", 3_000L),
            Arguments.of("5 ticks", 3_000L)
        );
    }

    @ParameterizedTest
    @MethodSource("parseCases")
    @DisplayName("supported PB time formats convert to milliseconds")
    void parseCases(Object input, Long expected)
    {
        assertEquals(expected, PersonalBestTimeParser.parseToMillis(input));
    }

    @Test
    void rejectsInvalidValues()
    {
        assertNull(PersonalBestTimeParser.parseToMillis(""));
        assertNull(PersonalBestTimeParser.parseToMillis("not-a-time"));
        assertNull(PersonalBestTimeParser.parseToMillis(-1));
        assertNull(PersonalBestTimeParser.parseToMillis("0"));
    }
}
