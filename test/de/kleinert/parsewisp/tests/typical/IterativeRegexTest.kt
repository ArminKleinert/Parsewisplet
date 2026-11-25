package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.List
import java.util.Set

internal class IterativeRegexTest {
    @Test
    fun testCheckByPartials() {
        val p = parser("S = #'A+'")
        val parses = p.parses(
            "AAAA",
            ParsingOptions.getDefault()
                .withIterativeDeepening(true)
                .withPartial(true)
        )
        Assertions.assertEquals(
            List.of<ParseTree?>(
                PT.create("S", "A"),
                PT.create("S", "AA"),
                PT.create("S", "AAA"),
                PT.create("S", "AAAA")
            ),
            parses
        )
    }

    @Test
    fun testRegexString() {
        val p = parser("S = #'A+' 'A' | #'A+' 'AA' | #'A+' 'AAA' | #'A+' 'AAAA'")

        Assertions.assertTrue(p.parses("").isEmpty())
        Assertions.assertTrue(p.parses("ABA").isEmpty())

        val parses = p.parses(
            "AAAA",
            ParsingOptions.getDefault().withIterativeDeepening(true)
        )
        Assertions.assertEquals(
            Set.of<ParseTree?>(
                PT.create("S", "A", "AAA"),
                PT.create("S", "AA", "AA"),
                PT.create("S", "AAA", "A")
            ),
            HashSet<ParseTree?>(parses)
        )
    }

    @Test
    fun testStringRegex() {
        val p = parser("S = 'A' #'A+' | 'AA' #'A+' | 'AAA' #'A+' | 'AAAA' #'A+'")

        Assertions.assertTrue(p.parses("").isEmpty())
        Assertions.assertTrue(p.parses("ABA").isEmpty())

        val parses = p.parses(
            "AAAA",
            ParsingOptions.getDefault().withIterativeDeepening(true)
        )
        Assertions.assertEquals(
            Set.of<ParseTree?>(
                PT.create("S", "A", "AAA"),
                PT.create("S", "AA", "AA"),
                PT.create("S", "AAA", "A")
            ),
            HashSet<ParseTree?>(parses)
        )
    }

    @Test
    fun testRegexRegex() {
        val p = parser("S = #'A+' #'A+'")

        Assertions.assertTrue(p.parses("").isEmpty())
        Assertions.assertTrue(p.parses("ABA").isEmpty())

        val parses = p.parses(
            "AAAA",
            ParsingOptions.getDefault().withIterativeDeepening(true)
        )
        Assertions.assertEquals(
            Set.of<ParseTree?>(
                PT.create("S", "A", "AAA"),
                PT.create("S", "AA", "AA"),
                PT.create("S", "AAA", "A")
            ),
            HashSet<ParseTree?>(parses)
        )
    }

    @Test
    fun defaultTest() {
        val p = parser("S = #'A+' 'A'")
        val opts = ParsingOptions.getDefault().withIterativeDeepening(true)
        Assertions.assertEquals(
            PT.create("S", "AA", "A"),
            p.parse("AAA", opts)
        )
    }
}