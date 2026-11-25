package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.List
import java.util.Set

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
            AlternationRule.create(List.of<Rule>(string_a(), string_b())),
            AlternationRule.create(List.of<Rule>(string_a(), string_b()))
        )
    }

    @Test
    fun createEmpty() {
        // create(...) called with an empty input is free to create an Epsilon or something equivalent instead.
        Assertions.assertEquals(
            AlternationRule.create(mutableListOf<Rule>()),
            AlternationRule.create(mutableListOf<Rule>())
        )

        Assertions.assertEquals(
            parser(Gr.grammar("S", EpsilonTerm.getDefault())).parse(""),
            parser(Gr.grammar("S", AlternationRule.create(mutableListOf<Rule>()))).parse("")
        )
    }

    @Test
    fun createOnlyOne() {
        // create(...) called with only one element in the input list, it may return only that one element.
        // This behavior is not mandatory.
        Assertions.assertEquals(
            AlternationRule.create(List.of<Rule>(string_a())),
            AlternationRule.create(List.of<Rule>(string_a()))
        )

        Assertions.assertEquals(
            parser(Gr.grammar("S", string_a())).parse("a"),
            parser(Gr.grammar("S", AlternationRule.create(List.of<Rule>(string_a())))).parse("a")
        )
    }

    @Test
    fun createManySimilar() {
        // If an element in the input is repeated, create(...) can (and should) make the inputs distinct.
        val l = List.of<Rule?>(
            string_a(),
            string_b(),
            string_a(),
            string_b(),
            string_a(),
            string_b(),
            string_a(),
            string_b()
        )
        val lDistinct = l.stream().distinct().toList()
        Assertions.assertTrue(
            AlternationRule.altUnsafe(lDistinct) == AlternationRule.create(l)
                    || AlternationRule.create(lDistinct) == AlternationRule.create(l)
        )

        Assertions.assertEquals(
            parser(Gr.grammar("S", AlternationRule.create(lDistinct))).parses("a"),
            parser(Gr.grammar("S", AlternationRule.create(l))).parses("a")
        )
    }

    @Test
    fun altUnsafe() {
        Assertions.assertEquals(
            AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a(), string_a())),
            AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a(), string_a()))
        )
        Assertions.assertEquals(
            AlternationRule.create(List.of<Rule>(string_a(), string_b())),
            AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_b()))
        )

        Assertions.assertEquals(
            AlternationRule.altUnsafe(mutableListOf<Rule>()),
            AlternationRule.altUnsafe(mutableListOf<Rule>())
        )

        Assertions.assertEquals(
            AlternationRule.altUnsafe(List.of<Rule>(EpsilonTerm.getDefault(), EpsilonTerm.getDefault())),
            AlternationRule.altUnsafe(List.of<Rule>(EpsilonTerm.getDefault(), EpsilonTerm.getDefault()))
        )
        Assertions.assertNotEquals(
            AlternationRule.altUnsafe(List.of<Rule>(EpsilonTerm.getDefault())),
            AlternationRule.altUnsafe(List.of<Rule>(EpsilonTerm.getDefault(), EpsilonTerm.getDefault()))
        )
        Assertions.assertEquals(
            2,
            AlternationRule.altUnsafe(List.of<Rule>(EpsilonTerm.getDefault(), EpsilonTerm.getDefault())).getRules().size
        )

        val flat = AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a(), string_a(), string_a()))
        val nest1 = AlternationRule.altUnsafe(
            List.of<Rule>(
                AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a())),
                string_a(),
                string_a()
            )
        )
        val nest2 = AlternationRule.altUnsafe(
            List.of<Rule>(
                string_a(),
                AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a())),
                string_a()
            )
        )
        val nest3 = AlternationRule.altUnsafe(
            List.of<Rule>(
                string_a(),
                string_a(),
                AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a()))
            )
        )
        Assertions.assertNotEquals(flat, nest1)
        Assertions.assertNotEquals(flat, nest2)
        Assertions.assertNotEquals(flat, nest3)
    }

    @Test
    fun parse() {
        val p_ab = parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    List.of<Rule>(
                        AlternationRule.create(List.of<Rule>(string_a(), string_b())),
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
        val p_a = parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    List.of<Rule>(
                        AlternationRule.create(
                            List.of<Rule>(
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
            List.of<ParseTree?>(PT.create("S", "a", "a")),
            p_a.parses("aa")
        )
    }

    @Test
    fun parseError() {
        val p_ab = parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    List.of<Rule>(
                        AlternationRule.create(List.of<Rule>(string_a(), string_b())),
                        string_a()
                    )
                )
            )
        )
        Assertions.assertTrue(p_ab.parse("ca").isFailure())
    }

    @Test
    fun fullParseSingle() {
        val p_ab = parser(Gr.grammar("S", AlternationRule.create(List.of<Rule>(string_a(), string_b()))))
        Assertions.assertEquals(PT.create("S", "a"), p_ab.parse("a"))
        Assertions.assertEquals(PT.create("S", "b"), p_ab.parse("b"))
    }

    @Test
    fun fullParseConcat() {
        val p_ab = parser(
            Gr.grammar(
                "S",
                ConcatRule.create(
                    List.of<Rule>(
                        string_a(),
                        AlternationRule.create(List.of<Rule>(string_a(), string_b()))
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
        val p_a = parser(Gr.grammar("S", AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a(), string_a()))))
        Assertions.assertEquals(
            List.of<ParseTree?>(PT.create("S", "a")),
            p_a.parses("a")
        )
    }

    @Test
    fun fullParseError() {
        val p_ab = parser(Gr.grammar("S", AlternationRule.create(List.of<Rule>(string_a(), string_b()))))
        Assertions.assertTrue(p_ab.parse("c").isFailure())
    }

    @Test
    fun getRules() {
        val flat = AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a(), string_a(), string_a()))
        val nest1 = AlternationRule.altUnsafe(
            List.of<Rule>(
                AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a())),
                string_a(),
                string_a()
            )
        )
        val nest2 = AlternationRule.altUnsafe(
            List.of<Rule>(
                string_a(),
                AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a())),
                string_a()
            )
        )
        val nest3 = AlternationRule.altUnsafe(
            List.of<Rule>(
                string_a(),
                string_a(),
                AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a()))
            )
        )

        Assertions.assertEquals(List.of<Rule?>(string_a(), string_a(), string_a(), string_a()), flat.getRules())
        Assertions.assertEquals(
            List.of<Rule?>(
                AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a())),
                string_a(),
                string_a()
            ), nest1.getRules()
        )
        Assertions.assertEquals(
            List.of<Rule?>(
                string_a(),
                AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a())),
                string_a()
            ), nest2.getRules()
        )
        Assertions.assertEquals(
            List.of<Rule?>(
                string_a(),
                string_a(),
                AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_a()))
            ), nest3.getRules()
        )
    }

    @Test
    fun withRules() {
        val i = List.of<Rule>(string_a(), string_b())
        val r = AlternationRule.altUnsafe(i)

        // Expected
        Assertions.assertEquals(i, r.getRules())

        val ni = List.of<Rule>(string_a(), StringTerm.createDirect("c", false))
        val nr = r.withRules(ni)

        // The new rule.
        Assertions.assertEquals(ni, nr.getRules())

        // Original left untouched.
        Assertions.assertEquals(i, r.getRules())
    }

    @Test
    fun withRulesPreservesHideAndRed() {
        val i = List.of<Rule>(string_a(), string_b())
        val r = AlternationRule
            .altUnsafe(i)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(sym("A")))

        // Expected
        Assertions.assertEquals(i, r.getRules())

        // Create a new rule, check whether hide and reduction are still unchanged.
        val ni = List.of<Rule>(string_a(), StringTerm.createDirect("c", false))
        val nr = r.withRules(ni)
        Assertions.assertEquals(ni, nr.getRules())
        Assertions.assertEquals(r.isHiddenInResult(), nr.isHiddenInResult())
        Assertions.assertEquals(r.getReduction(), nr.getReduction())

        // Original left untouched.
        Assertions.assertEquals(i, r.getRules())
    }

    @Test
    fun withHideTag() {
        val simpleRuleForTests = AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_b()))

        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(simpleRuleForTests, simpleRuleForTests.withHideTag(false))
        Assertions.assertEquals(simpleRuleForTests, simpleRuleForTests.includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(simpleRuleForTests.withHideTag(false), simpleRuleForTests.withHideTag(false))
        Assertions.assertEquals(simpleRuleForTests.withHideTag(true), simpleRuleForTests.withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(simpleRuleForTests.withHideTag(false).isHiddenInResult())
        Assertions.assertFalse(simpleRuleForTests.includeInResult().isHiddenInResult())
        Assertions.assertTrue(simpleRuleForTests.withHideTag(true).isHiddenInResult())
        Assertions.assertTrue(simpleRuleForTests.hideInResult().isHiddenInResult())

        // Alternative names give equivalent output
        Assertions.assertEquals(simpleRuleForTests.withHideTag(false), simpleRuleForTests.includeInResult())
        Assertions.assertEquals(simpleRuleForTests.withHideTag(true), simpleRuleForTests.hideInResult())

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
        val simpleRuleForTests = AlternationRule.altUnsafe(List.of<Rule>(string_a(), string_b()))

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

        // Merge upwards
        val pMerge = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", simpleRuleForTests.withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "a"), pMerge.parse("a"))

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

        // Add parse tree from a rule which doesn't appear in the grammar.
        val pMerge4 = parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(sym("A")),
                "A", ConcatRule.create(
                    List.of<Rule>(
                        simpleRuleForTests.withReduction(ReductionType.taggedTree(sym("B"))),
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
        val p = parser(
            Gr.grammar(
                "S", AlternationRule.altUnsafe(
                    List.of<Rule>(
                        NonTerminal.create(sym("A")),
                        NonTerminal.create(sym("B")),
                        NonTerminal.create(sym("C")),
                        NonTerminal.create(sym("D")),
                        NonTerminal.create(sym("E"))
                    )
                ),
                "A", EpsilonTerm.getDefault(),
                "B", EpsilonTerm.getDefault(),
                "C", EpsilonTerm.getDefault(),
                "D", EpsilonTerm.getDefault(),
                "E", EpsilonTerm.getDefault()
            )
        )

        Assertions.assertEquals(
            Set.of<ParseTree?>(
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
        val p = parser(
            Gr.grammar(
                "S", AlternationRule.altUnsafe(
                    List.of<Rule>(
                        ConcatRule.create(
                            List.of<Rule>(
                                string_a(), NonTerminal.create(sym("S")), string_a()
                            )
                        ),
                        string_a()
                    )
                )
            )
        )

        Assertions.assertEquals(
            Set.of<ParseTree?>(
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
        val p = parser(
            Gr.grammar(
                "S", AlternationRule.altUnsafe(
                    List.of<Rule>(
                        string_a(),
                        ConcatRule.create(
                            List.of<Rule>(
                                NonTerminal.create(sym("S")), NonTerminal.create(sym("S"))
                            )
                        )
                    )
                )
            )
        )

        // Results for parsing "aaa"
        Assertions.assertEquals(
            Set.of<ParseTree?>(
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
            Set.of<ParseTree?>(
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