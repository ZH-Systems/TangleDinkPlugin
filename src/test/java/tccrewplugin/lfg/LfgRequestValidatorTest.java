package tccrewplugin.lfg;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LfgRequestValidatorTest
{
	private final LfgRequestValidator validator = new LfgRequestValidator();

	@Test
	void acceptsUnlimitedMassGroup()
	{
		LfgRequestValidator.ValidationResult result = validator.validateCreateRequest(
			"raid",
			"Theatre of Blood",
			"",
			null,
			null,
			Set.of("raid")
		);

		assertTrue(result.isValid());
	}

	@Test
	void rejectsInvalidActivityLengthAndControlCharacters()
	{
		LfgRequestValidator.ValidationResult result = validator.validateCreateRequest(
			"raid",
			"",
			"",
			null,
			null,
			Set.of("raid")
		);

		assertFalse(result.isValid());
	}

	@Test
	void rejectsPastStartTimes()
	{
		LfgRequestValidator.ValidationResult result = validator.validateCreateRequest(
			"raid",
			"Theatre of Blood",
			"",
			5,
			Instant.parse("2020-01-01T00:00:00Z"),
			Set.of("raid")
		);

		assertFalse(result.isValid());
	}

	@Test
	void validatesActionGroupIds()
	{
		assertFalse(validator.validateActionRequest(" ").isValid());
		assertTrue(validator.validateActionRequest("group-123").isValid());
	}
}
