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

internal class EOFTermTest {
    @Test
    fun getDefault() {
        Assertions.assertEquals(EOFTerm.default, EOFTerm.default)
    }

    @Test
    fun parse() {
        val p2 = parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(List.of<Rule>(EOFTerm.default, EOFTerm.default))
            )
        )
        Assertions.assertEquals(PT.create("S"), p2.parse(""))

        val p3 = parser(
            Gr.grammar(
                "S",
                ConcatRule.createDirect(
                    listOf(
                        EOFTerm.default,
                        EOFTerm.default,
                        EOFTerm.default
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S"), p3.parse(""))
    }

    @Test
    fun parseError() {
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(List.of<Rule>(EOFTerm.default, StringTerm.create("a", false)))
            )
        )
        Assertions.assertTrue(p.parse("a").isFailure)
    }

    @Test
    fun fullParseSingle() {
        val p = parser(Gr.grammar("S", EOFTerm.default))
        Assertions.assertEquals(PT.create("S"), p.parse(""))
    }

    @Test
    fun fullParseConcat() {
        val pE = parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(List.of<Rule>(EOFTerm.default, EOFTerm.default))
            )
        )
        Assertions.assertEquals(PT.create("S"), pE.parse(""))

        val pA = parser(
            Gr.grammar(
                "S", ConcatRule.createDirect(List.of<Rule>(StringTerm.create("a", false), EOFTerm.default))
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pA.parse("a"))
    }

    @Test
    fun fullParseError() {
        val p = parser(Gr.grammar("S", EOFTerm.default))
        Assertions.assertTrue(p.parse("a").isFailure)
    }

    @Test
    fun withHideTag() {
        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(EOFTerm.default, EOFTerm.default.withHideTag(false))
        Assertions.assertEquals(EOFTerm.default, EOFTerm.default.includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(EOFTerm.default.withHideTag(false), EOFTerm.default.withHideTag(false))
        Assertions.assertEquals(EOFTerm.default.withHideTag(true), EOFTerm.default.withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(EOFTerm.default.withHideTag(false).isHiddenInResult)
        Assertions.assertFalse(EOFTerm.default.includeInResult().isHiddenInResult)
        Assertions.assertTrue(EOFTerm.default.withHideTag(true).isHiddenInResult)
        Assertions.assertTrue(EOFTerm.default.hideInResult().isHiddenInResult)
    }

    @Test
    fun withHideTagParse() {
        // Alternative names give equivalent output
        Assertions.assertEquals(EOFTerm.default.withHideTag(false), EOFTerm.default.includeInResult())
        Assertions.assertEquals(EOFTerm.default.withHideTag(true), EOFTerm.default.hideInResult())

        // By default, not hidden in output, but this type adds nothing to the tree
        val pDefault = parser(Gr.grammar("S", EOFTerm.default))
        Assertions.assertEquals(PT.create("S"), pDefault.parse(""))

        // With explicit show, also not hidden, but this type adds nothing to the tree
        val pShow = parser(Gr.grammar("S", EOFTerm.default.withHideTag(false)))
        Assertions.assertEquals(PT.create("S"), pShow.parse(""))

        // With explicit hide
        val pHide = parser(Gr.grammar("S", EOFTerm.default.withHideTag(true)))
        Assertions.assertEquals(PT.create("S"), pHide.parse(""))
    }

    @Test
    fun withReduction() {
        // Default.
        Assertions.assertEquals(
            EOFTerm.default,
            EOFTerm.default.withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", EOFTerm.default
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
                "A", EOFTerm.default.withReduction(ReductionType.merging())
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
                "S", EOFTerm.default.withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B"), pMerge2.parse(""))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", EOFTerm.default.withReduction(ReductionType.taggedTree(sym("B")))
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
                        EOFTerm.default.withReduction(ReductionType.taggedTree(sym("B"))),
                        EOFTerm.default
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