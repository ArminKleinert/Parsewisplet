package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parsing.EOFTerm
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class StrangeNonTerminalNamesTest {
    @Test
    fun nonTerminalStartsWithEpsilonName() {
        val opts = ParserCreationOptions.default

        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = EpsNT\nEpsNT = \"1\"", opts) })

        val p = Parsewisp.parser("S = EpsNT\nEpsNT = \"1\"", opts)
        Assertions.assertEquals(
            PT.create("S", PT.create("EpsNT", "1")),
            p.parse("1")
        )
    }

    @Test
    fun nonTerminalStartsWithEofName() {
        val opts = ParserCreationOptions.default

        Assertions.assertDoesNotThrow({
            Parsewisp.parser(
                "S = " + EOFTerm.text() + "NT\n" + EOFTerm.text() + "NT = \"1\"",
                opts
            )
        })

        val p = Parsewisp.parser("S = EpsNT\nEpsNT = \"1\"", opts)
        Assertions.assertEquals(
            PT.create("S", PT.create("EpsNT", "1")),
            p.parse("1")
        )
    }
}
