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

internal class EpsilonTermTest {
    private fun string_a(): Rule {
        return StringTerm.createDirect("a", false)
    }

    @Test
    fun getDefault() {
        Assertions.assertEquals(EpsilonTerm.getDefault(), EpsilonTerm.getDefault())
    }

    @Test
    fun parse() {
        val p1 = parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(List.of<Rule>(EpsilonTerm.getDefault(), string_a()))
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), p1.parse("a"))

        val p2 = parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(List.of<Rule>(EpsilonTerm.getDefault(), EpsilonTerm.getDefault()))
            )
        )
        Assertions.assertEquals(PT.create("S"), p2.parse(""))
    }

    // Never happens. Eps can only fail on full parse.
    // @Test void parseError() {}
    @Test
    fun fullParseSingle() {
        val p = parser(Gr.grammar("S", EpsilonTerm.getDefault()))
        Assertions.assertEquals(PT.create("S"), p.parse(""))
    }

    @Test
    fun fullParseConcat() {
        val pE = parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(List.of<Rule>(EpsilonTerm.getDefault(), EpsilonTerm.getDefault()))
            )
        )
        Assertions.assertEquals(PT.create("S"), pE.parse(""))

        val pA = parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(List.of<Rule>(string_a(), EpsilonTerm.getDefault()))
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pA.parse("a"))
    }

    @Test
    fun fullParseError() {
        val p = parser(Gr.grammar("S", EpsilonTerm.getDefault()))
        Assertions.assertTrue(p.parse("a").isFailure())
    }

    @Test
    fun withHideTag() {
        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(EpsilonTerm.getDefault(), EpsilonTerm.getDefault().withHideTag(false))
        Assertions.assertEquals(EpsilonTerm.getDefault(), EpsilonTerm.getDefault().includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(
            EpsilonTerm.getDefault().withHideTag(false),
            EpsilonTerm.getDefault().withHideTag(false)
        )
        Assertions.assertEquals(EpsilonTerm.getDefault().withHideTag(true), EpsilonTerm.getDefault().withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(EpsilonTerm.getDefault().withHideTag(false).isHiddenInResult())
        Assertions.assertFalse(EpsilonTerm.getDefault().includeInResult().isHiddenInResult())
        Assertions.assertTrue(EpsilonTerm.getDefault().withHideTag(true).isHiddenInResult())
        Assertions.assertTrue(EpsilonTerm.getDefault().hideInResult().isHiddenInResult())

        // Alternative names give equivalent output
        Assertions.assertEquals(EpsilonTerm.getDefault().withHideTag(false), EpsilonTerm.getDefault().includeInResult())
        Assertions.assertEquals(EpsilonTerm.getDefault().withHideTag(true), EpsilonTerm.getDefault().hideInResult())
    }

    @Test
    fun withHideTagParse() {
        // By default, not hidden in output, but this type adds nothing to the tree
        val pDefault = parser(Gr.grammar("S", EpsilonTerm.getDefault()))
        Assertions.assertEquals(PT.create("S"), pDefault.parse(""))

        // With explicit show, also not hidden, but this type adds nothing to the tree
        val pShow = parser(Gr.grammar("S", EpsilonTerm.getDefault().withHideTag(false)))
        Assertions.assertEquals(PT.create("S"), pShow.parse(""))

        // With explicit hide
        val pHide = parser(Gr.grammar("S", EpsilonTerm.getDefault().withHideTag(true)))
        Assertions.assertEquals(PT.create("S"), pHide.parse(""))
    }

    @Test
    fun withReduction() {
        // Default.
        Assertions.assertEquals(
            EpsilonTerm.getDefault(),
            EpsilonTerm.getDefault().withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", EpsilonTerm.getDefault()
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A")), pDefault.parse(""))
    }

    @Test
    fun withReductionMergeUp() {
        // Merge upwards
        val pMerge = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", EpsilonTerm.getDefault().withReduction(ReductionType.merging())
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
                "S", EpsilonTerm.getDefault().withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B"), pMerge2.parse(""))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", EpsilonTerm.getDefault().withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B")), pMerge3.parse(""))
    }

    @Test
    fun withReductionNotDirectRhs() {
        // Add parse tree from a rule which doesn't appear in the grammar.
        val pMerge4 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", ConcatRule.create(
                    List.of<Rule>(
                        EpsilonTerm.getDefault().withReduction(ReductionType.taggedTree(sym("B"))),
                        EpsilonTerm.getDefault()
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("B"))),
            pMerge4.parse("")
        )
    }
}