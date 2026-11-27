package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.Reduction
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * This class represents the `[p]` or `p?` operator (where p is an instance of [Rule]).
 * When parsing, the rule contained herein is optional (run zero times or once).
 */
class OptionalRule private constructor(
    hide: Boolean,
    red: Reduction,
    rule: Rule
) : RuleWithChild(hide, red, rule) {
    override fun parse(index: Int, runner: Gll) {
        val nodeKeyForOpt: ListenerKey = makeKey(index, this)
        runner.pushListener(
            makeKey(index, rule),
            runner.nodeListener(nodeKeyForOpt)
        )
        runner.pushSuccessMessageWithoutValue(nodeKeyForOpt, index)
    }

    override fun fullParse(index: Int, runner: Gll) {
        val thisNodeKey: ListenerKey = makeKey(index, this)
        runner.pushFullListener(makeKey(index, rule), runner.nodeListener(thisNodeKey))
        if (index == runner.tramp().text.length) {
            runner.pushSuccessMessageWithoutValue(thisNodeKey, index)
        } else {
            runner.fail(thisNodeKey, index, this, true)
        }
    }

    override fun withRule(rule: Rule): OptionalRule {
        return OptionalRule(isHiddenInResult, reduction, rule)
    }

    override fun withHideTag(hide: Boolean): OptionalRule {
        return if (isHiddenInResult == hide) this else OptionalRule(hide, reduction, rule)
    }

    override fun withReduction(red: Reduction): OptionalRule {
        return if (reduction === red) this else OptionalRule(isHiddenInResult, red, rule)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rule The rule.
         * @return A rule.
         */
        fun create(rule: Rule): Rule {
            if (rule is EpsilonTerm) return rule
            return OptionalRule(defaultHidden, defaultReduction, rule)
        }
    }
}
