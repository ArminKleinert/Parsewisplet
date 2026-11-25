package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.functions.Listener
import de.kleinert.parsewisp.functions.NegativeListener
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.result.success.ParseMessage
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * Represents a negative lookahead. Written `!P`.
 *
 *
 * Example: The production `S := !'a' ('a'|'b')+` matches any string of 'a' and 'b' which does NOT start with 'a'.
 *
 * @since 0.9.7
 */
class NegativeLookaheadRule private constructor(
    hide: Boolean,
    red: ReductionType,
    rule: Rule
) : RuleWithChild(hide, red, rule) {
    private fun resultExists_Q(
        runner: Gll,
        nodeKey: ListenerKey
    ): Boolean {
        val node = runner.tramp().getNode(nodeKey)

        if (node == null) return false

        return !node.fullResults.isEmpty() || !node.results.isEmpty()
    }

    override fun parse(index: Int, runner: Gll) {
        val nodeKey: ListenerKey = Rule.Companion.makeKey(index, rule)
        val thisNodeKey: ListenerKey = Rule.Companion.makeKey(index, this)

        if (resultExists_Q(runner, nodeKey)) {
            runner.fail(thisNodeKey, index, this, false)
            return
        }

        runner.pushListener(nodeKey, Listener { ignored: ParseMessage? ->
            runner.fail(
                thisNodeKey, index, this, false
            )
        })

        runner.pushNegativeListener(nodeKey, NegativeListener {
            if (!resultExists_Q(runner, nodeKey)) {
                runner.pushSuccessMessageWithoutValue(thisNodeKey, index)
            }
        })
    }

    override fun fullParse(index: Int, runner: Gll) {
        parse(index, runner)
    }

    override fun withRule(rule: Rule): NegativeLookaheadRule {
        return NegativeLookaheadRule(hide, red, rule)
    }

    override fun withHideTag(hide: Boolean): NegativeLookaheadRule {
        return if (isHiddenInResult1() == hide) this else NegativeLookaheadRule(hide, red, rule)
    }

    override fun withReduction(red: ReductionType): NegativeLookaheadRule {
        return if (getReduction1() === red) this else NegativeLookaheadRule(hide, red, rule)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rule The thing to avoid.
         * @return A rule.
         * @since 0.9.7
         */
        fun create(rule: Rule): Rule {
            return NegativeLookaheadRule(Rule.Companion.defaultHidden, Rule.Companion.defaultReductionType, rule)
        }
    }
}
