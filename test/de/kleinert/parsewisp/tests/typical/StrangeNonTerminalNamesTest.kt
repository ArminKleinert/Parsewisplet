package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parsing.EOFTerm
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.ThrowingSupplier

internal class StrangeNonTerminalNamesTest {
    @Test
    fun nonTerminalStartsWithEpsilonName() {
        val opts = ParserCreationOptions.getDefault()

        Assertions.assertDoesNotThrow<Parser>(ThrowingSupplier { parser("S = EpsNT\nEpsNT = \"1\"", opts) })

        val p = parser("S = EpsNT\nEpsNT = \"1\"", opts)
        Assertions.assertEquals(
            PT.create("S", PT.create("EpsNT", "1")),
            p.parse("1")
        )
    }

    @Test
    fun nonTerminalStartsWithEofName() {
        val opts = ParserCreationOptions.getDefault()

        Assertions.assertDoesNotThrow<Parser>(ThrowingSupplier {
            parser(
                "S = " + EOFTerm.text() + "NT\n" + EOFTerm.text() + "NT = \"1\"",
                opts
            )
        })

        val p = parser("S = EpsNT\nEpsNT = \"1\"", opts)
        Assertions.assertEquals(
            PT.create("S", PT.create("EpsNT", "1")),
            p.parse("1")
        )
    }
}
