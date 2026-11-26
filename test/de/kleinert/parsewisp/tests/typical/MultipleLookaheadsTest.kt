package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

internal class MultipleLookaheadsTest {
    @Test
    fun contradictoryLookaheads() {
        val p = parser("S := &'a' &'b' ('a' | 'b' | 'c')+")
        Assertions.assertTrue(p.parse("a").isFailure)
        Assertions.assertTrue(p.parse("b").isFailure)
        Assertions.assertTrue(p.parse("c").isFailure)
    }

    @Test
    fun lookaheads1() {
        val grammarAsMap = LinkedHashMap<Sym, Rule>()
        grammarAsMap[sym("S")] = ConcatRule.create(
            listOf(
                LookaheadRule.create(LookaheadRule.create(StringTerm.create("a", false))),
                RegexTerm.create(Pattern.compile("[abc]"))
            )
        )
        val p = parser(
            Grammar(
                sym("S"),
                grammarAsMap
            ),
            ParserCreationOptions.default.withStartProduction(sym("S"))
        )
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertTrue(p.parse("b").isFailure)
    }

    @Test
    fun doubledLookahead() {
        val p = parser("S = &'a' &'a' ('a' | 'b' | 'c')+")
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertTrue(p.parse("b").isFailure)
    }

    @Test
    fun doubledNegativeLookahead() {
        val p = parser("S = !'a' !'a' ('a' | 'b' | 'c')+")
        Assertions.assertEquals(PT.create("S", "b"), p.parse("b"))
        Assertions.assertEquals(PT.create("S", "c"), p.parse("c"))
        Assertions.assertTrue(p.parse("a").isFailure)
    }

    @Test
    fun doubledNegativeLookahead1() {
        val p = parser("S = !'a' !'b' ('a' | 'b' | 'c')+")
        Assertions.assertEquals(PT.create("S", "c"), p.parse("c"))
        Assertions.assertTrue(p.parse("a").isFailure)
        Assertions.assertTrue(p.parse("b").isFailure)
    }
}
