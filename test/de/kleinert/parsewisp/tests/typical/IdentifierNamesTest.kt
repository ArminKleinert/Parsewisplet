package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.api.function.ThrowingSupplier

internal class IdentifierNamesTest {
    @Test
    fun extendedNamesAllowed() {
        val opts = ParserCreationOptions.default
        Assertions.assertThrows<Exception?>(Exception::class.java, Executable { parser("1 = 'a'", opts) })
        Assertions.assertDoesNotThrow<Parser>(ThrowingSupplier { parser("S = 'a'", opts) })
        Assertions.assertDoesNotThrow<Parser>(ThrowingSupplier { parser("\uD83C\uDF81 = 'a'", opts) })
        Assertions.assertDoesNotThrow<Parser>(ThrowingSupplier { parser("a123 = 'a'", opts) })
        Assertions.assertDoesNotThrow<Parser>(ThrowingSupplier { parser("a_123 = 'a'", opts) })
    }
}
