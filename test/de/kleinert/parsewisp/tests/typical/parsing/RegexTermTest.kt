package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.parsing.ConcatRule
import de.kleinert.parsewisp.parsing.NonTerminal
import de.kleinert.parsewisp.parsing.RegexTerm
import de.kleinert.parsewisp.reduction.Reduction
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class RegexTermTest {
    @Test
    fun testUnicodeSequence() {
        // This used to cause a bug where "\\u" consumed one character too far.
        // E.g. "\\u0041-" would consume the minus as if it was part of the number sequence.
        val p = Parsewisp.parser("S = #'[\\u0041-\\u005A]+'")
        Assertions.assertEquals(PT.create("S", "ABC"), p.parse("ABC"))
    }

    private fun digit(): RegexTerm {
        return RegexTerm.create(Regex("[0-9]")) as RegexTerm
    }

    private fun number(): RegexTerm {
        return RegexTerm.create(Regex("[+\\-]?[0-9]+")) as RegexTerm
    }

    @Test
    fun create() {
        Assertions.assertEquals(
            RegexTerm.create(Regex("abc")),
            RegexTerm.create(Regex("abc"))
        )
        Assertions.assertEquals(
            RegexTerm.create(Regex("[0-9]+")),
            RegexTerm.create(Regex("[0-9]+"))
        )
        Assertions.assertEquals(
            RegexTerm.create(Regex("[0-9]1")),
            RegexTerm.create(Regex("[0-9]1"))
        )
    }

    @Test
    fun parse() {
        val gD = Gr.grammar("S", ConcatRule.create(listOf(digit(), digit())))
        val pD = Parsewisp.parser(gD)
        Assertions.assertEquals(PT.create("S", "1", "2"), pD.parse("12"))

        val gN = Gr.grammar("S", ConcatRule.create(listOf(digit(), number())))
        val pN = Parsewisp.parser(gN)
        Assertions.assertEquals(PT.create("S", "1", "234"), pN.parse("1234"))
    }

    @Test
    fun parseEager() {
        val gN = Gr.grammar("S", ConcatRule.create(listOf(number(), digit())))
        val pN = Parsewisp.parser(gN)
        Assertions.assertTrue(pN.parse("1234").isFailure)
    }

    @Test
    fun parseIncremental() {
        val gN = Gr.grammar("S", ConcatRule.create(listOf(number(), digit())))
        val pN = Parsewisp.parser(gN)
        Assertions.assertEquals(
            PT.create("S", "123", "4"),
            pN.parse("1234", ParsingOptions.default.withIterativeDeepening(true))
        )
    }

    @Test
    fun parseError() {
        val g = Gr.grammar("S", ConcatRule.create(listOf(digit(), digit())))
        val p = Parsewisp.parser(g)
        Assertions.assertTrue(p.parse("-1").isFailure)
        Assertions.assertTrue(p.parse("1").isFailure)
        Assertions.assertTrue(p.parse("b").isFailure)
    }

    @Test
    fun fullParseSingle() {
        val g = Gr.grammar("S", number())
        val p = Parsewisp.parser(g)
        Assertions.assertEquals(PT.create("S", "1"), p.parse("1"))
        Assertions.assertEquals(PT.create("S", "123"), p.parse("123"))
        Assertions.assertEquals(PT.create("S", "+123"), p.parse("+123"))
        Assertions.assertEquals(PT.create("S", "-123"), p.parse("-123"))
    }

    @Test
    fun fullParseConcat() {
        val gD = Gr.grammar("S", ConcatRule.create(listOf(digit(), digit())))
        val pD = Parsewisp.parser(gD)
        Assertions.assertEquals(PT.create("S", "1", "2"), pD.parse("12"))

        val gN = Gr.grammar("S", ConcatRule.create(listOf(digit(), number())))
        val pN = Parsewisp.parser(gN)
        Assertions.assertEquals(PT.create("S", "1", "234"), pN.parse("1234"))
    }

    @Test
    fun fullParseError() {
        val g = Gr.grammar("S", number())
        val p = Parsewisp.parser(g)
        Assertions.assertTrue(p.parse("").isFailure)
        Assertions.assertTrue(p.parse("a").isFailure)
        Assertions.assertTrue(p.parse("&1").isFailure)
    }

    @Test
    fun getRegexp() {
        val pattern = Regex("[0-9]")
        Assertions.assertEquals(
            pattern,
            (RegexTerm.create(pattern) as RegexTerm).regexp
        )
        Assertions.assertEquals(
            pattern.pattern,
            (RegexTerm.create(pattern) as RegexTerm).regexp.pattern
        )
    }

    @Test
    fun withHideTag() {
        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(digit(), digit().withHideTag(false))
        Assertions.assertEquals(digit(), digit().includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(digit().withHideTag(false), digit().withHideTag(false))
        Assertions.assertEquals(digit().withHideTag(true), digit().withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(digit().withHideTag(false).isHiddenInResult)
        Assertions.assertFalse(digit().includeInResult().isHiddenInResult)
        Assertions.assertTrue(digit().withHideTag(true).isHiddenInResult)
        Assertions.assertTrue(digit().hideInResult().isHiddenInResult)

        // Alternative names give equivalent output
        Assertions.assertEquals(digit().withHideTag(false), digit().includeInResult())
        Assertions.assertEquals(digit().withHideTag(true), digit().hideInResult())
    }

    @Test
    fun withHideTagParse() {
        // By default, not hidden in output
        val pDefault = Parsewisp.parser(
            Gr.grammar("S", digit())
        )
        Assertions.assertEquals(PT.create("S", "1"), pDefault.parse("1"))

        // With explicit show, also not hidden
        val pShow = Parsewisp.parser(
            Gr.grammar("S", digit().withHideTag(false))
        )
        Assertions.assertEquals(PT.create("S", "1"), pShow.parse("1"))

        // With explicit hide, also not hidden
        val pHide = Parsewisp.parser(
            Gr.grammar("S", digit().withHideTag(true))
        )
        Assertions.assertEquals(PT.create("S"), pHide.parse("1"))
    }

    @Test
    fun withReduction() {
        // Default.
        Assertions.assertEquals(
            digit(),
            digit().withReduction(Reduction.initial())
        )

        // Default: Don't merge up
        val pDefault = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", digit()
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A", "1")), pDefault.parse("1"))
    }

    @Test
    fun withReductionMergeUp() {
        // Merge upwards
        val pMerge = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", digit().withReduction(Reduction.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "1"), pMerge.parse("1"))
    }

    @Test
    fun withReductionTaggedTree() {
        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", digit().withReduction(Reduction.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B", "1"), pMerge2.parse("1"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", digit().withReduction(Reduction.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B", "1")), pMerge3.parse("1"))
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
                        digit().withReduction(Reduction.taggedTree(Sym.sym("B"))),
                        digit()
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("B", "1"), "2")),
            pMerge4.parse("12")
        )
    }
}