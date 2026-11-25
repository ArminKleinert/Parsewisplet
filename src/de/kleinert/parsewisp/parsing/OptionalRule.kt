package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * This class represents the `[p]` or `p?` operator (where p is an instance of [Rule]).
 * When parsing, the rule contained herein is optional (run zero times or once).
 *
 * @since 0.9.7
 */
class OptionalRule private constructor(
    hide: Boolean,
    red: ReductionType,
    rule: Rule
) : RuleWithChild(hide, red, rule) {
    override fun parse(index: Int, runner: Gll) {
        val nodeKeyForOpt: ListenerKey = Rule.Companion.makeKey(index, this)
        runner.pushListener(
            Rule.Companion.makeKey(index, rule),
            runner.nodeListener(nodeKeyForOpt)
        )
        runner.pushSuccessMessageWithoutValue(nodeKeyForOpt, index)
    }

    override fun fullParse(index: Int, runner: Gll) {
        val thisNodeKey: ListenerKey = Rule.Companion.makeKey(index, this)
        runner.pushFullListener(Rule.Companion.makeKey(index, rule), runner.nodeListener(thisNodeKey))
        if (index == runner.tramp().getText1().length) {
            runner.pushSuccessMessageWithoutValue(thisNodeKey, index)
        } else {
            runner.fail(thisNodeKey, index, this, true)
        }
    }

    override fun withRule(rule: Rule): OptionalRule {
        return OptionalRule(hide, red, rule)
    }

    override fun withHideTag(hide: Boolean): OptionalRule {
        return if (isHiddenInResult1() == hide) this else OptionalRule(hide, red, rule)
    }

    override fun withReduction(red: ReductionType): OptionalRule {
        return if (getReduction1() === red) this else OptionalRule(hide, red, rule)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rule The rule.
         * @return A rule.
         * @since 0.9.7
         */
        fun create(rule: Rule): Rule {
            if (rule is EpsilonTerm) return rule
            return OptionalRule(Rule.Companion.defaultHidden, Rule.Companion.defaultReductionType, rule)
        }
    }
}
