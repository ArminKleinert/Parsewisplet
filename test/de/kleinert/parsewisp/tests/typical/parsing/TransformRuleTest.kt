package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.ConcatRule
import de.kleinert.parsewisp.parsing.NonTerminal
import de.kleinert.parsewisp.parsing.StringTerm
import de.kleinert.parsewisp.parsing.TransformRule
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TransformRuleTest {
    private val simpleF: (List<Any?>, Int)-> List<Any?> =
        { r, _ -> listOf((r[0] as String).repeat(2)) }

    @Test
    fun create() {
        Assertions.assertEquals(simpleF, simpleF)
        Assertions.assertEquals(
            TransformRule.create(StringTerm.create("a", false), simpleF),
            TransformRule.create(StringTerm.create("a", false), simpleF)
        )
    }

    @Test
    fun parse() {
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        TransformRule.create(
                            StringTerm.create("abc", false), { r, _ ->
                                (r[0] as String).split("".toRegex()).filterNot { it.isEmpty() }
                            }),
                        StringTerm.create("def", false)
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", "a", "b", "c", "def"),
            p.parse("abcdef")
        )
    }

    @Test
    fun fullParseSingle() {
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", TransformRule.create(
                    StringTerm.create("abc", false)
                ) { _, _ -> listOf("a", "b", "c") }))
        Assertions.assertEquals(
            PT.create("S", "a", "b", "c"),
            p.parse("abc")
        )
    }

    @Test
    fun fullParseConcat() {
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        StringTerm.create("abc", false),
                        TransformRule.create(
                            StringTerm.create("def", false)
                        ) { r, _ ->
                                (r[0] as String).split("".toRegex()).filterNot { it.isEmpty() }
                        }
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", "abc", "d", "e", "f"),
            p.parse("abcdef")
        )
    }

    @Test
    fun getRule() {
        val inner = StringTerm.create("a", false)
        val r = TransformRule.create(inner, simpleF) as TransformRule
        Assertions.assertEquals(inner, r.rule)
    }

    @Test
    fun withRule() {
        val i = StringTerm.create("a", false)
        val r = TransformRule.create(i, simpleF) as TransformRule

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
        val r = TransformRule.create(i, simpleF)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(Sym.sym("A"))) as TransformRule

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
        val r = TransformRule.create(StringTerm.create("a", false), simpleF) as TransformRule

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
        val simpleA = TransformRule.create(
            StringTerm.createDirect("a", false), simpleF
        ) as TransformRule

        // By default, not hidden in output
        val pDefault = Parsewisp.parser(
            Gr.grammar("S", simpleA)
        )
        Assertions.assertEquals(PT.create("S", "aa"), pDefault.parse("a"))

        // With explicit show, also not hidden
        val pShow = Parsewisp.parser(
            Gr.grammar("S", simpleA.withHideTag(false))
        )
        Assertions.assertEquals(PT.create("S", "aa"), pShow.parse("a"))

        // With explicit hide
        val pHide = Parsewisp.parser(
            Gr.grammar("S", simpleA.withHideTag(true))
        )
        Assertions.assertEquals(PT.create("S"), pHide.parse("a"))
    }

    @Test
    fun withReduction() {
        val simpleA = TransformRule.create(
            StringTerm.createDirect("a", false), simpleF
        ) as TransformRule

        // Default.
        Assertions.assertEquals(
            simpleA,
            simpleA.withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", simpleA
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A", "aa")), pDefault.parse("a"))
    }

    @Test
    fun withReductionMergeUp() {
        val simpleA = TransformRule.create(
            StringTerm.createDirect("a", false), simpleF
        ) as TransformRule

        // Merge upwards
        val pMerge = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", simpleA.withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "aa"), pMerge.parse("a"))
    }

    @Test
    fun withReductionTaggedTree() {
        val simpleA = TransformRule.create(
            StringTerm.createDirect("a", false), simpleF
        ) as TransformRule

        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", simpleA.withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B", "aa"), pMerge2.parse("a"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", simpleA.withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B", "aa")), pMerge3.parse("a"))
    }

    @Test
    fun withReductionNotDirectRhs() {
        val simpleA = TransformRule.create(
            StringTerm.createDirect("a", false), simpleF
        ) as TransformRule

        // Add parse tree from a rule which doesn't appear in the grammar.
        val pMerge4 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", ConcatRule.create(
                    listOf(
                        simpleA.withReduction(ReductionType.taggedTree(Sym.sym("B"))),
                        StringTerm.createDirect("b", false)
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("B", "aa"), "b")),
            pMerge4.parse("ab")
        )
    }
}