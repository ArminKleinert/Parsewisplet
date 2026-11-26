package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.parser_options.UnhideOptions
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class UnhideOptionsTest {
    @Test
    fun testUnhide1() {
        val p = Parsewisp.parser(
            """
                S   = A <B>
                <A> = 'a'
                B   = 'b'
                
                """.trimIndent()
        )
        val text = "ab"

        Assertions.assertEquals(
            PT.create("S", "a"),
            p.parse(text)
        )
        Assertions.assertEquals(
            PT.create("S", "a", PT.create("B", "b")),
            p.parse(text, ParsingOptions.default.withUnhide(UnhideOptions.CONTENT))
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", "a")),
            p.parse(text, ParsingOptions.default.withUnhide(UnhideOptions.TAGS))
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", "a"), PT.create("B", "b")),
            p.parse(text, ParsingOptions.default.withUnhide(UnhideOptions.ALL))
        )
    }
}
