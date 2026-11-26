package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class AlternationRuleTest {
    private fun string_a(): Rule {
        return StringTerm.createDirect("a", false)
    }

    private fun string_b(): Rule {
        return StringTerm.createDirect("b", false)
    }

    @Test
    fun create() {
        Assertions.assertEquals(
            AlternationRule.create(listOf(string_a(), string_b())),
            AlternationRule.create(listOf(string_a(), string_b()))
        )
    }

    @Test
    fun createEmpty() {
        // create(...) called with an empty input is free to create an Epsilon or something equivalent instead.
        Assertions.assertEquals(
            AlternationRule.create(listOf()),
            AlternationRule.create(listOf())
        )

        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", EpsilonTerm.default)).parse(""),
            Parsewisp.parser(Gr.grammar("S", AlternationRule.create(listOf()))).parse("")
        )
    }

    @Test
    fun createOnlyOne() {
        // create(...) called with only one element in the input list, it may return only that one element.
        // This behavior is not mandatory.
        Assertions.assertEquals(
            AlternationRule.create(listOf(string_a())),
            AlternationRule.create(listOf(string_a()))
        )

        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", string_a())).parse("a"),
            Parsewisp.parser(Gr.grammar("S", AlternationRule.create(listOf(string_a())))).parse("a")
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
            AlternationRule.altUnsafe(lDistinct) == AlternationRule.create(l)
                    || AlternationRule.create(lDistinct) == AlternationRule.create(l)
        )

        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", AlternationRule.create(lDistinct))).parses("a"),
            Parsewisp.parser(Gr.grammar("S", AlternationRule.create(l))).parses("a")
        )
    }

    @Test
    fun altUnsafe() {
        Assertions.assertEquals(
            AlternationRule.altUnsafe(listOf(string_a(), string_a(), string_a())),
            AlternationRule.altUnsafe(listOf(string_a(), string_a(), string_a()))
        )
        Assertions.assertEquals(
            AlternationRule.create(listOf(string_a(), string_b())),
            AlternationRule.altUnsafe(listOf(string_a(), string_b()))
        )

        Assertions.assertEquals(
            AlternationRule.altUnsafe(listOf()),
            AlternationRule.altUnsafe(listOf())
        )

        Assertions.assertEquals(
            AlternationRule.altUnsafe(listOf(EpsilonTerm.default, EpsilonTerm.default)),
            AlternationRule.altUnsafe(listOf(EpsilonTerm.default, EpsilonTerm.default))
        )
        Assertions.assertNotEquals(
            AlternationRule.altUnsafe(listOf(EpsilonTerm.default)),
            AlternationRule.altUnsafe(listOf(EpsilonTerm.default, EpsilonTerm.default))
        )
        Assertions.assertEquals(
            2,
            AlternationRule.altUnsafe(listOf(EpsilonTerm.default, EpsilonTerm.default)).rules.size
        )

        val flat = AlternationRule.altUnsafe(listOf(string_a(), string_a(), string_a(), string_a()))
        val nest1 = AlternationRule.altUnsafe(
            listOf(
                AlternationRule.altUnsafe(listOf(string_a(), string_a())),
                string_a(),
                string_a()
            )
        )
        val nest2 = AlternationRule.altUnsafe(
            listOf(
                string_a(),
                AlternationRule.altUnsafe(listOf(string_a(), string_a())),
                string_a()
            )
        )
        val nest3 = AlternationRule.altUnsafe(
            listOf(
                string_a(),
                string_a(),
                AlternationRule.altUnsafe(listOf(string_a(), string_a()))
            )
        )
        Assertions.assertNotEquals(flat, nest1)
        Assertions.assertNotEquals(flat, nest2)
        Assertions.assertNotEquals(flat, nest3)
    }

    @Test
    fun parse() {
        val p_ab = Parsewisp.parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    listOf(
                        AlternationRule.create(listOf(string_a(), string_b())),
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
                        AlternationRule.create(
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
            listOf<ParseTree?>(PT.create("S", "a", "a")),
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
                        AlternationRule.create(listOf(string_a(), string_b())),
                        string_a()
                    )
                )
            )
        )
        Assertions.assertTrue(p_ab.parse("ca").isFailure)
    }

    @Test
    fun fullParseSingle() {
        val p_ab = Parsewisp.parser(Gr.grammar("S", AlternationRule.create(listOf(string_a(), string_b()))))
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
                        AlternationRule.create(listOf(string_a(), string_b()))
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
        val p_a = Parsewisp.parser(Gr.grammar("S", AlternationRule.altUnsafe(listOf(string_a(), string_a(), string_a()))))
        Assertions.assertEquals(
            listOf<ParseTree?>(PT.create("S", "a")),
            p_a.parses("a")
        )
    }

    @Test
    fun fullParseError() {
        val p_ab = Parsewisp.parser(Gr.grammar("S", AlternationRule.create(listOf(string_a(), string_b()))))
        Assertions.assertTrue(p_ab.parse("c").isFailure)
    }

    @Test
    fun getRules() {
        val flat = AlternationRule.altUnsafe(listOf(string_a(), string_a(), string_a(), string_a()))
        val nest1 = AlternationRule.altUnsafe(
            listOf(
                AlternationRule.altUnsafe(listOf(string_a(), string_a())),
                string_a(),
                string_a()
            )
        )
        val nest2 = AlternationRule.altUnsafe(
            listOf(
                string_a(),
                AlternationRule.altUnsafe(listOf(string_a(), string_a())),
                string_a()
            )
        )
        val nest3 = AlternationRule.altUnsafe(
            listOf(
                string_a(),
                string_a(),
                AlternationRule.altUnsafe(listOf(string_a(), string_a()))
            )
        )

        Assertions.assertEquals(listOf<Rule?>(string_a(), string_a(), string_a(), string_a()), flat.rules)
        Assertions.assertEquals(
            listOf<Rule?>(
                AlternationRule.altUnsafe(listOf(string_a(), string_a())),
                string_a(),
                string_a()
            ), nest1.rules
        )
        Assertions.assertEquals(
            listOf<Rule?>(
                string_a(),
                AlternationRule.altUnsafe(listOf(string_a(), string_a())),
                string_a()
            ), nest2.rules
        )
        Assertions.assertEquals(
            listOf<Rule?>(
                string_a(),
                string_a(),
                AlternationRule.altUnsafe(listOf(string_a(), string_a()))
            ), nest3.rules
        )
    }

    @Test
    fun withRules() {
        val i = listOf(string_a(), string_b())
        val r = AlternationRule.altUnsafe(i)

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
        val r = AlternationRule
            .altUnsafe(i)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(Sym.sym("A")))

        // Expected
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
        val simpleRuleForTests = AlternationRule.altUnsafe(listOf(string_a(), string_b()))

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
        val simpleRuleForTests = AlternationRule.altUnsafe(listOf(string_a(), string_b()))

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

        // Merge upwards
        val pMerge = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", simpleRuleForTests.withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pMerge.parse("a"))

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

        // Add parse tree from a rule which doesn't appear in the grammar.
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
                "S", AlternationRule.altUnsafe(
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
            setOf<ParseTree?>(
                PT.create("S", PT.create("A")),
                PT.create("S", PT.create("B")),
                PT.create("S", PT.create("C")),
                PT.create("S", PT.create("D")),
                PT.create("S", PT.create("E"))
            ),
            HashSet<ParseTree?>(p.parses(""))
        )
    }

    @Test
    fun recursiveParsesTest1() {
        // Tests the following grammar: `S = "a" S "a" | "a"
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", AlternationRule.altUnsafe(
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
            setOf<ParseTree?>(
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
            HashSet<ParseTree?>(p.parses("aaaaa"))
        )
    }

    @Test
    fun recursiveMultipleParsesTest() {
        // Tests the following grammar: `S = "a" | S S
        val p = Parsewisp.parser(
            Gr.grammar(
                "S", AlternationRule.altUnsafe(
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
            setOf<ParseTree?>(
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
            HashSet<ParseTree?>(p.parses("aaa"))
        )

        // Results for parsing "aaaaa" (5x "a")
        Assertions.assertEquals(
            setOf<ParseTree?>(
                PT.create(
                    "S",
                    PT.create("S", "a"),
                    PT.create(
                        "S",
                        PT.create("S", "a"),
                        PT.create(
                            "S",
                            PT.create("S", "a"),
                            PT.create(
                                "S",
                                PT.create("S", "a"),
                                PT.create("S", "a")
                            )
                        )
                    )
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create("S", "a"),
                        PT.create("S", "a")
                    ),
                    PT.create(
                        "S",
                        PT.create("S", "a"),
                        PT.create(
                            "S",
                            PT.create("S", "a"),
                            PT.create("S", "a")
                        )
                    )
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create("S", "a"),
                        PT.create(
                            "S",
                            PT.create("S", "a"),
                            PT.create(
                                "S",
                                PT.create("S", "a"),
                                PT.create("S", "a")
                            )
                        )
                    ),
                    PT.create("S", "a")
                ),
                PT.create(
                    "S",
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
                PT.create(
                    "S",
                    PT.create("S", "a"),
                    PT.create(
                        "S",
                        PT.create(
                            "S",
                            PT.create("S", "a"),
                            PT.create("S", "a")
                        ),
                        PT.create(
                            "S",
                            PT.create("S", "a"),
                            PT.create("S", "a")
                        )
                    )
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create(
                            "S",
                            PT.create("S", "a"),
                            PT.create("S", "a")
                        ),
                        PT.create(
                            "S",
                            PT.create("S", "a"),
                            PT.create("S", "a")
                        )
                    ),
                    PT.create("S", "a")
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create(
                            "S",
                            PT.create("S", "a"),
                            PT.create("S", "a")
                        ),
                        PT.create("S", "a")
                    ),
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
                        PT.create(
                            "S",
                            PT.create("S", "a"),
                            PT.create(
                                "S",
                                PT.create("S", "a"),
                                PT.create("S", "a")
                            )
                        ),
                        PT.create("S", "a")
                    ),
                    PT.create("S", "a")
                ),
                PT.create(
                    "S",
                    PT.create("S", "a"),
                    PT.create(
                        "S",
                        PT.create(
                            "S",
                            PT.create("S", "a"),
                            PT.create(
                                "S",
                                PT.create("S", "a"),
                                PT.create("S", "a")
                            )
                        ),
                        PT.create("S", "a")
                    )
                ),
                PT.create(
                    "S",
                    PT.create("S", "a"),
                    PT.create(
                        "S",
                        PT.create(
                            "S",
                            PT.create(
                                "S",
                                PT.create("S", "a"),
                                PT.create("S", "a")
                            ),
                            PT.create("S", "a")
                        ),
                        PT.create("S", "a")
                    )
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create(
                            "S",
                            PT.create(
                                "S",
                                PT.create("S", "a"),
                                PT.create("S", "a")
                            ),
                            PT.create("S", "a")
                        ),
                        PT.create("S", "a")
                    ),
                    PT.create("S", "a")
                ),
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create("S", "a"),
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
                    PT.create("S", "a")
                ),
                PT.create(
                    "S",
                    PT.create("S", "a"),
                    PT.create(
                        "S",
                        PT.create("S", "a"),
                        PT.create(
                            "S",
                            PT.create(
                                "S",
                                PT.create("S", "a"),
                                PT.create("S", "a")
                            ),
                            PT.create("S", "a")
                        )
                    )
                )
            ),
            HashSet<ParseTree?>(p.parses("aaaaa"))
        )
    }
}