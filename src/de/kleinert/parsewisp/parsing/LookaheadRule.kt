package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.result.success.ParseMessage
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * This class represents the ABNF "lookahead" operator `&p` (where p is an instance of [Rule]).
 *
 * @since 0.9.7
 */
class LookaheadRule private constructor(
    hide: Boolean,
    red: ReductionType,
    rule: Rule
) : RuleWithChild(hide, red, rule) {
    override fun parse(index: Int, runner: Gll) {
        val nodeKey: ListenerKey = Rule.Companion.makeKey(index, this)
        runner.pushListener(
            Rule.Companion.makeKey(index, rule),
            { ignored: ParseMessage? -> runner.pushSuccessMessageWithoutValue(nodeKey, index) })
    }

    override fun fullParse(index: Int, runner: Gll) {
        if (index == runner.tramp().getText1().length) {
            parse(index, runner)
        } else if (!(rule is EpsilonTerm || rule is EOFTerm)) {
            runner.fail(
                Rule.Companion.makeKey(index, this),
                index, this, false
            )
        }
    }

    override fun withHideTag(hide: Boolean): LookaheadRule {
        return if (isHiddenInResult1() == hide) this else LookaheadRule(hide, red, rule)
    }

    override fun withReduction(red: ReductionType): LookaheadRule {
        return if (getReduction1() === red) this else LookaheadRule(hide, red, rule)
    }

    override fun withRule(rule: Rule): LookaheadRule {
        return LookaheadRule(hide, red, rule)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rule The inner rule.
         * @return A rule.
         * @since 0.9.7
         */
        fun create(rule: Rule): Rule {
            if (rule is EpsilonTerm) return rule
            return LookaheadRule(Rule.Companion.defaultHidden, Rule.Companion.defaultReductionType, rule)
        }
    }
}
