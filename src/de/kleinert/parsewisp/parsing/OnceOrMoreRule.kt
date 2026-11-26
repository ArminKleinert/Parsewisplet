package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.collections.FlatResultSeq
import de.kleinert.parsewisp.collections.FlatResultSeq.Companion.make
import de.kleinert.parsewisp.functions.Listener
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.result.success.ParseMessage
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * Represents a "once or more" parse. That is the `p+` operator (where p is an instance of [Rule]).
 */
class OnceOrMoreRule private constructor(
    hide: Boolean,
    red: ReductionType,
    rule: Rule
) : RuleWithChild(hide, red, rule) {
    override fun parse(index: Int, runner: Gll) {
        runner.pushListener(
            makeKey(index, rule),
            plusListener(make(), rule, index, makeKey(index, this), runner)
        )
    }

    override fun fullParse(index: Int, runner: Gll) {
        runner.pushListener(
            makeKey(index, rule),
            plusFullListener(make(), rule, index, makeKey(index, this), runner)
        )
    }

    override fun withRule(rule: Rule): OnceOrMoreRule {
        return OnceOrMoreRule(isHiddenInResult, reduction, rule)
    }

    override fun withHideTag(hide: Boolean): OnceOrMoreRule {
        return if (isHiddenInResult == hide) this else OnceOrMoreRule(hide, reduction, rule)
    }

    override fun withReduction(red: ReductionType): OnceOrMoreRule {
        return if (reduction === red) this else OnceOrMoreRule(isHiddenInResult, red, rule)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rule The [Rule] to match repeatedly.
         * @return A rule.
         */
        fun create(rule: Rule): Rule {
            if (rule is EpsilonTerm) return rule
            return OnceOrMoreRule(defaultHidden, defaultReductionType, rule)
        }

        fun plusListener(
            resultsSoFar: FlatResultSeq,
            rule: Rule,
            prevIndex: Int,
            nodeKey: ListenerKey,
            runner: Gll
        ): Listener {
            return object : Listener {
                override fun invoke(result: ParseMessage) {
                    val continueIndex = result.index()
                    val newResultsSoFar = resultsSoFar.appendOrConcat(result.result)
                    if (continueIndex == prevIndex) {
                        if (resultsSoFar.isEmpty) {
                            runner.pushSuccessMessage(nodeKey, newResultsSoFar, continueIndex)
                        }
                        return
                    }
                    runner.pushListener(
                        ListenerKey(continueIndex, rule),
                        plusListener(newResultsSoFar, rule, continueIndex, nodeKey, runner)
                    )
                    runner.pushSuccessMessage(nodeKey, newResultsSoFar, continueIndex)
                }
            }
        }

        fun plusFullListener(
            resultsSoFar: FlatResultSeq,
            rule: Rule,
            prevIndex: Int,
            nodeKey: ListenerKey,
            runner: Gll
        ): Listener {
            return object : Listener {
                override fun invoke(result: ParseMessage) {
                    val continueIndex = result.index()
                    val newResultsSoFar = resultsSoFar.appendOrConcat(result.result)
                    if (continueIndex == prevIndex) {
                        if (resultsSoFar.isEmpty) {
                            runner.pushSuccessMessage(nodeKey, newResultsSoFar, continueIndex)
                            //runner.pushSuccessMessageWithoutValue(nodeKey, continueIndex);
                        }
                        return
                    }
                    if (continueIndex == runner.tramp().text.length) {
                        runner.pushSuccessMessage(nodeKey, newResultsSoFar, continueIndex)
                    } else {
                        runner.pushListener(
                            ListenerKey(continueIndex, rule),
                            plusFullListener(newResultsSoFar, rule, continueIndex, nodeKey, runner)
                        )
                    }
                }
            }
        }
    }
}
