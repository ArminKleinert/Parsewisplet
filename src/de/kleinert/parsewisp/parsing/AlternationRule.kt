package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.reduction.ReductionType

/**
 * A class representing a choice or alternation. That is the `(p1 | p2)` operator in EBNF (where p1 and p2 are instances of [Rule]).
 *
 *
 * Notation: `rule1 | rule2`
 *
 *
 * Example
 * <pre>
 * `// Accepts the language {"a", "b", "ab"}     var p = Parsewisp.parser("S := 'a' | 'b' | 'ab'");     println(p.parse("a"));  // [:S, a]     println(p.parse("b"));  // [:S, b]     println(p.parse("ab")); // [:S, ab] `
</pre> *
 *
 *
 * Alternatively, the [ParserCreationOptions] class allows an alternative notation for defining alternations:
 * <pre>
 * `var opts = Parsewisp.ParserCreationOptions             .getDefault()             .withRedefinitionOption(Grammar.RedefinitionOption.CHOICE);     var p = Parsewisp.parser("""             S : 'a'             S : 'b'             S : 'ab'             """, opts);     println(p.parse("a"));  // [:S, a]     println(p.parse("b"));  // [:S, b]     println(p.parse("ab")); // [:S, ab] `
</pre> *
 *
 * @since 0.9.7
 */
class AlternationRule private constructor(
    hide: Boolean,
    red: ReductionType,
    rules: List<Rule>
) : RuleWithManyChildren(hide, red, rules) {
    override fun parse(index: Int, runner: Gll) {
        for (rule in rules) {
            runner.pushListener(
                makeKey(index, rule),
                runner.nodeListener(makeKey(index, this))
            )
        }
    }

    override fun fullParse(index: Int, runner: Gll) {
        for (rule in rules) {
            runner.pushFullListener(
                makeKey(index, rule),
                runner.nodeListener(makeKey(index, this))
            )
        }
    }

    override fun withHideTag(hide: Boolean): AlternationRule {
        return if (isHiddenInResult == hide) this else AlternationRule(hide, reduction, rules)
    }

    override fun withReduction(red: ReductionType): AlternationRule {
        return if (reduction === red) this else AlternationRule(isHiddenInResult, red, rules)
    }

    override fun withRules(rules: List<Rule>): AlternationRule {
        return AlternationRule(isHiddenInResult, reduction, rules)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rules The wrapped rules.
         * @return A rule.
         * @since 0.9.7
         */
        fun create(rules: List<Rule>): Rule {
            if (rules.isEmpty()) return EpsilonTerm.default
            if (rules.size == 1) return rules.get(0)

            val uniques = mutableSetOf<Rule>()
            val compressedRules = mutableListOf<Rule>()

            for (rule in rules) {
                if (!uniques.add(rule)) {
                    // continue;
                } else if (rule is AlternationRule && !rule.isHiddenInResult1() && rule.red == defaultReductionType) {
                    compressedRules.addAll(rule.rules)
                } else {
                    compressedRules.add(rule)
                }
            }

            if (compressedRules.size == 1) return rules.get(0)

            return AlternationRule(
                defaultHidden, defaultReductionType,
                compressedRules
            )
        }

        /**
         * Like [.create] except the input
         * list is distinct (each rule in the list occurs exactly once).
         * Use this method only if you are sure that the rules are distinct.
         *
         * @param rules The wrapped rules.
         * @return A rule.
         * @since 0.9.7
         */
        fun altUnsafe(
            rules: List<Rule>
        ): AlternationRule {
            return AlternationRule(defaultHidden, defaultReductionType, rules)
        }
    }
}
