package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class StringTest {
    @Test
    fun explicitStringCaseInsensitivity() {
        val opts = ParserCreationOptions.default
        Assertions.assertEquals(
            PT.create("S", "A"),
            parser("S = %i\"A\"", opts).parse("A")
        )
        Assertions.assertEquals(
            PT.create("S", "A"),
            parser("S = %i\"A\"", opts).parse("a")
        )
    }

    @Test
    fun explicitStringCaseSensitivity() {
        val opts = ParserCreationOptions.default
        Assertions.assertEquals(
            PT.create("S", "A"),
            parser("S = %s\"A\"", opts).parse("A")
        )
        Assertions.assertTrue(
            parser("S = %s\"A\"", opts).parse("a").isFailure
        )
    }
}
