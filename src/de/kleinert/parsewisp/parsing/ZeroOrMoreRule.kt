package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.collections.FlatResultSeq.Companion.make
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * "Zero or more" repetition. Represents a production which repeatedly ties to match an input. E.g. `P*` matches zero or more.
 *
 *
 * Notation: `{rule}` or `rule*`
 *
 * @since 0.9.7
 */
class ZeroOrMoreRule private constructor(
    hide: Boolean,
    red: ReductionType,
    rule: Rule
) : RuleWithChild(hide, red, rule) {
    override fun parse(index: Int, runner: Gll) {
        val nodeKeyForStar: ListenerKey = makeKey(index, this)
        runner.pushListener(
            makeKey(index, rule),
            OnceOrMoreRule.plusListener(make(), rule, index, nodeKeyForStar, runner)
        )
        runner.pushSuccessMessageWithoutValue(nodeKeyForStar, index)
    }

    override fun fullParse(index: Int, runner: Gll) {
        val nodeKeyForStar: ListenerKey = makeKey(index, this)
        if (index == runner.tramp().text.length) {
            runner.pushSuccessMessageWithoutValue(nodeKeyForStar, index)
            return
        }
        runner.pushListener(
            makeKey(index, rule),
            OnceOrMoreRule.plusFullListener(make(), rule, index, nodeKeyForStar, runner)
        )
    }

    override fun withRule(rule: Rule): ZeroOrMoreRule {
        return ZeroOrMoreRule(isHiddenInResult, reduction, rule)
    }

    override fun withHideTag(hide: Boolean): ZeroOrMoreRule {
        return if (isHiddenInResult == hide) this else ZeroOrMoreRule(hide, reduction, rule)
    }

    override fun withReduction(red: ReductionType): ZeroOrMoreRule {
        return if (reduction === red) this else ZeroOrMoreRule(isHiddenInResult, red, rule)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rule The [Rule] to match repeatedly.
         * @return A rule.
         * @since 0.9.7
         */
        fun create(rule: Rule): Rule {
            if (rule is EpsilonTerm) return rule
            return ZeroOrMoreRule(defaultHidden, defaultReductionType, rule)
        }
    }
}
