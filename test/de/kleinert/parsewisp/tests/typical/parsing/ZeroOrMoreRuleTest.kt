package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ZeroOrMoreRuleTest {
    private fun rep_a(): ZeroOrMoreRule {
        return ZeroOrMoreRule.create(StringTerm.createDirect("a", false)) as ZeroOrMoreRule
    }

    private fun string_a(): Rule {
        return StringTerm.createDirect("a", false)
    }

    @Test
    fun create() {
        // If the rule is a EpsilonTerm, the #create(...) method is free to create something other than a ZeroOrMoreRule,
        // but the same input still must mean equivalent output.
        Assertions.assertEquals(
            ZeroOrMoreRule.create(EpsilonTerm.default),
            ZeroOrMoreRule.create(EpsilonTerm.default)
        )

        // If the rule is a ZeroOrMoreRule, the #create(...) method is free to not create a new rule,
        // but the same input still must mean equivalent output.
        Assertions.assertEquals(
            ZeroOrMoreRule.create(ZeroOrMoreRule.create(StringTerm.createDirect("a", false))),
            ZeroOrMoreRule.create(ZeroOrMoreRule.create(StringTerm.createDirect("a", false)))
        )

        // As expected.
        Assertions.assertEquals(
            ZeroOrMoreRule.create(StringTerm.createDirect("a", false)),
            ZeroOrMoreRule.create(StringTerm.createDirect("a", false))
        )
    }

    @Test
    fun parse() {
        val p = parser(Gr.grammar("S", ConcatRule.create(listOf(rep_a(), string_a()))))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa"))

        val pN = parser(Gr.grammar("S", ConcatRule.create(listOf(string_a(), rep_a(), string_a()))))
        Assertions.assertEquals(PT.create("S", "a", "a"), pN.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), pN.parse("aaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a", "a"), pN.parse("aaaaa"))

        val pM = parser(
            Gr.grammar(
                "S", ConcatRule.create(listOf(rep_a(), rep_a(), rep_a(), rep_a(), rep_a(), string_a()))
            )
        )
        Assertions.assertEquals(PT.create("S", "a", "a"), pM.parse("aa"))
    }

    // Never happens. Can only fail on full parse.
    // @Test void parseError() {}
    @Test
    fun fullParseSingle() {
        val p = parser(Gr.grammar("S", rep_a()))
        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa"))
    }

    @Test
    fun fullParseConcat() {
        val pN = parser(Gr.grammar("S", ConcatRule.create(listOf(string_a(), rep_a()))))
        Assertions.assertEquals(PT.create("S", "a"), pN.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), pN.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), pN.parse("aaa"))

        val pM = parser(Gr.grammar("S", ConcatRule.create(listOf(rep_a(), rep_a()))))
        Assertions.assertEquals(PT.create("S"), pM.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), pM.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), pM.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), pM.parse("aaa"))
    }

    @Test
    fun fullParseError() {
        val p = parser(Gr.grammar("S", rep_a()))
        Assertions.assertTrue(p.parse("b").isFailure)
    }

    @Test
    fun getRule() {
        val r = ZeroOrMoreRule.create(string_a()) as ZeroOrMoreRule
        Assertions.assertEquals(string_a(), r.rule)
    }

    @Test
    fun withRule() {
        val i = string_a()
        val r = ZeroOrMoreRule.create(i) as ZeroOrMoreRule

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
        val r = ZeroOrMoreRule
            .create(i)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(sym("A"))) as ZeroOrMoreRule

        // As expected.
        Assertions.assertEquals(i, r.rule)

        // Create a new rule, check whether hide and reduction are still unchanged.
        val ni = StringTerm.create("b", false)
        val nr = r.withRule(ni)
        Assertions.assertEquals(ni, nr.rule)
        Assertions.assertEquals(r.isHiddenInResult1(), nr.isHiddenInResult1())
        Assertions.assertEquals(r.getReduction1(), nr.getReduction1())

        // Original left untouched.
        Assertions.assertEquals(i, r.rule)
    }

    @Test
    fun withHideTag() {
        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(rep_a(), rep_a().withHideTag(false))
        Assertions.assertEquals(rep_a(), rep_a().includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(rep_a().withHideTag(false), rep_a().withHideTag(false))
        Assertions.assertEquals(rep_a().withHideTag(true), rep_a().withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(rep_a().withHideTag(false).isHiddenInResult1())
        Assertions.assertFalse(rep_a().includeInResult().isHiddenInResult1())
        Assertions.assertTrue(rep_a().withHideTag(true).isHiddenInResult1())
        Assertions.assertTrue(rep_a().hideInResult().isHiddenInResult1())

        // Alternative names give equivalent output
        Assertions.assertEquals(rep_a().withHideTag(false), rep_a().includeInResult())
        Assertions.assertEquals(rep_a().withHideTag(true), rep_a().hideInResult())
    }

    @Test
    fun withHideTagParse() {
        // By default, not hidden in output
        val pDefault = parser(
            Gr.grammar("S", rep_a())
        )
        Assertions.assertEquals(PT.create("S"), pDefault.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), pDefault.parse("a"))

        // With explicit show, also not hidden
        val pShow = parser(
            Gr.grammar("S", rep_a().withHideTag(false))
        )
        Assertions.assertEquals(PT.create("S"), pShow.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), pShow.parse("a"))

        // With explicit hide
        val pHide = parser(
            Gr.grammar("S", rep_a().withHideTag(true))
        )
        Assertions.assertEquals(PT.create("S"), pHide.parse(""))
        Assertions.assertEquals(PT.create("S"), pHide.parse("a"))
    }

    @Test
    fun withReduction() {
        // Default.
        Assertions.assertEquals(
            rep_a(),
            rep_a().withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", rep_a()
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A")), pDefault.parse(""))
        Assertions.assertEquals(PT.create("S", PT.create("A", "a")), pDefault.parse("a"))
    }

    @Test
    fun withReductionMergeUp() {
        // Merge upwards
        val pMerge = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", rep_a().withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S"), pMerge.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), pMerge.parse("a"))
    }

    @Test
    fun withReductionTaggedTree() {
        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = parser(
            Gr.grammarS(
                "S",
                "S", rep_a().withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B"), pMerge2.parse(""))
        Assertions.assertEquals(PT.create("B", "a"), pMerge2.parse("a"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", rep_a().withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B")), pMerge3.parse(""))
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
                        rep_a().withReduction(ReductionType.taggedTree(sym("B"))),
                        OptionalRule.create(StringTerm.create("b", false))
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("B"), "b")),
            pMerge4.parse("b")
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("B", "a"), "b")),
            pMerge4.parse("ab")
        )
    }
}