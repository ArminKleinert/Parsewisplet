package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

internal class ValueRangeTermTest {
    @Test
    fun create() {
        Assertions.assertEquals(
            ValueRangeTerm.create(0, 255),
            ValueRangeTerm.create(0, 255)
        )
    }

    @Test
    fun createFailure() {
        // Condition broken: lo <= hi
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            { ValueRangeTerm.create(66, 65) })

        // Condition broken: lo >= 0
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            { ValueRangeTerm.create(-1, 255) })

        // Condition broken: hi <= 0x10FFFF
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            { ValueRangeTerm.create(0, 0x110000) })
    }

    @Test
    fun parse() {
        val simple_a = ValueRangeTerm.create('a'.code, 'a'.code) as ValueRangeTerm
        val fullUnicode = ValueRangeTerm.create(0, 0x10FFFF) as ValueRangeTerm

        // Case-sensitive.
        // Will use parse(...) for "a" and fullParse(...) for "b"
        val p = parser(Gr.grammar("S", ConcatRule.create(listOf(simple_a, fullUnicode))))
        Assertions.assertEquals(PT.create("S", "a", "b"), p.parse("ab"))
    }

    @Test
    fun parseError() {
        val simple_a = ValueRangeTerm.create('a'.code, 'a'.code) as ValueRangeTerm
        val fullUnicode = ValueRangeTerm.create(0, 0x10FFFF) as ValueRangeTerm

        // Will use parse(...) for "a" and fullParse(...) for "b"
        val p = parser(Gr.grammar("S", ConcatRule.create(listOf(simple_a, fullUnicode))))
        Assertions.assertTrue(p.parse("b").isFailure)
    }

    @Test
    fun fullParseSingle() {
        val simple_a = ValueRangeTerm.create('a'.code, 'a'.code) as ValueRangeTerm

        // Case-sensitive.
        val p = parser(Gr.grammar("S", simple_a))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
    }

    @Test
    fun fullParseConcat() {
        val simple_a = ValueRangeTerm.create('a'.code, 'a'.code) as ValueRangeTerm
        val fullUnicode = ValueRangeTerm.create(0, 0x10FFFF) as ValueRangeTerm

        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(listOf(simple_a, fullUnicode))
            )
        )
        Assertions.assertEquals(PT.create("S", "a", "b"), p.parse("ab"))
    }

    @Test
    fun fullParseError() {
        val simple_a = ValueRangeTerm.create('a'.code, 'a'.code) as ValueRangeTerm

        val p = parser(Gr.grammar("S", simple_a)) // Will use parse(...) for "a" and fullParse(...) for "b"
        Assertions.assertTrue(p.parse("").isFailure)
    }

    @Test
    fun getLo() {
        val r1 = ValueRangeTerm.create(0, 0x10FFFF) as ValueRangeTerm
        val r2 = ValueRangeTerm.create(0x10FFFF, 0x10FFFF) as ValueRangeTerm
        Assertions.assertEquals(0, r1.lo)
        Assertions.assertEquals(0x10FFFF, r2.lo)
    }

    @Test
    fun getHi() {
        val r1 = ValueRangeTerm.create(0, 0x10FFFF) as ValueRangeTerm
        val r2 = ValueRangeTerm.create(0, 0) as ValueRangeTerm
        Assertions.assertEquals(0x10FFFF, r1.hi)
        Assertions.assertEquals(0, r2.hi)
    }

    @Test
    fun withHideTag() {
        val simpleRuleForTests = ValueRangeTerm.create('a'.code, 'z'.code) as ValueRangeTerm

        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(simpleRuleForTests, simpleRuleForTests.withHideTag(false))
        Assertions.assertEquals(simpleRuleForTests, simpleRuleForTests.includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(simpleRuleForTests.withHideTag(false), simpleRuleForTests.withHideTag(false))
        Assertions.assertEquals(simpleRuleForTests.withHideTag(true), simpleRuleForTests.withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(simpleRuleForTests.withHideTag(false).isHiddenInResult1())
        Assertions.assertFalse(simpleRuleForTests.includeInResult().isHiddenInResult1())
        Assertions.assertTrue(simpleRuleForTests.withHideTag(true).isHiddenInResult1())
        Assertions.assertTrue(simpleRuleForTests.hideInResult().isHiddenInResult1())

        // Alternative names give equivalent output
        Assertions.assertEquals(simpleRuleForTests.withHideTag(false), simpleRuleForTests.includeInResult())
        Assertions.assertEquals(simpleRuleForTests.withHideTag(true), simpleRuleForTests.hideInResult())
    }

    @Test
    fun withHideTagParse() {
        val simple_a = ValueRangeTerm.create('a'.code, 'a'.code) as ValueRangeTerm

        // By default, not hidden in output
        val pDefault = parser(
            Gr.grammar("S", simple_a)
        )
        Assertions.assertEquals(PT.create("S", "a"), pDefault.parse("a"))

        // With explicit show, also not hidden
        val pShow = parser(
            Gr.grammar("S", simple_a.withHideTag(false))
        )
        Assertions.assertEquals(PT.create("S", "a"), pShow.parse("a"))

        // With explicit hide
        val pHide = parser(
            Gr.grammar("S", simple_a.withHideTag(true))
        )
        Assertions.assertEquals(PT.create("S"), pHide.parse("a"))
    }

    @Test
    fun withReduction() {
        val simple_a = ValueRangeTerm.create('a'.code, 'a'.code) as ValueRangeTerm

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
        Assertions.assertEquals(PT.create("S", PT.create("A", "a")), pDefault.parse("a"))
    }

    @Test
    fun withReductionMergeUp() {
        val simple_a = ValueRangeTerm.create('a'.code, 'a'.code) as ValueRangeTerm

        // Merge upwards
        val pMerge = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simple_a.withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pMerge.parse("a"))
    }

    @Test
    fun withReductionTaggedTree() {
        val simple_a = ValueRangeTerm.create('a'.code, 'a'.code) as ValueRangeTerm

        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = parser(
            Gr.grammarS(
                "S",
                "S", simple_a.withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B", "a"), pMerge2.parse("a"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simple_a.withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B", "a")), pMerge3.parse("a"))
    }

    @Test
    fun withReductionNotDirectRhs() {
        val simple_a = ValueRangeTerm.create('a'.code, 'a'.code) as ValueRangeTerm

        // Add parse tree from a rule which doesn't appear in the grammar.
        val pMerge4 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", ConcatRule.create(
                    listOf(
                        simple_a.withReduction(ReductionType.taggedTree(sym("B"))),
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