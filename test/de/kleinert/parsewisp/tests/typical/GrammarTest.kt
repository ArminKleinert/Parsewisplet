package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parsing.Rule
import de.kleinert.parsewisp.parsing.StringTerm
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class GrammarTest {
    @Test
    fun grammarCollect() {
        val g = parser("S = '1' | '2' S").grammar
        val ga = g.analyze()

        Assertions.assertTrue(ga.isValid)

        Assertions.assertEquals(setOf(sym("S")), HashSet<Sym?>(ga.usedNTs()))
        Assertions.assertEquals(setOf(sym("S")), HashSet<Sym?>(ga.definedNTs()))

        Assertions.assertEquals(
            setOf(StringTerm.create("1", false), StringTerm.create("2", false)),
            ga.collect({ it: Rule? -> it is StringTerm }).toSet()
        )
    }

    @Test
    fun grammarAnalysisDoesNotRateStartSymAsUnused() {
        val ga = parser(
            """
                S = A | "a"
                A = A epsilon | epsilon
                B = "b"
                C = "c"
                
                """.trimIndent()
        ).grammar.analyze()
        Assertions.assertFalse(ga.unusedNTs.contains(sym("S")))

        val ga2 = parser("S = 'a'").grammar.analyze()
        Assertions.assertFalse(ga2.unusedNTs.contains(sym("S")))
    }

    @Test
    fun grammarAnalysis() {
        val ga = parser(
            """
                S = A | "a"
                A = A epsilon | epsilon
                B = "b"
                C = "c"
                
                """.trimIndent()
        ).grammar.analyze()
        Assertions.assertEquals(
            setOf(sym("S"), sym("A"), sym("B"), sym("C")),
            ga.definedNTs()
        )
        Assertions.assertEquals(
            setOf(sym("A")),
            ga.usedNTs()
        )
        Assertions.assertEquals(
            setOf(sym("B"), sym("C")),
            ga.unusedNTs
        )
        Assertions.assertFalse(ga.unusedNTs.contains(sym("S")))
        Assertions.assertTrue(ga.isValid)
    }

    @Test
    fun grammarSubsetFullTest() {
        val ga = parser(
            """
                S = A | "a"
                A = A B | epsilon
                B = C
                C = "c"
                """.trimIndent()
        ).grammar.analyze()
        Assertions.assertEquals(
            ga.grammar,
            ga.subGrammar(sym("S"))
        )
    }

    @Test
    fun grammarSubsetPartialTest() {
        val ga = parser(
            """
                S = A | "a"
                A = A | C
                B = C
                C = "c"
                
                """.trimIndent()
        ).grammar.analyze()
        Assertions.assertEquals(
            setOf(sym("S"), sym("A"), sym("C")),
            ga.subGrammar(sym("S")).keys
        )
        Assertions.assertEquals(
            setOf(sym("A"), sym("C")),
            ga.subGrammar(sym("A")).keys
        )
    }

    @Test
    fun grammarSubsetPartialTest1() {
        val p1 = parser(
            """
                S = A | "a"
                A = A | C
                C = "c"
                
                """.trimIndent()
        )
        val p2 = parser(
            """
                S = A | "a"
                A = A | C
                B = C
                C = "c"
                
                """.trimIndent()
        )
        Assertions.assertEquals(
            p1.grammar,
            p2.grammar.analyze().subGrammar(sym("S"))
        )
    }

    @Test
    fun grammarIsProductiveTest() {
        val noCheckOpts = ParserCreationOptions.default.withCorrectnessCheck(false)
        Assertions.assertFalse(parser("S = S", noCheckOpts).grammar.analyze().isProductive(sym("S")))
        Assertions.assertTrue(parser("S = S | epsilon", noCheckOpts).grammar.analyze().isProductive(sym("S")))

        Assertions.assertTrue(parser("S = A | epsilon; A = A", noCheckOpts).grammar.analyze().isProductive(sym("S")))
        Assertions.assertFalse(parser("S = A | epsilon; A = A", noCheckOpts).grammar.analyze().isProductive(sym("A")))
        Assertions.assertTrue(parser("S = A | epsilon; A = S", noCheckOpts).grammar.analyze().isProductive(sym("S")))

        Assertions.assertTrue(
            parser("S = A B | epsilon; A = 'a'; B = 'b'", noCheckOpts).grammar.analyze().isProductive(sym("S"))
        )
        Assertions.assertTrue(
            parser("S = A B | epsilon; A = 'a'; B = S", noCheckOpts).grammar.analyze().isProductive(sym("S"))
        )
        Assertions.assertFalse(parser("S = A B; A = 'a'; B = S", noCheckOpts).grammar.analyze().isProductive(sym("S")))
        Assertions.assertFalse(
            parser("S = A B; A = 'a'; B = C; C = B", noCheckOpts).grammar.analyze().isProductive(sym("S"))
        )
    }
}
