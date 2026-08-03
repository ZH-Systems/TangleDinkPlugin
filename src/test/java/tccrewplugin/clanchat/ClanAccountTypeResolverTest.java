package tccrewplugin.clanchat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClanAccountTypeResolverTest
{
	static Stream<Arguments> mappings()
	{
		return Stream.of(
			Arguments.of(null, AccountType.NORMAL),
			Arguments.of("", AccountType.NORMAL),
			Arguments.of("Player", AccountType.NORMAL),
			Arguments.of("<img=0>Player", AccountType.PLAYER_MODERATOR),
			Arguments.of("<img=2>Player", AccountType.IRON),
			Arguments.of("<img=10>Player", AccountType.HARDCORE_IRON),
			Arguments.of("<img=3>Player", AccountType.ULTIMATE_IRON),
			Arguments.of("<img=41>Player", AccountType.GROUP_IRON),
			Arguments.of("<img=43>Player", AccountType.UNRANKED_IRON),
			Arguments.of("<img=42>Player", AccountType.HARDCORE_GROUP_IRON)
		);
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void resolvesAccountType(String rawAuthor, AccountType expected)
	{
		assertEquals(expected, ClanAccountTypeResolver.getAccountType(rawAuthor));
	}
}
