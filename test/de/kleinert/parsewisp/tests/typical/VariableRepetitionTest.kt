package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.error.ParserCreationFailure
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.util.Set

internal class VariableRepetitionTest {
    @Test
    fun parseRepetitionMinimumOnly() {
        run {
            val p = parser("S = 'a' {2,}")
            Assertions.assertTrue(p.parse("").isFailure)
            Assertions.assertTrue(p.parse("a").isFailure)
            Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
            Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa"))
            Assertions.assertEquals(PT.create("S", "a", "a", "a", "a"), p.parse("aaaa"))
            Assertions.assertEquals(PT.create("S", "a", "a", "a", "a", "a"), p.parse("aaaaa"))
        }
    }

    @Test
    fun parseRepetitionMaximumOnly() {
        run {
            val p = parser("S = 'a' {,2}")
            Assertions.assertEquals(PT.create("S"), p.parse(""))
            Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
            Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
            Assertions.assertTrue(p.parse("aaa").isFailure)
            Assertions.assertTrue(p.parse("aaaa").isFailure)
            Assertions.assertTrue(p.parse("aaaaa").isFailure)
        }
        run {
            val p = parser("S = 'a' {,0}")
            Assertions.assertEquals(PT.create("S"), p.parse(""))
            Assertions.assertTrue(p.parse("a").isFailure)
            Assertions.assertTrue(p.parse("aa").isFailure)
            Assertions.assertTrue(p.parse("aaa").isFailure)
            Assertions.assertTrue(p.parse("aaaa").isFailure)
            Assertions.assertTrue(p.parse("aaaaa").isFailure)
        }
    }

    @Test
    fun parseRepetitionMinMax() {
        run {
            val p = parser("S = 'a' {2,4}")
            Assertions.assertTrue(p.parse("").isFailure)
            Assertions.assertTrue(p.parse("a").isFailure)
            Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
            Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa"))
            Assertions.assertEquals(PT.create("S", "a", "a", "a", "a"), p.parse("aaaa"))
            Assertions.assertTrue(p.parse("aaaaa").isFailure)
        }
        run {
            val p = parser("S = 'a' {0,0}")
            Assertions.assertEquals(PT.create("S"), p.parse(""))
            Assertions.assertTrue(p.parse("a").isFailure)
            Assertions.assertTrue(p.parse("aa").isFailure)
            Assertions.assertTrue(p.parse("aaa").isFailure)
            Assertions.assertTrue(p.parse("aaaa").isFailure)
            Assertions.assertTrue(p.parse("aaaaa").isFailure)
        }
    }

    @Test
    fun parseRepetitionExact() {
        val p = parser("S = 'a' {2}")
        Assertions.assertTrue(p.parse("").isFailure)
        Assertions.assertTrue(p.parse("a").isFailure)
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
        Assertions.assertTrue(p.parse("aaa").isFailure)
        Assertions.assertTrue(p.parse("aaaa").isFailure)
    }

    @Test
    fun parseWithPartial() {
        val text = "aaaaaa"
        val treesPartial = Set.of<ParseTree?>(
            PT.create("S", "a"),
            PT.create("S", "a", "a"),
            PT.create("S", "a", "a", "a"),
            PT.create("S", "a", "a", "a", "a"),
            PT.create("S", "a", "a", "a", "a", "a"),
            PT.create("S", "a", "a", "a", "a", "a", "a")
        )
        val partialOpts = ParsingOptions.default.withPartial(true)

        val creationOpts = ParserCreationOptions.default
        val repeated_a = parser(
            """
                        S = 'a' 'a' {0,6}
                        
                        """.trimIndent(),
            creationOpts
        )
        Assertions.assertEquals(treesPartial, HashSet<ParseTree?>(repeated_a.parses(text, partialOpts)))
    }

    @Test
    fun parseFullWithPartial() {
        val text = "aaaaaa"
        val treesPartial = Set.of<ParseTree?>(
            PT.create("S", "a"),
            PT.create("S", "a", "a"),
            PT.create("S", "a", "a", "a"),
            PT.create("S", "a", "a", "a", "a"),
            PT.create("S", "a", "a", "a", "a", "a"),
            PT.create("S", "a", "a", "a", "a", "a", "a")
        )
        val partialOpts = ParsingOptions.default.withPartial(true)

        val creationOpts = ParserCreationOptions.default
        val repeated_a = parser(
            """
                        S = 'a' {1,6}
                        
                        """.trimIndent(),
            creationOpts
        )
        Assertions.assertEquals(treesPartial, HashSet<ParseTree?>(repeated_a.parses(text, partialOpts)))
    }

    @Test
    fun createRepetitionParserFormatFailure() {
        // Negative minimum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = -1*2") })
        // Negative maximum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = *-1") })
        // Negative minimum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = -1*") })
        // Negative exact
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = -1") })
        // Minimum greater than maximum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = 4*2") })
    }

    @Test
    fun createRepetitionParserFailure() {
        // Negative minimum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = -1*2 'a'") })
        // Negative maximum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = *-1 'a'") })
        // Negative minimum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = -1* 'a'") })
        // Negative exact
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = -1 'a'") })
        // Minimum greater than maximum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = 4*2 'a'") })
    }
}
