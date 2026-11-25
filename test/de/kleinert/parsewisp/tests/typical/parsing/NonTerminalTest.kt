package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parsing.ConcatRule
import de.kleinert.parsewisp.parsing.NonTerminal
import de.kleinert.parsewisp.parsing.Rule
import de.kleinert.parsewisp.parsing.StringTerm
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.List

internal class NonTerminalTest {
    private fun nt(name: String): NonTerminal {
        return NonTerminal.create(sym(name))
    }

    private fun simpleRuleForTests(): NonTerminal {
        return NonTerminal.create(sym("A"))
    }

    @Test
    fun create() {
        Assertions.assertEquals(
            NonTerminal.create(sym("S")), NonTerminal.create(sym("S"))
        )
        Assertions.assertNotEquals(
            NonTerminal.create(sym("S1")), NonTerminal.create(sym("S2"))
        )
    }

    @Test
    fun getKeyword() {
        Assertions.assertEquals(
            sym("S"),
            NonTerminal.create(sym("S")).keyword
        )
    }

    @Test
    fun parse() {
        val p = parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    listOf<Rule>(
                        nt("A"),
                        nt("B")
                    )
                ),  // Calls parse(...) for A, but fullParse(...) for B.
                "A",
                StringTerm.create("a", false),
                "B",
                StringTerm.create("b", false)
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", "a"), PT.create("B", "b")),
            p.parse("ab")
        )
    }

    @Test
    fun parseError() {
        // A NonTerminal can't really fail. Only its related rules can.
        val p = parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    listOf<Rule>(
                        nt("A"),
                        nt("B")
                    )
                ),  // Calls parse(...) for A, but fullParse(...) for B.
                "A",
                StringTerm.create("a", false),
                "B",
                StringTerm.create("b", false)
            )
        )
        Assertions.assertTrue(p.parse("cb").isFailure)
        Assertions.assertTrue(p.parse("b").isFailure)
    }

    @Test
    fun fullParseSingle() {
        val p = parser(
            Gr.grammar(
                "S", nt("A"),
                "A", StringTerm.create("ab", false)
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", "ab")),
            p.parse("ab")
        )
    }

    @Test
    fun fullParseConcat() {
        val p = parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    listOf<Rule>(
                        nt("A"),
                        nt("B")
                    )
                ),  // Calls parse(...) for A, but fullParse(...) for B.
                "A",
                StringTerm.create("a", false),
                "B",
                StringTerm.create("b", false)
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", "a"), PT.create("B", "b")),
            p.parse("ab")
        )
    }

    @Test
    fun fullParseError() {
        // A NonTerminal can't really fail. Only its related rules can.
        val p = parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    listOf<Rule>(
                        nt("A"),
                        nt("B")
                    )
                ),  // Calls parse(...) for A, but fullParse(...) for B.
                "A",
                StringTerm.create("a", false),
                "B",
                StringTerm.create("a", false)
            )
        )
        Assertions.assertTrue(p.parse("cb").isFailure)
        Assertions.assertTrue(p.parse("b").isFailure)
    }

    @Test
    fun withHideTag() {
        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(simpleRuleForTests(), simpleRuleForTests().withHideTag(false))
        Assertions.assertEquals(simpleRuleForTests(), simpleRuleForTests().includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(simpleRuleForTests().withHideTag(false), simpleRuleForTests().withHideTag(false))
        Assertions.assertEquals(simpleRuleForTests().withHideTag(true), simpleRuleForTests().withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(simpleRuleForTests().withHideTag(false).isHiddenInResult1())
        Assertions.assertFalse(simpleRuleForTests().includeInResult().isHiddenInResult1())
        Assertions.assertTrue(simpleRuleForTests().withHideTag(true).isHiddenInResult1())
        Assertions.assertTrue(simpleRuleForTests().hideInResult().isHiddenInResult1())

        // Alternative names give equivalent output
        Assertions.assertEquals(simpleRuleForTests().withHideTag(false), simpleRuleForTests().includeInResult())
        Assertions.assertEquals(simpleRuleForTests().withHideTag(true), simpleRuleForTests().hideInResult())
    }

    @Test
    fun withHideTagParse() {
        // By default, not hidden in output
        val pDefault = parser(
            Gr.grammar(
                "S", nt("A"),
                "A", StringTerm.create("a", false)
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", "a")),
            pDefault.parse("a")
        )

        // With explicit show, also not hidden
        val pShow = parser(
            Gr.grammar(
                "S", nt("A").withHideTag(false),
                "A", StringTerm.create("a", false)
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", "a")),
            pShow.parse("a")
        )

        // With explicit hide
        val pHide = parser(
            Gr.grammar(
                "S", nt("A").withHideTag(true),
                "A", StringTerm.create("a", false)
            )
        )
        Assertions.assertEquals(PT.create("S"), pHide.parse("a"))
    }

    @Test
    fun withReduction() {
        // Default.
        Assertions.assertEquals(
            simpleRuleForTests(),
            simpleRuleForTests().withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = parser(
            Gr.grammar(
                "S", nt("A"),
                "A", nt("B"),
                "B", StringTerm.create("a", false)
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("B", "a"))),
            pDefault.parse("a")
        )
    }

    @Test
    fun withReductionMergeUp() {
        val pDefault = parser(
            Gr.grammar(
                "S", nt("A"),
                "A", nt("B"),
                "B", StringTerm.create("a", false)
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("B", "a"))),
            pDefault.parse("a")
        )
    }

    @Test
    fun withReductionTaggedTree() {
        val pDefault = parser(
            Gr.grammar(
                "S", nt("A"),
                "A", nt("B").withReduction(ReductionType.taggedTree(sym("C"))),
                "B", StringTerm.create("a", false)
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("C", PT.create("B", "a"))),
            pDefault.parse("a")
        )
    }

    @Test
    fun withReductionNotDirectRhs() {
        val pDefault = parser(
            Gr.grammar(
                "S", nt("A"),
                "A", ConcatRule.create(
                    listOf<Rule>(
                        nt("B").withReduction(ReductionType.taggedTree(sym("C"))),
                        StringTerm.create("b", false)
                    )
                ),
                "B", StringTerm.create("a", false)
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("C", PT.create("B", "a")), "b")),
            pDefault.parse("ab")
        )
    }
}