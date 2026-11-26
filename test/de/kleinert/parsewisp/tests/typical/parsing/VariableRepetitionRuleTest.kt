package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class VariableRepetitionRuleTest {
    private fun rep_a14(): VariableRepetitionRule {
        return VariableRepetitionRule.create(StringTerm.createDirect("a", false), 1, 4) as VariableRepetitionRule
    }

    private fun string_a(): Rule {
        return StringTerm.createDirect("a", false)
    }

    @Test
    fun create() {
        // Epsilon as input may return Epsilon
        Assertions.assertEquals(
            VariableRepetitionRule.create(EpsilonTerm.default, 2, 4),
            VariableRepetitionRule.create(EpsilonTerm.default, 2, 4)
        )

        // Zero min and zero max -> may create epsilon
        Assertions.assertEquals(
            VariableRepetitionRule.create(string_a(), 0, 0),
            VariableRepetitionRule.create(string_a(), 0, 0)
        )

        // 1 min and 1 max -> may return the rule itself
        Assertions.assertEquals(
            VariableRepetitionRule.create(string_a(), 1, 1),
            VariableRepetitionRule.create(string_a(), 1, 1)
        )

        // The usual
        Assertions.assertEquals(
            VariableRepetitionRule.create(string_a(), 2, 4),
            VariableRepetitionRule.create(string_a(), 2, 4)
        )

        // Exact
        Assertions.assertEquals(
            VariableRepetitionRule.create(string_a(), 4, 4),
            VariableRepetitionRule.create(string_a(), 4, 4)
        )

        // Repetition of repetition may create a nested repetition or just a single one
        val r = VariableRepetitionRule.create(string_a(), 2, 4)
        Assertions.assertEquals(
            VariableRepetitionRule.create(r, 1, 3),
            VariableRepetitionRule.create(r, 1, 3)
        )

        // Negative min
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java
        ) { VariableRepetitionRule.create(string_a(), -4, 4) }

        // Negative max
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java
        ) { VariableRepetitionRule.create(string_a(), 4, -4) }

        // min > max
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java
        ) { VariableRepetitionRule.create(string_a(), 9, 1) }
    }

    @Test
    fun parse() {
        val p = Parsewisp.parser(Gr.grammar("S", ConcatRule.create(listOf(rep_a14(), string_a()))))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a"), p.parse("aaaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a", "a"), p.parse("aaaaa"))
    }

    @Test
    fun parseError() {
        val p = Parsewisp.parser(Gr.grammar("S", ConcatRule.create(listOf(rep_a14(), string_a()))))
        Assertions.assertTrue(p.parse("a").isFailure)
        Assertions.assertTrue(p.parse("aaaaaa").isFailure)
    }

    @Test
    fun fullParseSingle() {
        val p = Parsewisp.parser(Gr.grammar("S", rep_a14()))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a"), p.parse("aaaa"))
    }

    @Test
    fun fullParseConcat() {
        val p = Parsewisp.parser(Gr.grammar("S", ConcatRule.create(listOf(string_a(), rep_a14()))))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a"), p.parse("aaaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a", "a"), p.parse("aaaaa"))

        val pRR = Parsewisp.parser(Gr.grammar("S", ConcatRule.create(listOf(rep_a14(), rep_a14()))))
        Assertions.assertEquals(PT.create("S", "a", "a"), pRR.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), pRR.parse("aaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a"), pRR.parse("aaaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a", "a"), pRR.parse("aaaaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a", "a", "a"), pRR.parse("aaaaaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a", "a", "a", "a"), pRR.parse("aaaaaaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a", "a", "a", "a", "a"), pRR.parse("aaaaaaaa"))
    }

    @Test
    fun fullParseError() {
        val p = Parsewisp.parser(Gr.grammar("S", rep_a14()))
        Assertions.assertTrue(p.parse("").isFailure)
        Assertions.assertTrue(p.parse("aaaaa").isFailure)
    }

    @Test
    fun getRule() {
        val r = VariableRepetitionRule.create(string_a(), 2, 4) as VariableRepetitionRule
        Assertions.assertEquals(string_a(), r.rule)
    }

    @Test
    fun getMin() {
        Assertions.assertEquals(
            0,
            (VariableRepetitionRule.create(string_a(), 0, 1) as VariableRepetitionRule).min
        )
        Assertions.assertEquals(
            0,
            (VariableRepetitionRule.create(string_a(), 0, 2) as VariableRepetitionRule).min
        )
        Assertions.assertEquals(
            2,
            (VariableRepetitionRule.create(string_a(), 2, 4) as VariableRepetitionRule).min
        )
        Assertions.assertEquals(
            4,
            (VariableRepetitionRule.create(string_a(), 4, 4) as VariableRepetitionRule).min
        )
    }

    @Test
    fun getMax() {
        Assertions.assertEquals(
            1,
            (VariableRepetitionRule.create(string_a(), 0, 1) as VariableRepetitionRule).max
        )
        Assertions.assertEquals(
            2,
            (VariableRepetitionRule.create(string_a(), 0, 2) as VariableRepetitionRule).max
        )
        Assertions.assertEquals(
            4,
            (VariableRepetitionRule.create(string_a(), 2, 4) as VariableRepetitionRule).max
        )
        Assertions.assertEquals(
            4,
            (VariableRepetitionRule.create(string_a(), 4, 4) as VariableRepetitionRule).max
        )
    }

    @Test
    fun withRule() {
        val i = string_a()
        val r = VariableRepetitionRule.create(i, 1, 3) as VariableRepetitionRule

        // Expected
        Assertions.assertEquals(i, r.rule)

        val ni = StringTerm.createDirect("c", false)
        val nr = r.withRule(ni)

        // The new rule.
        Assertions.assertEquals(ni, nr.rule)

        // Original left untouched.
        Assertions.assertEquals(i, r.rule)
    }

    @Test
    fun withRulePreservesHideAndRed() {
        val i = StringTerm.create("a", false)
        val r = VariableRepetitionRule
            .create(i, 1, 4)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(Sym.sym("A"))) as VariableRepetitionRule

        // As expected.
        Assertions.assertEquals(i, r.rule)

        // Create a new rule, check whether hide and reduction are still unchanged.
        val ni = StringTerm.create("b", false)
        val nr = r.withRule(ni)
        Assertions.assertEquals(ni, nr.rule)
        Assertions.assertEquals(r.isHiddenInResult, nr.isHiddenInResult)
        Assertions.assertEquals(r.reduction, nr.reduction)

        // Original left untouched.
        Assertions.assertEquals(i, r.rule)
    }

    @Test
    fun withHideTag() {
        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(rep_a14(), rep_a14().withHideTag(false))
        Assertions.assertEquals(rep_a14(), rep_a14().includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(rep_a14().withHideTag(false), rep_a14().withHideTag(false))
        Assertions.assertEquals(rep_a14().withHideTag(true), rep_a14().withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(rep_a14().withHideTag(false).isHiddenInResult)
        Assertions.assertFalse(rep_a14().includeInResult().isHiddenInResult)
        Assertions.assertTrue(rep_a14().withHideTag(true).isHiddenInResult)
        Assertions.assertTrue(rep_a14().hideInResult().isHiddenInResult)

        // Alternative names give equivalent output
        Assertions.assertEquals(rep_a14().withHideTag(false), rep_a14().includeInResult())
        Assertions.assertEquals(rep_a14().withHideTag(true), rep_a14().hideInResult())
    }

    @Test
    fun withHideTagParse() {
        // By default, not hidden in output
        val pDefault = Parsewisp.parser(
            Gr.grammar("S", rep_a14())
        )
        Assertions.assertEquals(PT.create("S", "a"), pDefault.parse("a"))

        // With explicit show, also not hidden
        val pShow = Parsewisp.parser(
            Gr.grammar("S", rep_a14().withHideTag(false))
        )
        Assertions.assertEquals(PT.create("S", "a"), pShow.parse("a"))

        // With explicit hide
        val pHide = Parsewisp.parser(
            Gr.grammar("S", rep_a14().withHideTag(true))
        )
        Assertions.assertEquals(PT.create("S"), pHide.parse("a"))
    }

    @Test
    fun withReduction() {
        // Default.
        Assertions.assertEquals(
            rep_a14(),
            rep_a14().withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", rep_a14()
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A", "a")), pDefault.parse("a"))
    }

    @Test
    fun withReductionMergeUp() {
        // Merge upwards
        val pMerge = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", rep_a14().withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pMerge.parse("a"))
    }

    @Test
    fun withReductionTaggedTree() {
        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", rep_a14().withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B", "a"), pMerge2.parse("a"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", rep_a14().withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B", "a")), pMerge3.parse("a"))
    }

    @Test
    fun withReductionNotDirectRhs() {
        // Add parse tree from a rule which doesn't appear in the grammar.
        val pMerge4 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", ConcatRule.create(
                    listOf(
                        rep_a14().withReduction(ReductionType.taggedTree(Sym.sym("B"))),
                        OptionalRule.create(StringTerm.create("b", false))
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