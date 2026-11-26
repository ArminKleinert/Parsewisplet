package de.kleinert.parsewisp.tests.typical.parsing

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.testutil.Gr
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ConcatRuleTest {
    private fun string_a(): Rule {
        return StringTerm.createDirect("a", false)
    }

    @Test
    fun create() {
        Assertions.assertEquals(
            ConcatRule.create(listOf(string_a(), string_a(), string_a())),
            ConcatRule.create(listOf(string_a(), string_a(), string_a()))
        )
    }

    @Test
    fun createEmpty() {
        // Empty list as input -> Can create an Epsilon rule or something equivalent.
        // Restriction: Equivalent input -> equivalent output
        Assertions.assertEquals(
            ConcatRule.create(listOf()),
            ConcatRule.create(listOf())
        )

        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", EpsilonTerm.default)).parse(""),
            Parsewisp.parser(Gr.grammar("S", ConcatRule.create(listOf()))).parse("")
        )
    }

    @Test
    fun createOnlyEps() {
        // Only epsilons in concatenation -> Parse must give equivalent output to a single epsilon.
        // I.e. `cat(eps, eps) == eps` is not necessarily true, but they must give the same parse result.

        Assertions.assertEquals(
            ConcatRule.create(listOf(EpsilonTerm.default, EpsilonTerm.default)),
            ConcatRule.create(listOf(EpsilonTerm.default, EpsilonTerm.default))
        )

        Assertions.assertTrue(
            ConcatRule.createDirect(
                listOf(
                    EpsilonTerm.default,
                    EpsilonTerm.default
                )
            ) == ConcatRule.create(listOf(EpsilonTerm.default, EpsilonTerm.default))
                    ||
                    ConcatRule.createDirect(listOf(EpsilonTerm.default)) == ConcatRule.create(
                listOf(
                    EpsilonTerm.default,
                    EpsilonTerm.default
                )
            )
                    ||
                    EpsilonTerm.default == ConcatRule.create(
                listOf(
                    EpsilonTerm.default,
                    EpsilonTerm.default
                )
            )
        )

        // create(...) is allowed to remove any epsilon terms.
        val r = (ConcatRule.create(
            listOf(
                EpsilonTerm.default,
                string_a(),
                EpsilonTerm.default,
                string_a(),
                EpsilonTerm.default
            )
        ) as ConcatRule)
        Assertions.assertTrue(r.rules.size >= 2) // At least the two string terms.
        Assertions.assertTrue(r.rules.size <= 5) // At max all five rules.

        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", EpsilonTerm.default)).parse(""),
            Parsewisp.parser(
                Gr.grammar(
                    "S",
                    ConcatRule.create(listOf(EpsilonTerm.default, EpsilonTerm.default))
                )
            )
                .parse("")
        )
    }

    @Test
    fun createOnlyOneElement() {
        // Only one element in list -> Parser must give equivalent output to the single element
        Assertions.assertEquals(
            ConcatRule.create(listOf(string_a())),
            ConcatRule.create(listOf(string_a()))
        )

        Assertions.assertTrue(
            ConcatRule.createDirect(listOf(string_a())) == ConcatRule.create(listOf(string_a()))
                    ||
                    string_a() == ConcatRule.create(listOf(string_a()))
        )
        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", string_a())).parse(""),
            Parsewisp.parser(Gr.grammar("S", ConcatRule.create(listOf(string_a())))).parse("")
        )
    }

    @Test
    fun createWithNested() {
        // Nested concatenation -> Parser must give equivalent output to a flat concatenation
        val flat = ConcatRule.create(listOf(string_a(), string_a(), string_a(), string_a()))
        val nest1 = ConcatRule.create(
            listOf(
                ConcatRule.create(listOf(string_a(), string_a())),
                string_a(),
                string_a()
            )
        )
        val nest2 = ConcatRule.create(
            listOf(
                string_a(),
                ConcatRule.create(listOf(string_a(), string_a())),
                string_a()
            )
        )
        val nest3 = ConcatRule.create(
            listOf(
                string_a(),
                string_a(),
                ConcatRule.create(listOf(string_a(), string_a()))
            )
        )
        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", flat)).parse("aaaa"),
            Parsewisp.parser(Gr.grammar("S", nest1)).parse("aaaa")
        )
        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", flat)).parse("aaaa"),
            Parsewisp.parser(Gr.grammar("S", nest2)).parse("aaaa")
        )
        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", flat)).parse("aaaa"),
            Parsewisp.parser(Gr.grammar("S", nest3)).parse("aaaa")
        )
    }

    @Test
    fun createDirect() {
        Assertions.assertEquals(
            ConcatRule.createDirect(listOf(string_a(), string_a(), string_a())),
            ConcatRule.createDirect(listOf(string_a(), string_a(), string_a()))
        )
        Assertions.assertEquals(
            ConcatRule.create(listOf(string_a(), string_a(), string_a())),
            ConcatRule.createDirect(listOf(string_a(), string_a(), string_a()))
        )

        Assertions.assertEquals(
            ConcatRule.createDirect(listOf()),
            ConcatRule.createDirect(listOf())
        )

        Assertions.assertEquals(
            ConcatRule.createDirect(listOf(EpsilonTerm.default, EpsilonTerm.default)),
            ConcatRule.createDirect(listOf(EpsilonTerm.default, EpsilonTerm.default))
        )
        Assertions.assertNotEquals(
            ConcatRule.createDirect(listOf(EpsilonTerm.default)),
            ConcatRule.createDirect(listOf(EpsilonTerm.default, EpsilonTerm.default))
        )
        Assertions.assertEquals(
            2,
            ConcatRule.createDirect(listOf(EpsilonTerm.default, EpsilonTerm.default)).rules.size
        )

        val flat = ConcatRule.createDirect(listOf(string_a(), string_a(), string_a(), string_a()))
        val nest1 = ConcatRule.createDirect(
            listOf(
                ConcatRule.createDirect(listOf(string_a(), string_a())),
                string_a(),
                string_a()
            )
        )
        val nest2 = ConcatRule.createDirect(
            listOf(
                string_a(),
                ConcatRule.createDirect(listOf(string_a(), string_a())),
                string_a()
            )
        )
        val nest3 = ConcatRule.createDirect(
            listOf(
                string_a(),
                string_a(),
                ConcatRule.createDirect(listOf(string_a(), string_a()))
            )
        )
        Assertions.assertNotEquals(flat, nest1)
        Assertions.assertNotEquals(flat, nest2)
        Assertions.assertNotEquals(flat, nest3)
    }

    /**
     * The [ConcatRule.create] is allowed to flatten concatenations.
     * But it must not flatten if the sub-concatenation has been changed with [ConcatRule.withHideTag] or [ConcatRule.withReduction].
     */
    @Test
    fun createDoesNotMergeConcatIfRedOrHidden() {
        val c1 = ConcatRule.create(listOf(string_a(), string_a()))
            .withHideTag(true) // different hide-tag
        val c2 = ConcatRule.create(listOf(string_a(), string_a()))
            .withReduction(ReductionType.taggedTree(Sym.sym("A"))) // reduction has changed
        val c3 = ConcatRule.create(listOf(string_a(), string_a()))
            .withReduction(ReductionType.taggedTree(Sym.sym("B"))).withHideTag(true) // different reduction and hide-tag

        val cc = ConcatRule.create(listOf(c1, c2, c3))
        Assertions.assertEquals(
            ConcatRule.createDirect(listOf(c1, c2, c3)),
            cc
        )

        val p = Parsewisp.parser(Gr.grammar("S", cc))
        Assertions.assertEquals(
            PT.create(
                "S",  /*hidden,*/
                PT.create("A", "a", "a"),
                PT.create("B" /*, hidden*/)
            ),
            p.parse("aaaaaa")
        )
    }

    // Rule#parse() is usually called in Concatenations. Since nested concatenations are usually flattened,
    // this is only possible though using alternations or nonterms.
    @Test
    fun parse() {
        val p = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S",
                ConcatRule.create(
                    listOf(
                        NonTerminal.create(Sym.sym("A")),
                        string_a()
                    )
                ),  // Rule#fullParse for this concat.
                "A",
                ConcatRule.create(listOf(string_a(), string_a()))
            )
        ) // << Rule#parse for this one.
        Assertions.assertEquals(
            PT.create("S", PT.create("A", "a", "a"), "a"),
            p.parse("aaa")
        )
    }

    @Test
    fun parseError() {
        val p = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S",
                ConcatRule.create(
                    listOf(
                        NonTerminal.create(Sym.sym("A")),
                        string_a()
                    )
                ),  // Rule#fullParse for this concat.
                "A",
                ConcatRule.create(listOf(string_a(), string_a()))
            )
        ) // << Rule#parse for this one.
        Assertions.assertTrue(p.parse("").isFailure)
        Assertions.assertTrue(p.parse("baaa").isFailure)
    }

    @Test
    fun fullParseSingle() {
        // Nested concatenation -> Parser must give equivalent output to a flat concatenation
        val flat = ConcatRule.createDirect(listOf(string_a(), string_a(), string_a(), string_a()))
        val nest1 = ConcatRule.createDirect(
            listOf(
                ConcatRule.createDirect(listOf(string_a(), string_a())),
                string_a(),
                string_a()
            )
        )
        val nest2 = ConcatRule.createDirect(
            listOf(
                string_a(),
                ConcatRule.createDirect(listOf(string_a(), string_a())),
                string_a()
            )
        )
        val nest3 = ConcatRule.createDirect(
            listOf(
                string_a(),
                string_a(),
                ConcatRule.createDirect(listOf(string_a(), string_a()))
            )
        )
        Assertions.assertEquals(
            PT.create("S", "a", "a", "a", "a"),
            Parsewisp.parser(Gr.grammar("S", flat)).parse("aaaa")
        )
        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", flat)).parse("aaaa"),
            Parsewisp.parser(Gr.grammar("S", nest1)).parse("aaaa")
        )
        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", flat)).parse("aaaa"),
            Parsewisp.parser(Gr.grammar("S", nest2)).parse("aaaa")
        )
        Assertions.assertEquals(
            Parsewisp.parser(Gr.grammar("S", flat)).parse("aaaa"),
            Parsewisp.parser(Gr.grammar("S", nest3)).parse("aaaa")
        )
    }

    @Test
    fun fullParseError() {
        val p = Parsewisp.parser(Gr.grammar("S", string_a())) // Will use parse(...) for "a" and fullParse(...) for "b"
        Assertions.assertTrue(p.parse("").isFailure)
    }

    @Test
    fun getRules() {
        val flat = ConcatRule.createDirect(listOf(string_a(), string_a(), string_a(), string_a()))
        val nest1 = ConcatRule.createDirect(
            listOf(
                ConcatRule.createDirect(listOf(string_a(), string_a())),
                string_a(),
                string_a()
            )
        )
        val nest2 = ConcatRule.createDirect(
            listOf(
                string_a(),
                ConcatRule.createDirect(listOf(string_a(), string_a())),
                string_a()
            )
        )
        val nest3 = ConcatRule.createDirect(
            listOf(
                string_a(),
                string_a(),
                ConcatRule.createDirect(listOf(string_a(), string_a()))
            )
        )

        Assertions.assertEquals(listOf(string_a(), string_a(), string_a(), string_a()), flat.rules)
        Assertions.assertEquals(
            listOf(
                ConcatRule.createDirect(listOf(string_a(), string_a())),
                string_a(),
                string_a()
            ), nest1.rules
        )
        Assertions.assertEquals(
            listOf(
                string_a(),
                ConcatRule.createDirect(listOf(string_a(), string_a())),
                string_a()
            ), nest2.rules
        )
        Assertions.assertEquals(
            listOf(
                string_a(),
                string_a(),
                ConcatRule.createDirect(listOf(string_a(), string_a()))
            ), nest3.rules
        )
    }

    @Test
    fun withRules() {
        val i = listOf(string_a(), string_a(), string_a(), string_a())
        val r = ConcatRule.create(i) as ConcatRule

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
        val i = listOf(string_a(), string_a(), string_a(), string_a())
        val r = ConcatRule
            .create(i)
            .withHideTag(true)
            .withReduction(ReductionType.taggedTree(Sym.sym("A"))) as ConcatRule

        // Expected
        Assertions.assertEquals(i, r.rules)

        // Create a new rule, check whether hide and reduction are still unchanged.
        val ni = listOf(string_a(), string_a())
        val nr = r.withRules(ni)
        Assertions.assertEquals(ni, nr.rules)
        Assertions.assertEquals(r.isHiddenInResult, nr.isHiddenInResult)
        Assertions.assertEquals(r.reduction, nr.reduction)

        // Original left untouched.
        Assertions.assertEquals(i, r.rules)
    }

    @Test
    fun withHideTag() {
        val simpleRuleForTests = ConcatRule.createDirect(listOf(string_a(), string_a()))

        // By default, not hidden in result. May create a new object, but the output must be equal to the original.
        Assertions.assertEquals(simpleRuleForTests, simpleRuleForTests.withHideTag(false))
        Assertions.assertEquals(simpleRuleForTests, simpleRuleForTests.includeInResult())

        // Same input -> Equivalent output
        Assertions.assertEquals(simpleRuleForTests.withHideTag(false), simpleRuleForTests.withHideTag(false))
        Assertions.assertEquals(simpleRuleForTests.withHideTag(true), simpleRuleForTests.withHideTag(true))

        // Alternative names give same output for isHiddenInResult()
        Assertions.assertFalse(simpleRuleForTests.withHideTag(false).isHiddenInResult)
        Assertions.assertFalse(simpleRuleForTests.includeInResult().isHiddenInResult)
        Assertions.assertTrue(simpleRuleForTests.withHideTag(true).isHiddenInResult)
        Assertions.assertTrue(simpleRuleForTests.hideInResult().isHiddenInResult)

        // Alternative names give equivalent output
        Assertions.assertEquals(simpleRuleForTests.withHideTag(false), simpleRuleForTests.includeInResult())
        Assertions.assertEquals(simpleRuleForTests.withHideTag(true), simpleRuleForTests.hideInResult())
    }

    @Test
    fun withHideTagParse() {
        val simpleRuleForTests = ConcatRule.createDirect(listOf(string_a(), string_a()))

        // By default, not hidden in output
        val pDefault = Parsewisp.parser(
            Gr.grammar("S", simpleRuleForTests)
        )
        Assertions.assertEquals(PT.create("S", "a", "a"), pDefault.parse("aa"))

        // With explicit show, also not hidden
        val pShow = Parsewisp.parser(
            Gr.grammar("S", simpleRuleForTests.withHideTag(false))
        )
        Assertions.assertEquals(PT.create("S", "a", "a"), pShow.parse("aa"))

        // With explicit hide
        val pHide = Parsewisp.parser(
            Gr.grammar("S", simpleRuleForTests.withHideTag(true))
        )
        Assertions.assertEquals(PT.create("S"), pHide.parse("aa"))
    }

    @Test
    fun withReduction() {
        val simpleRuleForTests = ConcatRule.createDirect(listOf(string_a(), string_a()))

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
        Assertions.assertEquals(PT.create("S", PT.create("A", "a", "a")), pDefault.parse("aa"))

        // Merge upwards
        val pMerge = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", simpleRuleForTests.withReduction(ReductionType.merging())
            )
        )
        Assertions.assertEquals(PT.create("S", "a", "a"), pMerge.parse("aa"))

        // Add to parse tree with potentially different name (Top-level)
        val pMerge2 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", simpleRuleForTests.withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("B", "a", "a"), pMerge2.parse("aa"))

        // Add to parse tree with potentially different name (Nested)
        val pMerge3 = Parsewisp.parser(
            Gr.grammarS(
                "S",
                "S", NonTerminal.create(Sym.sym("A")),
                "A", simpleRuleForTests.withReduction(ReductionType.taggedTree(Sym.sym("B")))
            )
        )
        Assertions.assertEquals(PT.create("S", PT.create("B", "a", "a")), pMerge3.parse("aa"))

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
            PT.create("S", PT.create("A", PT.create("B", "a", "a"), "b")),
            pMerge4.parse("aab")
        )
    }

    @Test
    fun withReductionMergeUp() {
    }

    @Test
    fun withReductionTaggedTree() {
    }

    @Test
    fun withReductionNotDirectRhs() {
    }
}
