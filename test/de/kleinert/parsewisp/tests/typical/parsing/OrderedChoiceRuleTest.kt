package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class OrderedChoiceRuleTest {
    private fun string_a(): Rule {
        return StringTerm.createDirect("a", false)
    }

    private fun string_b(): Rule {
        return StringTerm.createDirect("b", false)
    }

    @Test
    fun create() {
        Assertions.assertEquals(
            OrderedChoiceRule.create(listOf(string_a(), string_b())),
            OrderedChoiceRule.create(listOf(string_a(), string_b()))
        )
    }

    @Test
    fun createEmpty() {
        // create(...) called with an empty input is free to create an Epsilon or something equivalent instead.
        Assertions.assertEquals(
            OrderedChoiceRule.create(listOf()),
            OrderedChoiceRule.create(listOf())
        )

        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", EpsilonTerm.default)).parse(""),
            Parsewisp.parser(Gr.grammar("S", OrderedChoiceRule.create(listOf()))).parse("")
        )
    }

    @Test
    fun createOnlyOne() {
        // create(...) called with only one element in the input list, it may return only that one element.
        // This behavior is not mandatory.
        Assertions.assertEquals(
            OrderedChoiceRule.create(listOf(string_a())),
            OrderedChoiceRule.create(listOf(string_a()))
        )

        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", string_a())).parse("a"),
            Parsewisp.parser(Gr.grammar("S", OrderedChoiceRule.create(listOf(string_a())))).parse("a")
        )
    }

    @Test
    fun createManySimilar() {
        // If an element in the input is repeated, create(...) can (and should) make the inputs distinct.
        val l = listOf(
            string_a(),
            string_b(),
            string_a(),
            string_b(),
            string_a(),
            string_b(),
            string_a(),
            string_b()
        )
        val lDistinct = l.distinct()
        Assertions.assertTrue(
            OrderedChoiceRule.create(lDistinct) == OrderedChoiceRule.create(l)
                    || OrderedChoiceRule.create(lDistinct) == OrderedChoiceRule.create(l)
        )

        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", OrderedChoiceRule.create(lDistinct))).parses("a"),
            Parsewisp.parser(Gr.grammar("S", OrderedChoiceRule.create(l))).parses("a")
        )
    }

    @Test
    fun parse() {
        val p_ab = Parsewisp.parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    listOf(
                        OrderedChoiceRule.create(listOf(string_a(), string_b())),
                        string_a()
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a", "a"), p_ab.parse("aa"))
        Assertions.assertEquals(PT.create("S", "b", "a"), p_ab.parse("ba"))
    }

    @Test
    fun parseRepeatedCasesDoNotMakeDuplicateTrees() {
        // Tests the grammar `S = "a" | "a" | "a"` on input "a". The expectation is that it creates exactly one tree, not three different ones.
        val p_a = Parsewisp.parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    listOf(
                        OrderedChoiceRule.create(
                            listOf(
                                string_a(),
                                string_a(),
                                string_a()
                            )
                        ), string_a()
                    )
                )
            )
        )
        Assertions.assertEquals(
            listOf(PT.create("S", "a", "a")),
            p_a.parses("aa")
        )
    }

    @Test
    fun parseError() {
        val p_ab = Parsewisp.parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    listOf(
                        OrderedChoiceRule.create(listOf(string_a(), string_b())),
                        string_a()
                    )
                )
            )
        )
        Assertions.assertTrue(p_ab.parse("ca").isFailure)
    }

    @Test
    fun fullParseSingle() {
        val p_ab = Parsewisp.parser(Gr.grammar("S", OrderedChoiceRule.create(listOf(string_a(), string_b()))))
        Assertions.assertEquals(PT.create("S", "a"), p_ab.parse("a"))
        Assertions.assertEquals(PT.create("S", "b"), p_ab.parse("b"))
    }

    @Test
    fun fullParseConcat() {
        val p_ab = Parsewisp.parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    listOf(
                        string_a(),
                        OrderedChoiceRule.create(listOf(string_a(), string_b()))
                    )
                )
            )
        )
        Assertions.assertEquals(PT.create("S", "a", "a"), p_ab.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "b"), p_ab.parse("ab"))
    }

    @Test
    fun fullParseRepeatedCasesDoNotMakeDuplicateTrees() {
        // Tests the grammar `S = "a" | "a" | "a"` on input "a". The expectation is that it creates exactly one tree, not three different ones.
        val p_a = Parsewisp.parser(Gr.grammar("S", OrderedChoiceRule.create(listOf(string_a(), string_a(), string_a()))))
        Assertions.assertEquals(
            listOf(PT.create("S", "a")),
            p_a.parses("a")
        )
    }

    @Test
    fun fullParseError() {
        val p_ab = Parsewisp.parser(Gr.grammar("S", OrderedChoiceRule.create(listOf(string_a(), string_b()))))
        Assertions.assertTrue(p_ab.parse("c").isFailure)
    }

    @Test
    fun getRules() {
        val r = OrderedChoiceRule.create(listOf(string_a(), string_b())) as OrderedChoiceRule
        Assertions.assertEquals(listOf(string_a(), string_b()), r.rules)
    }

    @Test
    fun withRules() {
        val i = listOf(string_a(), string_b())
        val r = OrderedChoiceRule.create(i) as OrderedChoiceRule

        // Expected
        Assertions.assertEquals(i, r.rules)

        val ni = listOf(string_a(), StringTerm.createDirect("c", false))
        val nr = r.withRules(ni)

        // The new rule.
        Assertions.assertEquals(ni, nr.rules)

        // Original left untouched.
        Assertions.assertEquals(i, r.rules)
    }

    @Test
    fun withRulesPreservesHideAndRed() {
        val i = listOf(string_a(), string_b())
        val r = OrderedChoiceRule
            .create(i)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(Sym.sym("A"))) as OrderedChoiceRule

        // As expected.
        Assertions.assertEquals(i, r.rules)

        // Create a new rule, check whether hide and reduction are still unchanged.
        val ni = listOf(string_a(), StringTerm.createDirect("c", false))
        val nr = r.withRules(ni)
        Assertions.assertEquals(ni, nr.rules)
        Assertions.assertEquals(r.isHiddenInResult1(), nr.isHiddenInResult1())
        Assertions.assertEquals(r.getReduction1(), nr.getReduction1())

        // Original left untouched.
        Assertions.assertEquals(i, r.rules)
    }

    @Test
    fun withHideTag() {
        val simpleRuleForTests = OrderedChoiceRule.create(listOf(string_a(), string_b())) as OrderedChoiceRule

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
        val simpleRuleForTests = OrderedChoiceRule.create(listOf(string_a(), string_b())) as OrderedChoiceRule

        // By default, not hidden in output
        val pDefault = Parsewisp.parser(
            Gr.grammar("S", simpleRuleForTests)
        )
        Assertions.assertEquals(PT.create("S", "a"), pDefault.parse("a"))

        // With explicit show, also not hidden
        val pShow = Parsewisp.parser(
            Gr.grammar("S", simpleRuleForTests.withHideTag(false))
        )
        Assertions.assertEquals(PT.create("S", "a"), pShow.parse("a"))

        // With explicit hide
        val pHide = Parsewisp.parser(
            Gr.grammar("S", simpleRuleForTests.withHideTag(true))
        )
        Assertions.assertEquals(PT.create("S"), pHide.parse("a"))
    }

    @Test
    fun withReduction() {
        val simpleRuleForTests = OrderedChoiceRule.create(listOf(string_a(), string_b())) as OrderedChoiceRule

        // Default.
        Assertions.assertEquals(
            simpleRuleForTests,
            simpleRuleForTests.withReduction(ReductionType.initial())
        )

        // Default: Don't merge up
        val pDefault = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", simpleRuleForTests
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("A", "a")), pDefault.parse("a"))
    }

    @Test
    fun withReductionMergeUp() {
        val simpleRuleForTests = OrderedChoiceRule.create(listOf(string_a(), string_b())) as OrderedChoiceRule
        // Merge upwards
        val pMerge = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", simpleRuleForTests.withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pMerge.parse("a"))
    }

    @Test
    fun withReductionTaggedTree() {
        val simpleRuleForTests = OrderedChoiceRule.create(listOf(string_a(), string_b())) as OrderedChoiceRule
        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", simpleRuleForTests.withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B", "a"), pMerge2.parse("a"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", simpleRuleForTests.withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B", "a")), pMerge3.parse("a"))
    }

    @Test
    fun withReductionNotDirectRhs() {
        // Add parse tree from a rule which doesn't appear in the grammar.
        val simpleRuleForTests = OrderedChoiceRule.create(listOf(string_a(), string_b())) as OrderedChoiceRule
        val pMerge4 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", ConcatRule.create(
                    listOf(
                        simpleRuleForTests.withReduction(ReductionType.taggedTree(Sym.sym("B"))),
                        StringTerm.create("b", false)
                    )
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A", PT.create("B", "a"), "b")),
            pMerge4.parse("ab")
        )
    }

    @Test
    fun multipleParsesTest1() {
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", OrderedChoiceRule.create(
                    listOf(
                        NonTerminal.create(Sym.sym("A")),
                        NonTerminal.create(Sym.sym("B")),
                        NonTerminal.create(Sym.sym("C")),
                        NonTerminal.create(Sym.sym("D")),
                        NonTerminal.create(Sym.sym("E"))
                    )
                ),
                "A", EpsilonTerm.default,
                "B", EpsilonTerm.default,
                "C", EpsilonTerm.default,
                "D", EpsilonTerm.default,
                "E", EpsilonTerm.default
            )
        )

        Assertions.assertEquals(
            listOf(
                PT.create("S", PT.create("A")),
                PT.create("S", PT.create("B")),
                PT.create("S", PT.create("C")),
                PT.create("S", PT.create("D")),
                PT.create("S", PT.create("E"))
            ),
            p.parses("")
        )
    }

    @Test
    fun recursiveParsesTest1() {
        // Tests the following grammar: `S = "a" S "a" / "a"
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", OrderedChoiceRule.create(
                    listOf(
                        ConcatRule.create(
                            listOf(
                                string_a(), NonTerminal.create(Sym.sym("S")), string_a()
                            )
                        ),
                        string_a()
                    )
                )
            )
        )

        Assertions.assertEquals(
            listOf(
                PT.create(
                    "S",
                    "a",
                    PT.create(
                        "S",
                        "a",
                        PT.create("S", "a"),
                        "a"
                    ),
                    "a"
                )
            ),
            p.parses("aaaaa")
        )
    }

    @Test
    fun recursiveMultipleParsesTest() {
        // Tests the following grammar: `S = "a" | S S
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", OrderedChoiceRule.create(
                    listOf(
                        string_a(),
                        ConcatRule.create(
                            listOf(
                                NonTerminal.create(Sym.sym("S")), NonTerminal.create(Sym.sym("S"))
                            )
                        )
                    )
                )
            )
        )

        // Results for parsing "aaa"
        Assertions.assertEquals(
            listOf(
                PT.create(
                    "S",
                    PT.create("S", "a"),
                    PT.create(
                        "S",
                        PT.create("S", "a"),
                        PT.create("S", "a")
                    )
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create("S", "a"),
                        PT.create("S", "a")
                    ),
                    PT.create("S", "a")
                )
            ),
            p.parses("aaa")
        )

        // Results for parsing "aaaaa" (5x "a")
        Assertions.assertEquals(
            listOf(
                PT.create(
                    "S",
                    PT.create("S", "a"),
                    PT.create(
                        "S",
                        PT.create("S", "a"),
                        PT.create("S", PT.create("S", "a"), PT.create("S", PT.create("S", "a"), PT.create("S", "a")))
                    )
                ),
                PT.create(
                    "S",
                    PT.create("S", PT.create("S", "a"), PT.create("S", "a")),
                    PT.create("S", PT.create("S", "a"), PT.create("S", PT.create("S", "a"), PT.create("S", "a")))
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create("S", PT.create("S", "a"), PT.create("S", PT.create("S", "a"), PT.create("S", "a"))),
                        PT.create("S", "a")
                    ),
                    PT.create("S", "a")
                ),
                PT.create(
                    "S",
                    PT.create("S", PT.create("S", "a"), PT.create("S", PT.create("S", "a"), PT.create("S", "a"))),
                    PT.create("S", PT.create("S", "a"), PT.create("S", "a"))
                ),
                PT.create(
                    "S",
                    PT.create("S", PT.create("S", "a"), PT.create("S", "a")),
                    PT.create("S", PT.create("S", PT.create("S", "a"), PT.create("S", "a")), PT.create("S", "a"))
                ),
                PT.create(
                    "S",
                    PT.create("S", "a"),
                    PT.create(
                        "S",
                        PT.create("S", PT.create("S", "a"), PT.create("S", "a")),
                        PT.create("S", PT.create("S", "a"), PT.create("S", "a"))
                    )
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create("S", PT.create("S", "a"), PT.create("S", "a")),
                        PT.create("S", PT.create("S", "a"), PT.create("S", "a"))
                    ),
                    PT.create("S", "a")
                ),
                PT.create(
                    "S",
                    PT.create("S", PT.create("S", PT.create("S", "a"), PT.create("S", "a")), PT.create("S", "a")),
                    PT.create("S", PT.create("S", "a"), PT.create("S", "a"))
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create("S", "a"),
                        PT.create("S", PT.create("S", "a"), PT.create("S", PT.create("S", "a"), PT.create("S", "a")))
                    ),
                    PT.create("S", "a")
                ),
                PT.create(
                    "S",
                    PT.create("S", "a"),
                    PT.create(
                        "S",
                        PT.create("S", PT.create("S", "a"), PT.create("S", PT.create("S", "a"), PT.create("S", "a"))),
                        PT.create("S", "a")
                    )
                ),
                PT.create(
                    "S",
                    PT.create("S", "a"),
                    PT.create(
                        "S",
                        PT.create("S", PT.create("S", PT.create("S", "a"), PT.create("S", "a")), PT.create("S", "a")),
                        PT.create("S", "a")
                    )
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create("S", PT.create("S", PT.create("S", "a"), PT.create("S", "a")), PT.create("S", "a")),
                        PT.create("S", "a")
                    ),
                    PT.create("S", "a")
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create("S", "a"),
                        PT.create("S", PT.create("S", PT.create("S", "a"), PT.create("S", "a")), PT.create("S", "a"))
                    ),
                    PT.create("S", "a")
                ),
                PT.create(
                    "S",
                    PT.create("S", "a"),
                    PT.create(
                        "S",
                        PT.create("S", "a"),
                        PT.create("S", PT.create("S", PT.create("S", "a"), PT.create("S", "a")), PT.create("S", "a"))
                    )
                )
            ),
            p.parses("aaaaa")
        )
    }
}