package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class IdentifierNamesTest {
    @Test
    fun extendedNamesAllowed() {
        val opts = ParserCreationOptions.default
        Assertions.assertThrows(Exception::class.java, { parser("1 = 'a'", opts) })
        Assertions.assertDoesNotThrow({ parser("S = 'a'", opts) })
        Assertions.assertDoesNotThrow({ parser("\uD83C\uDF81 = 'a'", opts) })
        Assertions.assertDoesNotThrow({ parser("a123 = 'a'", opts) })
        Assertions.assertDoesNotThrow({ parser("a_123 = 'a'", opts) })
    }
}
