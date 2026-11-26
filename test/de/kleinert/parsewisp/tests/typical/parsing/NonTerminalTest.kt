package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.ConcatRule
import de.kleinert.parsewisp.parsing.NonTerminal
import de.kleinert.parsewisp.parsing.Rule
import de.kleinert.parsewisp.parsing.StringTerm
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class NonTerminalTest {
    private fun nt(name: String): NonTerminal {
        return NonTerminal.create(Sym.sym(name))
    }

    private fun simpleRuleForTests(): NonTerminal {
        return NonTerminal.create(Sym.sym("A"))
    }

    @Test
    fun create() {
        Assertions.assertEquals(
            NonTerminal.create(Sym.sym("S")), NonTerminal.create(Sym.sym("S"))
        )
        Assertions.assertNotEquals(
            NonTerminal.create(Sym.sym("S1")), NonTerminal.create(Sym.sym("S2"))
        )
    }

    @Test
    fun getKeyword() {
        Assertions.assertEquals(
            Sym.sym("S"),
            NonTerminal.create(Sym.sym("S")).keyword
        )
    }

    @Test
    fun parse() {
        val p = Parsewisp.parser(
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
        val p = Parsewisp.parser(
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
        val p = Parsewisp.parser(
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
        val p = Parsewisp.parser(
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
        val p = Parsewisp.parser(
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
        Assertions.assertFalse(simpleRuleForTests().withHideTag(false).isHiddenInResult)
        Assertions.assertFalse(simpleRuleForTests().includeInResult().isHiddenInResult)
        Assertions.assertTrue(simpleRuleForTests().withHideTag(true).isHiddenInResult)
        Assertions.assertTrue(simpleRuleForTests().hideInResult().isHiddenInResult)

        // Alternative names give equivalent output
        Assertions.assertEquals(simpleRuleForTests().withHideTag(false), simpleRuleForTests().includeInResult())
        Assertions.assertEquals(simpleRuleForTests().withHideTag(true), simpleRuleForTests().hideInResult())
    }

    @Test
    fun withHideTagParse() {
        // By default, not hidden in output
        val pDefault = Parsewisp.parser(
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
        val pShow = Parsewisp.parser(
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
        val pHide = Parsewisp.parser(
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
        val pDefault = Parsewisp.parser(
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
        val pDefault = Parsewisp.parser(
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
        val pDefault = Parsewisp.parser(
            Gr.grammar(
                "S", nt("A"),
                "A", nt("B").withReduction(ReductionType.taggedTree(Sym.sym("C"))),
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
        val pDefault = Parsewisp.parser(
            Gr.grammar(
                "S", nt("A"),
                "A", ConcatRule.create(
                    listOf(
                        nt("B").withReduction(ReductionType.taggedTree(Sym.sym("C"))),
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