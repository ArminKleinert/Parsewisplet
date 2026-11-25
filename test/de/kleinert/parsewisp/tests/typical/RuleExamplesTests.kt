package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parser_options.RedefinitionOption
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class RuleExamplesTests {
    @Test
    fun testChoiceExample1() {
        val p1 = parser("S = 'a' | 'b' | 'ab'")
        Assertions.assertEquals(PT.create("S", "a"), p1.parse("a"))
        Assertions.assertEquals(PT.create("S", "b"), p1.parse("b"))
        Assertions.assertEquals(PT.create("S", "ab"), p1.parse("ab"))

        val opts = ParserCreationOptions
            .default
            .withRedefinitionOption(RedefinitionOption.CHOICE)
        val p2 = parser(
            """
                S =  'a'
                S =/ 'b'
                S =/ 'ab'
                
                """.trimIndent(), opts
        )
        Assertions.assertEquals(PT.create("S", "a"), p2.parse("a"))
        Assertions.assertEquals(PT.create("S", "b"), p2.parse("b"))
        Assertions.assertEquals(PT.create("S", "ab"), p2.parse("ab"))
    }
}
