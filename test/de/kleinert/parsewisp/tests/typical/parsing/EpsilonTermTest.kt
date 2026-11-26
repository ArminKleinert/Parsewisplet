package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class EpsilonTermTest {
    private fun string_a(): Rule {
        return StringTerm.createDirect("a", false)
    }

    @Test
    fun getDefault() {
        Assertions.assertEquals(EpsilonTerm.default, EpsilonTerm.default)
    }

    @Test
    fun parse() {
        val p1 = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(listOf(EpsilonTerm.default, string_a()))
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), p1.parse("a"))

        val p2 = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(listOf(EpsilonTerm.default, EpsilonTerm.default))
            )
        )
        Assertions.assertEquals(PT.create("S"), p2.parse(""))
    }

    // Never happens. Eps can only fail on full parse.
    // @Test void parseError() {}
    @Test
    fun fullParseSingle() {
        val p = Parsewisp.parser(Gr.grammar("S", EpsilonTerm.default))
        Assertions.assertEquals(PT.create("S"), p.parse(""))
    }

    @Test
    fun fullParseConcat() {
        val pE = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(listOf(EpsilonTerm.default, EpsilonTerm.default))
            )
        )
        Assertions.assertEquals(PT.create("S"), pE.parse(""))

        val pA = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(listOf(string_a(), EpsilonTerm.default))
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pA.parse("a"))
    }

    @Test
    fun fullParseError() {
        val p = Parsewisp.parser(Gr.grammar("S", EpsilonTerm.default))
        Assertions.assertTrue(p.parse("a").isFailure)
    }

    @Test
    fun withHideTag() {
        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(EpsilonTerm.default, EpsilonTerm.default.withHideTag(false))
        Assertions.assertEquals(EpsilonTerm.default, EpsilonTerm.default.includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(
            EpsilonTerm.default.withHideTag(false),
            EpsilonTerm.default.withHideTag(false)
        )
        Assertions.assertEquals(EpsilonTerm.default.withHideTag(true), EpsilonTerm.default.withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(EpsilonTerm.default.withHideTag(false).isHiddenInResult)
        Assertions.assertFalse(EpsilonTerm.default.includeInResult().isHiddenInResult)
        Assertions.assertTrue(EpsilonTerm.default.withHideTag(true).isHiddenInResult)
        Assertions.assertTrue(EpsilonTerm.default.hideInResult().isHiddenInResult)

        // Alternative names give equivalent output
        Assertions.assertEquals(EpsilonTerm.default.withHideTag(false), EpsilonTerm.default.includeInResult())
        Assertions.assertEquals(EpsilonTerm.default.withHideTag(true), EpsilonTerm.default.hideInResult())
    }

    @Test
    fun withHideTagParse() {
        // By default, not hidden in output, but this type adds nothing to the tree
        val pDefault = Parsewisp.parser(Gr.grammar("S", EpsilonTerm.default))
        Assertions.assertEquals(PT.create("S"), pDefault.parse(""))

        // With explicit show, also not hidden, but this type adds nothing to the tree
        val pShow = Parsewisp.parser(Gr.grammar("S", EpsilonTerm.default.withHideTag(false)))
        Assertions.assertEquals(PT.create("S"), pShow.parse(""))

        // With explicit hide
        val pHide = Parsewisp.parser(Gr.grammar("S", EpsilonTerm.default.withHideTag(true)))
        Assertions.assertEquals(PT.create("S"), pHide.parse(""))
    }

    @Test
    fun withReduction() {
        // Default.
        Assertions.assertEquals(
            EpsilonTerm.default,
            EpsilonTerm.default.withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", EpsilonTerm.default
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A")), pDefault.parse(""))
    }

    @Test
    fun withReductionMergeUp() {
        // Merge upwards
        val pMerge = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", EpsilonTerm.default.withReduction(ReductionType.merging())
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
                "S", EpsilonTerm.default.withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B"), pMerge2.parse(""))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", EpsilonTerm.default.withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B")), pMerge3.parse(""))
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
                        EpsilonTerm.default.withReduction(ReductionType.taggedTree(Sym.sym("B"))),
                        EpsilonTerm.default
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