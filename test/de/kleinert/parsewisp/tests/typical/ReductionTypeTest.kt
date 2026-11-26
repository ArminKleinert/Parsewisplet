package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.ConcatRule
import de.kleinert.parsewisp.parsing.NonTerminal
import de.kleinert.parsewisp.parsing.StringTerm
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ReductionTypeTest {
    @Test
    fun standardIntermediateReduction() {
        // Merge upwards
        val pMerge = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", StringTerm.createDirect("a", false)
                    .withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pMerge.parse("a"))
    }

    @Test
    fun standardInitialReduction() {
        // Default: Don't merge up
        val pDefault = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", StringTerm.createDirect("a", false)
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A", "a")), pDefault.parse("a"))

        // Same as default, but explicit
        val pMerge = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", StringTerm.createDirect("a", false)
                    .withReduction(ReductionType.initial())
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A", "a")), pMerge.parse("a"))
    }

    @Test
    fun taggedTreeReduction() {
        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", StringTerm.createDirect("a", false)
                    .withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B", "a"), pMerge2.parse("a"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", StringTerm.createDirect("a", false)
                    .withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B", "a")), pMerge3.parse("a"))

        // Create a parse tree which wouldn't be obvious from the grammar.
        val pMerge4 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", ConcatRule.create(
                    listOf(
                        StringTerm.createDirect("a", false)
                            .withReduction(ReductionType.taggedTree(Sym.sym("B"))),
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