package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parsing.ConcatRule
import de.kleinert.parsewisp.parsing.NonTerminal
import de.kleinert.parsewisp.parsing.SpecialSequenceRule
import de.kleinert.parsewisp.parsing.StringTerm
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class SpecialSequenceRuleTest {
    private fun simpleRuleForTests(): SpecialSequenceRule {
        return SpecialSequenceRule.create(
            "match \"a\"",
            { s: String? -> if (s!!.startsWith("a")) "a" else null }) as SpecialSequenceRule
    }

    private fun simple_a(): SpecialSequenceRule {
        return simpleRuleForTests()
    }

    private fun simple_b(): SpecialSequenceRule {
        return SpecialSequenceRule.create(
            "match \"b\"",
            { s: String? -> if (s!!.startsWith("b")) "b" else null }) as SpecialSequenceRule
    }

    @Test
    fun getDescription() {
    }

    @Test
    fun getFunction() {
        val f = { s: String -> if (s.startsWith("a")) "a" else null }
        Assertions.assertEquals(
            f,
            (SpecialSequenceRule.create("match \"a\"", f) as SpecialSequenceRule).function
        )
    }

    @Test
    fun create() {
        val f = { s: String -> if (s.startsWith("a")) "a" else null }

        Assertions.assertEquals(
            SpecialSequenceRule.create("match \"a\"", f),
            SpecialSequenceRule.create("match \"a\"", f)
        )

        val f2 = { s: String -> if (!s.startsWith("a")) null else "a" }

        Assertions.assertNotEquals(
            SpecialSequenceRule.create("match \"a\"", f),
            SpecialSequenceRule.create("match \"a\"", f2)
        )
    }

    @Test
    fun parse() {
        val p = parser(Gr.grammar("S", ConcatRule.create(listOf(simple_a(), simple_b()))))
        Assertions.assertEquals(PT.create("S", "a", "b"), p.parse("ab"))
    }

    @Test
    fun parseError() {
        val p = parser(Gr.grammar("S", ConcatRule.create(listOf(simple_a(), simple_b()))))
        Assertions.assertTrue(p.parse("b").isFailure)
    }

    @Test
    fun fullParseSingle() {
        val p = parser(Gr.grammar("S", simple_a()))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
    }

    @Test
    fun fullParseConcat() {
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(listOf(simple_a(), simple_b()))
            )
        )
        Assertions.assertEquals(PT.create("S", "a", "b"), p.parse("ab"))
    }

    @Test
    fun fullParseError() {
        val p = parser(Gr.grammar("S", simple_a()))
        Assertions.assertTrue(p.parse("").isFailure)
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
            Gr.grammar("S", simple_a())
        )
        Assertions.assertEquals(PT.create("S", "a"), pDefault.parse("a"))

        // With explicit show, also not hidden
        val pShow = parser(
            Gr.grammar("S", simple_a().withHideTag(false))
        )
        Assertions.assertEquals(PT.create("S", "a"), pShow.parse("a"))

        // With explicit hide
        val pHide = parser(
            Gr.grammar("S", simple_a().withHideTag(true))
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
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simple_a()
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A", "a")), pDefault.parse("a"))
    }

    @Test
    fun withReductionMergeUp() {
        // Merge upwards
        val pMerge = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simple_a().withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pMerge.parse("a"))
    }

    @Test
    fun withReductionTaggedTree() {
        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = parser(
            Gr.grammarS(
                "S",
                "S", simple_a().withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B", "a"), pMerge2.parse("a"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simple_a().withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B", "a")), pMerge3.parse("a"))
    }

    @Test
    fun withReductionNotDirectRhs() {
        // Add parse tree from a rule which doesn't appear in the grammar.
        val pMerge4 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", ConcatRule.create(
                    listOf(
                        simple_a().withReduction(ReductionType.taggedTree(sym("B"))),
                        StringTerm.createDirect("b", false)
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("B", "a"), "b")),
            pMerge4.parse("ab")
        )
    }
}