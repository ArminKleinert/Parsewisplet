package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.error.ParserCreationFailure
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

internal class TestVariableRepetitionFailures {
    @Test
    fun parseWithPartial1() {
        val text = "aaaaaa"
        val treesPartial = setOf(
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
                        S = 'a'{0,6} 'a'
                        
                        """.trimIndent(),
            creationOpts
        )
        //System.out.println(repeated_a);
        Assertions.assertEquals(treesPartial, repeated_a.parses(text, partialOpts).toSet())
    }

    @Test
    fun parseWithPartial2() {
        val text = "baaaaa"
        val treesPartial = setOf(
            PT.create("S", "b"),
            PT.create("S", "b", "a"),
            PT.create("S", "b", "a", "a"),
            PT.create("S", "b", "a", "a", "a"),
            PT.create("S", "b", "a", "a", "a", "a"),
            PT.create("S", "b", "a", "a", "a", "a", "a")
        )
        val partialOpts = ParsingOptions.default.withPartial(true)
        val creationOpts = ParserCreationOptions.default
        val repeated_a = parser(
            """
                        S = 'b' 'a'{0,6}
                        
                        """.trimIndent(),
            creationOpts
        )
        //System.out.println(repeated_a);
        Assertions.assertEquals(treesPartial, repeated_a.parses(text, partialOpts).toSet())
    }

    @Test
    fun parseFullWithPartial() {
        val text = "aaaaaa"
        val treesPartial = setOf(
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
                        S = 'a'{1,6}
                        
                        """.trimIndent(),
            creationOpts
        )
        //System.out.println(repeated_a);
        Assertions.assertEquals(treesPartial, repeated_a.parses(text, partialOpts).toSet())
    }

    @Test
    fun parseRepetitionMinMax() {
        val p = parser("S = 'a' {2,4}")
        Assertions.assertTrue(p.parse("").isFailure)
        Assertions.assertTrue(p.parse("a").isFailure)
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a"), p.parse("aaaa"))
        Assertions.assertTrue(p.parse("aaaaa").isFailure)
    }

    @Test
    fun createRepetitionParserFailure() {
        // Negative minimum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = 'a' {-1,6}") })
        // Negative maximum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = 'a' {,-1}") })
        // Negative minimum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = 'a' {-1,}") })
        // Negative exact
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = 'a' {-1}") })
        // Minimum greater than maximum
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser("S = 'a' {4,2}") })
    }
}
