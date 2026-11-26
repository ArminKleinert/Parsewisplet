package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class NegativeLookaheadRuleTest {
    @Test
    fun create() {
        val laEps = NegativeLookaheadRule.create(EpsilonTerm.default)
        Assertions.assertTrue((laEps is EpsilonTerm) || (laEps is NegativeLookaheadRule))

        Assertions.assertEquals(
            NegativeLookaheadRule.create(StringTerm.create("a", false)),
            NegativeLookaheadRule.create(StringTerm.create("a", false))
        )
    }

    @Test
    fun parse() {
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        NegativeLookaheadRule.create(RegexTerm.create(Regex("[a-zA-Z]"))),  // Look for any letter
                        NegativeLookaheadRule.create(RegexTerm.create(Regex("[a-fA-F0-9]"))),  // Look for any hex digit
                        RegexTerm.create(Regex("[\\x00-\\xFF]+")) // Any 8-bit ascii
                    )
                )
            )
        )
        for (c in 0.toChar()..255.toChar()) {
            val text = "" + c
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                Assertions.assertTrue(p.parse(text).isFailure)
            } else {
                Assertions.assertEquals(PT.create("S", text), p.parse(text))
            }
        }
    }

    @Test
    fun parseDoubledLook() {
        // This parser includes a doubled lookahead for the terminal "a".
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        NegativeLookaheadRule.create(StringTerm.create("b", false)),
                        NegativeLookaheadRule.create(StringTerm.create("b", false)),
                        RegexTerm.create(Regex("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertTrue(p.parse("b").isFailure)
        Assertions.assertEquals(PT.create("S", "c"), p.parse("c"))
    }

    @Test
    fun parseError() {
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        NegativeLookaheadRule.create(StringTerm.create("a", false)),
                        RegexTerm.create(Regex("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertTrue(p.parse("a").isFailure)
    }

    @Test
    fun fullParseSingle() {
        Assertions.assertEquals(
            PT.create("S"),
            Parsewisp.parser(
                Gr.grammar(
                    "S", NegativeLookaheadRule.create(StringTerm.create("a", false))
                )
            ).parse("")
        )
    }

    @Test
    fun fullParseConcat() {
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        StringTerm.create("a", false),
                        NegativeLookaheadRule.create(StringTerm.create("a", false))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
    }

    @Test
    fun fullParseError() {
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", NegativeLookaheadRule.create(EpsilonTerm.default)
            )
        )
        Assertions.assertTrue(p.parse("a").isFailure) // Nope, expected empty.
        Assertions.assertTrue(p.parse("").isFailure) // Nope, expected not epsilon.
    }

    @Test
    fun getRule() {
        val i = StringTerm.create("a", false)
        val r = NegativeLookaheadRule.create(i) as NegativeLookaheadRule
        Assertions.assertEquals(i, r.rule)
    }

    @Test
    fun withRule() {
        val i = StringTerm.create("a", false)
        val r = NegativeLookaheadRule.create(i) as NegativeLookaheadRule

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
        val r = NegativeLookaheadRule
            .create(i)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(Sym.sym("A"))) as NegativeLookaheadRule

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
        val r = NegativeLookaheadRule.create(StringTerm.create("a", false)) as NegativeLookaheadRule

        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(r, r.withHideTag(false))
        Assertions.assertEquals(r, r.includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(r.withHideTag(false), r.withHideTag(false))
        Assertions.assertEquals(r.withHideTag(true), r.withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(r.withHideTag(false).isHiddenInResult)
        Assertions.assertFalse(r.includeInResult().isHiddenInResult)
        Assertions.assertTrue(r.withHideTag(true).isHiddenInResult)
        Assertions.assertTrue(r.hideInResult().isHiddenInResult)

        // Alternative names give equivalent output
        Assertions.assertEquals(r.withHideTag(false), r.includeInResult())
        Assertions.assertEquals(r.withHideTag(true), r.hideInResult())
    }

    @Test
    fun withHideTagParse() {
        val r = NegativeLookaheadRule.create(StringTerm.create("b", false)) as NegativeLookaheadRule

        // By default, not hidden in output, but this type adds nothing to the tree
        val pDefault = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        r,
                        RegexTerm.create(Regex("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pDefault.parse("a"))
        Assertions.assertTrue(pDefault.parse("b").isFailure)

        // With explicit show, also not hidden, but this type adds nothing to the tree
        val pShow = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        r.withHideTag(false),
                        RegexTerm.create(Regex("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pShow.parse("a"))
        Assertions.assertTrue(pShow.parse("b").isFailure)

        // With explicit hide
        val pHide = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        r.withHideTag(true),
                        RegexTerm.create(Regex("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pHide.parse("a"))
        Assertions.assertTrue(pHide.parse("b").isFailure)
    }

    @Test
    fun withReduction() {
        val r = NegativeLookaheadRule.create(StringTerm.create("b", false)) as NegativeLookaheadRule

        // Default.
        Assertions.assertEquals(
            r,
            r.withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", ConcatRule.create(
                    listOf(
                        r,
                        RegexTerm.create(Regex("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A", "a")), pDefault.parse("a"))
        Assertions.assertTrue(pDefault.parse("b").isFailure)
    }

    @Test
    fun withReductionMergeUp() {
        // Merge upwards
        val pMerge = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", NegativeLookaheadRule.create(StringTerm.create("b", false))
                    .withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S"), pMerge.parse(""))
    }

    @Test
    fun withReductionTaggedTree() {
        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NegativeLookaheadRule.create(StringTerm.create("b", false))
                    .withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B"), pMerge2.parse(""))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", NegativeLookaheadRule.create(StringTerm.create("b", false))
                    .withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B")), pMerge3.parse(""))
    }

    @Test
    fun withReductionNotDirectRhs() {
        val r = NegativeLookaheadRule.create(StringTerm.create("b", false)) as NegativeLookaheadRule

        // Add parse tree from a rule which doesn't appear in the grammar.
        val pMerge4 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", ConcatRule.create(
                    listOf(
                        r.withReduction(ReductionType.taggedTree(Sym.sym("B"))),
                        RegexTerm.create(Regex("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("B"), "a")),
            pMerge4.parse("a")
        )
    }
}