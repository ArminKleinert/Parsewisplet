package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class OptionalRuleTest {
    private fun a_or_nay(): OptionalRule {
        return OptionalRule.create(StringTerm.createDirect("a", false)) as OptionalRule
    }

    private fun string_a(): Rule {
        return StringTerm.createDirect("a", false)
    }

    @Test
    fun create() {
        // If the rule is a EpsilonTerm, the #create(...) method is free to create something other than a OptionalRule,
        // but the same input still must mean equivalent output.
        Assertions.assertEquals(
            OptionalRule.create(EpsilonTerm.default),
            OptionalRule.create(EpsilonTerm.default)
        )

        // If the rule is a OptionalRule, the #create(...) method is free to not create a new rule,
        // but the same input still must mean equivalent output.
        Assertions.assertEquals(
            OptionalRule.create(OptionalRule.create(StringTerm.createDirect("a", false))),
            OptionalRule.create(OptionalRule.create(StringTerm.createDirect("a", false)))
        )

        // As expected.
        Assertions.assertEquals(
            OptionalRule.create(StringTerm.createDirect("a", false)),
            OptionalRule.create(StringTerm.createDirect("a", false))
        )
    }

    @Test
    fun parse() {
        val p = Parsewisp.parser(Gr.grammar("S", ConcatRule.create(listOf(a_or_nay(), string_a()))))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))

        val pN = Parsewisp.parser(Gr.grammar("S", ConcatRule.create(listOf(string_a(), a_or_nay(), string_a()))))
        Assertions.assertEquals(PT.create("S", "a", "a"), pN.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), pN.parse("aaa"))

        val pM = Parsewisp.parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    listOf(
                        a_or_nay(),
                        a_or_nay(),
                        a_or_nay(),
                        a_or_nay(),
                        a_or_nay(),
                        string_a()
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a", "a"), pM.parse("aa"))
    }

    // Never happens. Can only fail on full parse.
    // @Test void parseError() {}
    @Test
    fun fullParseSingle() {
        val p = Parsewisp.parser(Gr.grammar("S", a_or_nay()))
        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
    }

    @Test
    fun fullParseConcat() {
        val pN = Parsewisp.parser(Gr.grammar("S", ConcatRule.create(listOf(string_a(), a_or_nay(), a_or_nay()))))
        Assertions.assertEquals(PT.create("S", "a"), pN.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), pN.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), pN.parse("aaa"))

        val pM = Parsewisp.parser(Gr.grammar("S", ConcatRule.create(listOf(a_or_nay(), a_or_nay(), a_or_nay()))))
        Assertions.assertEquals(PT.create("S", "a"), pM.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), pM.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), pM.parse("aaa"))
    }

    @Test
    fun fullParseError() {
        val p = Parsewisp.parser(Gr.grammar("S", a_or_nay()))
        Assertions.assertTrue(p.parse("b").isFailure)
    }

    @Test
    fun getRule() {
        val r = OptionalRule.create(string_a()) as OptionalRule
        Assertions.assertEquals(string_a(), r.rule)
    }

    @Test
    fun withRule() {
        val i = StringTerm.create("a", false)
        val r = OptionalRule
            .create(i)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(Sym.sym("A"))) as OptionalRule

        // As expected.
        Assertions.assertEquals(i, r.rule)

        // Create a new rule, check whether hide and reduction are still unchanged.
        val ni = StringTerm.create("b", false)
        val nr = r.withRule(ni)
        Assertions.assertEquals(ni, nr.rule)

        // Original left untouched.
        Assertions.assertEquals(i, r.rule)
    }

    @Test
    fun withRulePreservesHideAndRed() {
        val i = StringTerm.create("a", false)
        val r = OptionalRule
            .create(i)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(Sym.sym("A"))) as OptionalRule

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
        Assertions.assertEquals(a_or_nay(), a_or_nay().withHideTag(false))
        Assertions.assertEquals(a_or_nay(), a_or_nay().includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(a_or_nay().withHideTag(false), a_or_nay().withHideTag(false))
        Assertions.assertEquals(a_or_nay().withHideTag(true), a_or_nay().withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(a_or_nay().withHideTag(false).isHiddenInResult1())
        Assertions.assertFalse(a_or_nay().includeInResult().isHiddenInResult1())
        Assertions.assertTrue(a_or_nay().withHideTag(true).isHiddenInResult1())
        Assertions.assertTrue(a_or_nay().hideInResult().isHiddenInResult1())

        // Alternative names give equivalent output
        Assertions.assertEquals(a_or_nay().withHideTag(false), a_or_nay().includeInResult())
        Assertions.assertEquals(a_or_nay().withHideTag(true), a_or_nay().hideInResult())
    }

    @Test
    fun withHideTagParse() {
        // By default, not hidden in output
        val pDefault = Parsewisp.parser(
            Gr.grammar("S", a_or_nay())
        )
        Assertions.assertEquals(PT.create("S"), pDefault.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), pDefault.parse("a"))

        // With explicit show, also not hidden
        val pShow = Parsewisp.parser(
            Gr.grammar("S", a_or_nay().withHideTag(false))
        )
        Assertions.assertEquals(PT.create("S"), pShow.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), pShow.parse("a"))

        // With explicit hide
        val pHide = Parsewisp.parser(
            Gr.grammar("S", a_or_nay().withHideTag(true))
        )
        Assertions.assertEquals(PT.create("S"), pHide.parse(""))
        Assertions.assertEquals(PT.create("S"), pHide.parse("a"))
    }

    @Test
    fun withReduction() {
        // Default.
        Assertions.assertEquals(
            a_or_nay(),
            a_or_nay().withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", a_or_nay()
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A")), pDefault.parse(""))
        Assertions.assertEquals(PT.create("S", PT.create("A", "a")), pDefault.parse("a"))
    }

    @Test
    fun withReductionMergeUp() {
        // Merge upwards
        val pMerge = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", a_or_nay().withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S"), pMerge.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), pMerge.parse("a"))
    }

    @Test
    fun withReductionTaggedTree() {
        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", a_or_nay().withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B"), pMerge2.parse(""))
        Assertions.assertEquals(PT.create("B", "a"), pMerge2.parse("a"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", a_or_nay().withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B")), pMerge3.parse(""))
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
                        a_or_nay().withReduction(ReductionType.taggedTree(Sym.sym("B"))),
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