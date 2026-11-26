package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * This class represents the ABNF "lookahead" operator `&p` (where p is an instance of [Rule]).
 */
class LookaheadRule private constructor(
    hide: Boolean,
    red: ReductionType,
    rule: Rule
) : RuleWithChild(hide, red, rule) {
    override fun parse(index: Int, runner: Gll) {
        val nodeKey: ListenerKey = makeKey(index, this)
        runner.pushListener(
            makeKey(index, rule),
            { runner.pushSuccessMessageWithoutValue(nodeKey, index) })
    }

    override fun fullParse(index: Int, runner: Gll) {
        if (index == runner.tramp().text.length) {
            parse(index, runner)
        } else if (!(rule is EpsilonTerm || rule is EOFTerm)) {
            runner.fail(
                makeKey(index, this),
                index, this, false
            )
        }
    }

    override fun withHideTag(hide: Boolean): LookaheadRule {
        return if (isHiddenInResult == hide) this else LookaheadRule(hide, reduction, rule)
    }

    override fun withReduction(red: ReductionType): LookaheadRule {
        return if (reduction === red) this else LookaheadRule(isHiddenInResult, red, rule)
    }

    override fun withRule(rule: Rule): LookaheadRule {
        return LookaheadRule(isHiddenInResult, reduction, rule)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rule The inner rule.
         * @return A rule.
         */
        fun create(rule: Rule): Rule {
            if (rule is EpsilonTerm) return rule
            return LookaheadRule(defaultHidden, defaultReductionType, rule)
        }
    }
}
