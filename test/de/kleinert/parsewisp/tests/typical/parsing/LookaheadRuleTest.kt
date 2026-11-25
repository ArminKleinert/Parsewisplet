package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.List
import java.util.regex.Pattern

internal class LookaheadRuleTest {
    @Test
    fun create() {
        val laEps = LookaheadRule.create(EpsilonTerm.getDefault())
        Assertions.assertTrue(laEps is EpsilonTerm || laEps is LookaheadRule)

        Assertions.assertEquals(
            LookaheadRule.create(StringTerm.create("a", false)),
            LookaheadRule.create(StringTerm.create("a", false))
        )
    }

    @Test
    fun parse() {
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    List.of<Rule?>(
                        LookaheadRule.create(RegexTerm.create(Pattern.compile("[a-zA-Z]"))),  // Look for any letter
                        LookaheadRule.create(RegexTerm.create(Pattern.compile("[a-fA-F0-9]"))),  // Look for any hex digit
                        RegexTerm.create(Pattern.compile("[\\x00-\\xFF]+")) // Any 8-bit ascii
                    )
                )
            )
        )
        for (c in 0.toChar()..255.toChar()) {
            val text = "" + c
            if ((c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                Assertions.assertEquals(PT.create("S", text), p.parse(text))
            } else {
                Assertions.assertTrue(p.parse(text).isFailure())
            }
        }
    }

    @Test
    fun parseDoubledLook() {
        // This parser includes a doubled lookahead for the terminal "a".
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    List.of<Rule?>(
                        LookaheadRule.create(StringTerm.create("a", false)),
                        LookaheadRule.create(StringTerm.create("a", false)),
                        RegexTerm.create(Pattern.compile("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertTrue(p.parse("b").isFailure()) // Nope. Was looking for "a".
    }

    @Test
    fun parseError() {
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    List.of<Rule>(
                        LookaheadRule.create(StringTerm.create("a", false)),
                        RegexTerm.create(Pattern.compile("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertTrue(p.parse("b").isFailure()) // Nope. Was looking for "a".
    }

    @Test
    fun parseContradictoryLookahead() {
        // This parser has lookaheads for two rules that can not be there simultaneously. It looks for both "a" and "b".
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    List.of<Rule?>(
                        LookaheadRule.create(StringTerm.create("a", false)),
                        LookaheadRule.create(StringTerm.create("b", false)),
                        RegexTerm.create(Pattern.compile("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertTrue(p.parse("a").isFailure()) // Nope. Was looking for "b".
        Assertions.assertTrue(p.parse("b").isFailure()) // Nope. Was looking for "a".
        Assertions.assertTrue(p.parse("c").isFailure()) // Fits neither lookahead.
    }

    @Test
    fun fullParseSingle() {
        Assertions.assertEquals(
            PT.create("S"),
            parser(
                Gr.grammar(
                    "S", LookaheadRule.create(EpsilonTerm.getDefault())
                )
            ).parse("")
        )
        Assertions.assertEquals(
            PT.create("S"),
            parser(
                Gr.grammar(
                    "S", LookaheadRule.create(EOFTerm.getDefault())
                )
            ).parse("")
        )
    }

    @Test
    fun fullParseConcat() {
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    List.of<Rule>(
                        StringTerm.create("a", false),
                        LookaheadRule.create(EpsilonTerm.getDefault())
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
    }

    @Test
    fun fullParseError() {
        val p = parser(
            Gr.grammar(
                "S", LookaheadRule.create(StringTerm.create("a", false))
            )
        )
        Assertions.assertTrue(p.parse("a").isFailure()) // Nope. Was looking for "b".
        Assertions.assertTrue(p.parse("b").isFailure()) // Nope. Was looking for "a".
        Assertions.assertTrue(p.parse("c").isFailure()) // Fits neither lookahead.
    }

    @Test
    fun getRule() {
        val i = StringTerm.create("a", false)
        val r = LookaheadRule.create(i) as LookaheadRule
        Assertions.assertEquals(i, r.getRule())
    }

    @Test
    fun withRule() {
        val i = StringTerm.create("a", false)
        val r = LookaheadRule.create(i) as LookaheadRule

        // Expected
        Assertions.assertEquals(i, r.getRule())

        val ni = StringTerm.createDirect("c", false)
        val nr = r.withRule(ni)

        // The new rule.
        Assertions.assertEquals(ni, nr.getRule())

        // Original left untouched.
        Assertions.assertEquals(i, r.getRule())
    }

    @Test
    fun withRulePreservesHideAndRed() {
        val i = StringTerm.create("a", false)
        val r = LookaheadRule
            .create(i)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(sym("A"))) as LookaheadRule

        // As expected.
        Assertions.assertEquals(i, r.getRule())

        // Create a new rule, check whether hide and reduction are still unchanged.
        val ni = StringTerm.create("b", false)
        val nr = r.withRule(ni)
        Assertions.assertEquals(ni, nr.getRule())
        Assertions.assertEquals(r.isHiddenInResult(), nr.isHiddenInResult())
        Assertions.assertEquals(r.getReduction(), nr.getReduction())

        // Original left untouched.
        Assertions.assertEquals(i, r.getRule())
    }

    @Test
    fun withHideTag() {
        val r = LookaheadRule.create(StringTerm.create("a", false)) as LookaheadRule

        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(r, r.withHideTag(false))
        Assertions.assertEquals(r, r.includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(r.withHideTag(false), r.withHideTag(false))
        Assertions.assertEquals(r.withHideTag(true), r.withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(r.withHideTag(false).isHiddenInResult())
        Assertions.assertFalse(r.includeInResult().isHiddenInResult())
        Assertions.assertTrue(r.withHideTag(true).isHiddenInResult())
        Assertions.assertTrue(r.hideInResult().isHiddenInResult())

        // Alternative names give equivalent output
        Assertions.assertEquals(r.withHideTag(false), r.includeInResult())
        Assertions.assertEquals(r.withHideTag(true), r.hideInResult())
    }

    @Test
    fun withHideTagParse() {
        val r = LookaheadRule.create(StringTerm.create("a", false)) as LookaheadRule

        // By default, not hidden in output, but this type adds nothing to the tree
        val pDefault = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    List.of<Rule>(
                        r,
                        RegexTerm.create(Pattern.compile("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pDefault.parse("a"))

        // With explicit show, also not hidden, but this type adds nothing to the tree
        val pShow = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    List.of<Rule>(
                        r.withHideTag(false),
                        RegexTerm.create(Pattern.compile("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pShow.parse("a"))

        // With explicit hide
        val pHide = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    List.of<Rule>(
                        r.withHideTag(true),
                        RegexTerm.create(Pattern.compile("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pHide.parse("a"))
    }

    @Test
    fun withReduction() {
        val r = LookaheadRule.create(StringTerm.create("a", false)) as LookaheadRule

        // Default.
        Assertions.assertEquals(
            r,
            r.withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", ConcatRule.create(
                    List.of<Rule>(
                        r,
                        RegexTerm.create(Pattern.compile("[a-z]"))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A", "a")), pDefault.parse("a"))
        Assertions.assertTrue(pDefault.parse("b").isFailure())
    }

    @Test
    fun withReductionMergeUp() {
        // Merge upwards
        val pMerge = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", LookaheadRule.create(EpsilonTerm.getDefault())
                    .withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S"), pMerge.parse(""))
    }

    @Test
    fun withReductionTaggedTree() {
        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = parser(
            Gr.grammarS(
                "S",
                "S", LookaheadRule.create(EpsilonTerm.getDefault())
                    .withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B"), pMerge2.parse(""))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", LookaheadRule.create(EpsilonTerm.getDefault())
                    .withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B")), pMerge3.parse(""))
    }

    @Test
    fun withReductionNotDirectRhs() {
        val r = LookaheadRule.create(StringTerm.create("a", false)) as LookaheadRule

        // Add parse tree from a rule which doesn't appear in the grammar.
        val pMerge4 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", ConcatRule.create(
                    List.of<Rule>(
                        r.withReduction(ReductionType.taggedTree(sym("B"))),
                        RegexTerm.create(Pattern.compile("[a-z]"))
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