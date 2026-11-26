package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parsing.Rule
import de.kleinert.parsewisp.parsing.StringTerm
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class GrammarTest {
    @Test
    fun grammarCollect() {
        val g = Parsewisp.parser("S = '1' | '2' S").grammar
        val ga = g.analyze()

        Assertions.assertTrue(ga.isValid)

        Assertions.assertEquals(setOf(Sym.sym("S")), HashSet<Sym?>(ga.usedNTs()))
        Assertions.assertEquals(setOf(Sym.sym("S")), HashSet<Sym?>(ga.definedNTs()))

        Assertions.assertEquals(
            setOf(StringTerm.create("1", false), StringTerm.create("2", false)),
            ga.collect({ it: Rule? -> it is StringTerm }).toSet()
        )
    }

    @Test
    fun grammarAnalysisDoesNotRateStartSymAsUnused() {
        val ga = Parsewisp.parser(
            """
                S = A | "a"
                A = A epsilon | epsilon
                B = "b"
                C = "c"
                """.trimIndent()
        ).grammar.analyze()
        Assertions.assertFalse(ga.unusedNTs.contains(Sym.sym("S")))

        val ga2 = Parsewisp.parser("S = 'a'").grammar.analyze()
        Assertions.assertFalse(ga2.unusedNTs.contains(Sym.sym("S")))
    }

    @Test
    fun grammarAnalysis() {
        val ga = Parsewisp.parser(
            """
                S = A | "a"
                A = A epsilon | epsilon
                B = "b"
                C = "c"
                
                """.trimIndent()
        ).grammar.analyze()
        Assertions.assertEquals(
            setOf(Sym.sym("S"), Sym.sym("A"), Sym.sym("B"), Sym.sym("C")),
            ga.definedNTs()
        )
        Assertions.assertEquals(
            setOf(Sym.sym("A")),
            ga.usedNTs()
        )
        Assertions.assertEquals(
            setOf(Sym.sym("B"), Sym.sym("C")),
            ga.unusedNTs
        )
        Assertions.assertFalse(ga.unusedNTs.contains(Sym.sym("S")))
        Assertions.assertTrue(ga.isValid)
    }

    @Test
    fun grammarSubsetFullTest() {
        val ga = Parsewisp.parser(
            """
                S = A | "a"
                A = A B | epsilon
                B = C
                C = "c"
                """.trimIndent()
        ).grammar.analyze()
        Assertions.assertEquals(
            ga.grammar,
            ga.subGrammar(Sym.sym("S"))
        )
    }

    @Test
    fun grammarSubsetPartialTest() {
        val ga = Parsewisp.parser(
            """
                S = A | "a"
                A = A | C
                B = C
                C = "c"
                
                """.trimIndent()
        ).grammar.analyze()
        Assertions.assertEquals(
            setOf(Sym.sym("S"), Sym.sym("A"), Sym.sym("C")),
            ga.subGrammar(Sym.sym("S")).keys
        )
        Assertions.assertEquals(
            setOf(Sym.sym("A"), Sym.sym("C")),
            ga.subGrammar(Sym.sym("A")).keys
        )
    }

    @Test
    fun grammarSubsetPartialTest1() {
        val p1 = Parsewisp.parser(
            """
                S = A | "a"
                A = A | C
                C = "c"
                
                """.trimIndent()
        )
        val p2 = Parsewisp.parser(
            """
                S = A | "a"
                A = A | C
                B = C
                C = "c"
                
                """.trimIndent()
        )
        Assertions.assertEquals(
            p1.grammar,
            p2.grammar.analyze().subGrammar(Sym.sym("S"))
        )
    }

    @Test
    fun grammarIsProductiveTest() {
        val noCheckOpts = ParserCreationOptions.default.withCorrectnessCheck(false)
        Assertions.assertFalse(Parsewisp.parser("S = S", noCheckOpts).grammar.analyze().isProductive(Sym.sym("S")))
        Assertions.assertTrue(Parsewisp.parser("S = S | epsilon", noCheckOpts).grammar.analyze().isProductive(Sym.sym("S")))

        Assertions.assertTrue(Parsewisp.parser("S = A | epsilon; A = A", noCheckOpts).grammar.analyze().isProductive(Sym.sym("S")))
        Assertions.assertFalse(Parsewisp.parser("S = A | epsilon; A = A", noCheckOpts).grammar.analyze().isProductive(Sym.sym("A")))
        Assertions.assertTrue(Parsewisp.parser("S = A | epsilon; A = S", noCheckOpts).grammar.analyze().isProductive(Sym.sym("S")))

        Assertions.assertTrue(
            Parsewisp.parser("S = A B | epsilon; A = 'a'; B = 'b'", noCheckOpts).grammar.analyze().isProductive(Sym.sym("S"))
        )
        Assertions.assertTrue(
            Parsewisp.parser("S = A B | epsilon; A = 'a'; B = S", noCheckOpts).grammar.analyze().isProductive(Sym.sym("S"))
        )
        Assertions.assertFalse(Parsewisp.parser("S = A B; A = 'a'; B = S", noCheckOpts).grammar.analyze().isProductive(Sym.sym("S")))
        Assertions.assertFalse(
            Parsewisp.parser("S = A B; A = 'a'; B = C; C = B", noCheckOpts).grammar.analyze().isProductive(Sym.sym("S"))
        )
    }
}
