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
import java.util.regex.Pattern

internal class ExclusionRuleTest {
    private fun word(): Rule {
        return RegexTerm.create(Pattern.compile("[a-zA-Z][a-zA-Z0-1_]*"))
    }

    private fun keywords(): Rule {
        return AlternationRule.create(
            listOf(
                StringTerm.create("int", false),
                StringTerm.create("float", false)
            )
        )
    }

    private fun wordButNotKeyword(): Rule {
        return ExclusionRule.create(word(), keywords())
    }

    @Test
    fun create() {
        Assertions.assertEquals(
            ExclusionRule.create(word(), keywords()),
            ExclusionRule.create(word(), keywords())
        )

        // The order matters
        Assertions.assertNotEquals(
            ExclusionRule.create(keywords(), word()),
            ExclusionRule.create(word(), keywords())
        )
    }

    @Test
    fun createExcludedInclusion() {
        // create(...) is allowed to create an Epsilon if the inclusion and exclusion sides are the same.
        val exSame = ExclusionRule.create(word(), word())
        Assertions.assertTrue(exSame is ExclusionRule || exSame is EpsilonTerm)
    }

    @Test
    fun parse() {
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        wordButNotKeyword(),
                        StringTerm.create(";", false) // Required so the exclusion calls "parse" instead of "fullParse".
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", "anyword", ";"),
            p.parse("anyword;")
        )
    }

    @Test
    fun parseError() {
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        wordButNotKeyword(),
                        StringTerm.create(";", false) // Required so the exclusion calls "parse" instead of "fullParse".
                    )
                )
            )
        )
        Assertions.assertTrue(p.parse("int;").isFailure) // Matched by exclusion
        Assertions.assertTrue(p.parse("...;").isFailure) // Invalid for inclusion
    }

    @Test
    fun fullParseSingle() {
        val p = parser(Gr.grammar("S", wordButNotKeyword()))
        Assertions.assertEquals(
            PT.create("S", "anyword"),
            p.parse("anyword")
        )
    }

    @Test
    fun fullParseConcat() {
        val p = parser(
            Gr.grammar(
                "S", ConcatRule.create(
                    listOf(
                        StringTerm.create(";", false),
                        wordButNotKeyword()
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", ";", "anyword"),
            p.parse(";anyword")
        )
    }

    @Test
    fun fullParseError() {
        val p = parser(Gr.grammar("S", wordButNotKeyword()))
        Assertions.assertTrue(p.parse("int").isFailure) // Matched by exclusion
        Assertions.assertTrue(p.parse("...").isFailure) // Invalid for inclusion
    }

    @Test
    fun getExpected() {
        val expected = word()
        val r = ExclusionRule.create(expected, keywords()) as ExclusionRule
        Assertions.assertSame(expected, r.expected)
    }

    @Test
    fun getExcluded() {
        val excluded = keywords()
        val r = ExclusionRule.create(word(), excluded) as ExclusionRule
        Assertions.assertSame(excluded, r.excluded)
    }

    @Test
    fun getRules() {
        val r = ExclusionRule.create(word(), keywords()) as ExclusionRule
        Assertions.assertEquals(listOf(word(), keywords()), r.rules)
    }

    @Test
    fun withRulesRequiresExactlyTwo() {
        val r = ExclusionRule.create(word(), keywords()) as ExclusionRule
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            { r.withRules(listOf()) })
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            { r.withRules(listOf(word())) })
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            {
                r.withRules(
                    listOf(
                        word(), keywords(),
                        RegexTerm.create(Pattern.compile("[a-zA-Z]"))
                    )
                )
            })
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            {
                r.withRules(
                    listOf(
                        word(), keywords(),
                        RegexTerm.create(Pattern.compile("[a-zA-Z]")),
                        RegexTerm.create(Pattern.compile("[0-9]"))
                    )
                )
            })
    }

    @Test
    fun withRules() {
        val i = listOf(word(), keywords())
        val r = ExclusionRule.create(word(), keywords()) as ExclusionRule

        // Expected
        Assertions.assertEquals(i, r.rules)

        val ni = listOf(word(), StringTerm.createDirect("c", false))
        val nr = r.withRules(ni)

        // The new rule.
        Assertions.assertEquals(ni, nr.rules)

        // Original left untouched.
        Assertions.assertEquals(i, r.rules)
    }

    @Test
    fun withRulesPreservesHideAndRed() {
        val i = listOf(word(), keywords())
        val r = ExclusionRule
            .create(word(), keywords())
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(sym("A"))) as ExclusionRule

        // Expected
        Assertions.assertEquals(i, r.rules)

        // Create a new rule, check whether hide and reduction are still unchanged.
        val ni = listOf(word(), StringTerm.createDirect("c", false))
        val nr = r.withRules(ni)
        Assertions.assertEquals(ni, nr.rules)
        Assertions.assertEquals(r.isHiddenInResult1(), nr.isHiddenInResult1())
        Assertions.assertEquals(r.getReduction1(), nr.getReduction1())

        // Original left untouched.
        Assertions.assertEquals(i, r.rules)
    }

    @Test
    fun withHideTag() {
        val simpleRuleForTests = ExclusionRule.create(word(), keywords()) as ExclusionRule

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
        val simpleRuleForTests = ExclusionRule.create(word(), keywords()) as ExclusionRule

        // By default, not hidden in output
        val pDefault = parser(
            Gr.grammar("S", simpleRuleForTests)
        )
        Assertions.assertEquals(PT.create("S", "a"), pDefault.parse("a"))

        // With explicit show, also not hidden
        val pShow = parser(
            Gr.grammar("S", simpleRuleForTests.withHideTag(false))
        )
        Assertions.assertEquals(PT.create("S", "a"), pShow.parse("a"))

        // With explicit hide
        val pHide = parser(
            Gr.grammar("S", simpleRuleForTests.withHideTag(true))
        )
        Assertions.assertEquals(PT.create("S"), pHide.parse("a"))
    }

    @Test
    fun withReduction() {
        val simpleRuleForTests = ExclusionRule.create(word(), keywords()) as ExclusionRule

        // Default.
        Assertions.assertEquals(
            simpleRuleForTests,
            simpleRuleForTests.withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simpleRuleForTests
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A", "a")), pDefault.parse("a"))
    }

    @Test
    fun withReductionMergeUp() {
        val simpleRuleForTests = ExclusionRule.create(word(), keywords()) as ExclusionRule

        // Merge upwards
        val pMerge = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simpleRuleForTests.withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pMerge.parse("a"))
    }

    @Test
    fun withReductionTaggedTree() {
        val simpleRuleForTests = ExclusionRule.create(word(), keywords()) as ExclusionRule

        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = parser(
            Gr.grammarS(
                "S",
                "S", simpleRuleForTests.withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B", "a"), pMerge2.parse("a"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simpleRuleForTests.withReduction(ReductionType.taggedTree(sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B", "a")), pMerge3.parse("a"))
    }

    @Test
    fun withReductionNotDirectRhs() {
        val simpleRuleForTests = ExclusionRule.create(word(), keywords()) as ExclusionRule

        // Add parse tree from a rule which doesn't appear in the grammar.
        val pMerge4 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", ConcatRule.create(
                    listOf(
                        simpleRuleForTests.withReduction(ReductionType.taggedTree(sym("B"))),
                        StringTerm.create(";", false)
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("B", "a"), ";")),
            pMerge4.parse("a;")
        )
    }
}