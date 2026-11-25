package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.function.BiFunction

internal class TransformRuleTest {
    private val simple_f:BiFunction<List<Any?>, Int, List<Any?>> =
        BiFunction { r: List<Any?>, i: Int -> listOf((r[0] as String).repeat(2)) }

    @Test
    fun create() {
        Assertions.assertEquals(
            TransformRule.create(StringTerm.create("a", false), simple_f),
            TransformRule.create(StringTerm.create("a", false), simple_f)
        )
    }

    @Test
    fun parse() {
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        TransformRule.create(
                            StringTerm.create("abc", false),
                            { r, i ->
                                (r!!.get(0) as String).split("".toRegex()).filterNot { it.isEmpty() }
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
        val p = parser(
            Gr.grammar(
                "S", TransformRule.create(
                    StringTerm.create("abc", false)
                ) { r: List<Any?>, i: Int -> listOf("a", "b", "c") }))
        Assertions.assertEquals(
            PT.create("S", "a", "b", "c"),
            p.parse("abc")
        )
    }

    @Test
    fun fullParseConcat() {
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        StringTerm.create("abc", false),
                        TransformRule.create(
                            StringTerm.create("def", false)
                        ) { r, i ->
                                (r!!.get(0) as String).split("".toRegex()).filterNot { it.isEmpty() }
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
        val r = TransformRule.create(inner, simple_f) as TransformRule
        Assertions.assertEquals(inner, r.rule)
    }

    @Test
    fun withRule() {
        val i = StringTerm.create("a", false)
        val r = TransformRule.create(i, simple_f) as TransformRule

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
        val r = TransformRule.create(i, simple_f)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(sym("A"))) as TransformRule

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
        val r = TransformRule.create(StringTerm.create("a", false), simple_f) as TransformRule

        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(r, r.withHideTag(false))
        Assertions.assertEquals(r, r.includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(r.withHideTag(false), r.withHideTag(false))
        Assertions.assertEquals(r.withHideTag(true), r.withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(r.withHideTag(false).isHiddenInResult1())
        Assertions.assertFalse(r.includeInResult().isHiddenInResult1())
        Assertions.assertTrue(r.withHideTag(true).isHiddenInResult1())
        Assertions.assertTrue(r.hideInResult().isHiddenInResult1())

        // Alternative names give equivalent output
        Assertions.assertEquals(r.withHideTag(false), r.includeInResult())
        Assertions.assertEquals(r.withHideTag(true), r.hideInResult())
    }

    @Test
    fun withHideTagParse() {
        val simple_a = TransformRule.create(
            StringTerm.createDirect("a", false), simple_f
        ) as TransformRule

        // By default, not hidden in output
        val pDefault = parser(
            Gr.grammar("S", simple_a)
        )
        Assertions.assertEquals(PT.create("S", "aa"), pDefault.parse("a"))

        // With explicit show, also not hidden
        val pShow = parser(
            Gr.grammar("S", simple_a.withHideTag(false))
        )
        Assertions.assertEquals(PT.create("S", "aa"), pShow.parse("a"))

        // With explicit hide
        val pHide = parser(
            Gr.grammar("S", simple_a.withHideTag(true))
        )
        Assertions.assertEquals(PT.create("S"), pHide.parse("a"))
    }

    @Test
    fun withReduction() {
        val simple_a = TransformRule.create(
            StringTerm.createDirect("a", false), simple_f
        ) as TransformRule

        // Default.
        Assertions.assertEquals(
            simple_a,
            simple_a.withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simple_a
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A", "aa")), pDefault.parse("a"))
    }

    @Test
    fun withReductionMergeUp() {
        val simple_a = TransformRule.create(
            StringTerm.createDirect("a", false), simple_f
        ) as TransformRule

        // Merge upwards
        val pMerge = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simple_a.withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "aa"), pMerge.parse("a"))
    }

    @Test
    fun withReductionTaggedTree() {
        val simple_a = TransformRule.create(
            StringTerm.createDirect("a", false), simple_f
        ) as TransformRule

        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = parser(
            Gr.grammarS(
                "S",
                "S", simple_a.withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B", "aa"), pMerge2.parse("a"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simple_a.withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B", "aa")), pMerge3.parse("a"))
    }

    @Test
    fun withReductionNotDirectRhs() {
        val simple_a = TransformRule.create(
            StringTerm.createDirect("a", false), simple_f
        ) as TransformRule

        // Add parse tree from a rule which doesn't appear in the grammar.
        val pMerge4 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", ConcatRule.create(
                    listOf<Rule>(
                        simple_a.withReduction(ReductionType.taggedTree(sym("B"))),
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