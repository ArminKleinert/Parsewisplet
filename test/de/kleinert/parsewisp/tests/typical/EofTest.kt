package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parsing.EOFTerm
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class EofTest {
    @Test
    fun basicTest1() {
        val opts = ParserCreationOptions.default

        Assertions.assertEquals(
            PT.create("S"),
            Parsewisp.parser("S = " + EOFTerm.text(), opts).parse("")
        )

        Assertions.assertEquals(
            PT.create("S"),
            Parsewisp.parser("S = <' '>" + EOFTerm.text(), opts).parse(" ")
        )

        Assertions.assertEquals(
            PT.create("S", "a"),
            Parsewisp.parser("S = 'a' " + EOFTerm.text(), opts).parse("a")
        )
    }

    @Test
    fun eofInParserWithWhitespace() {
        val opts = ParserCreationOptions.newWithStandardWhitespace()

        Assertions.assertEquals(
            PT.create("S"),
            Parsewisp.parser("S = " + EOFTerm.text(), opts).parse("")
        )

        Assertions.assertEquals(
            PT.create("S"),
            Parsewisp.parser("S = " + EOFTerm.text(), opts).parse(" ")
        )
    }
}
