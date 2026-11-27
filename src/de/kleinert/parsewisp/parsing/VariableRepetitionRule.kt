package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.collections.FlatResultSeq
import de.kleinert.parsewisp.collections.FlatResultSeq.Companion.make
import de.kleinert.parsewisp.functions.Listener
import de.kleinert.parsewisp.reduction.Reduction
import de.kleinert.parsewisp.result.success.ParseMessage
import de.kleinert.parsewisp.trampoline.ListenerKey
import kotlin.math.max
import kotlin.math.min

/**
 * A class representing the ABNF counted repetition operator.
 * There are multiple variants, depending on the syntax (where n and m are integers, and p is an instance of [Rule]):
 *
 *  * Repeat a minimum of `n` times and a maximum of `m` times: `p{n,m}`
 *  * Repeat a minimum of `n` times: `p{n,}`
 *  * Repeat a maximum of `m` times: `p{,m}`
 *  * Repeat exactly of `n` times: `p{n}`
 */
class VariableRepetitionRule private constructor(
    hide: Boolean,
    red: Reduction,
    rule: Rule,
    /**
     * Return the minimum number of repetitions.
     *
     * @return minimum
     */
    val min: Int,
    /**
     * Return the maximum number of repetitions.
     *
     * @return maximum
     */
    val max: Int
) : RuleWithChild(hide, red, rule) {
    override fun parse(index: Int, runner: Gll) {
        if (min == 0) {
            runner.pushSuccessMessageWithoutValue(makeKey(index, this), index)
        }
        if (max == 0) {
            return
        }
        runner.pushListener(
            makeKey(index, rule),
            repListener(make(), 0, this, makeKey(index, this), runner, index)
        )
    }

    override fun fullParse(index: Int, runner: Gll) {
        if (min == 0) {
            runner.pushSuccessMessageWithoutValue(makeKey(index, this), index)
        }
        if (max == 0) {
            return
        }
        runner.pushListener(
            makeKey(index, rule),
            repFullListener(make(), 0, rule, min, max, makeKey(index, this), runner)
        )
    }

    private fun repListener(
        resultsSoFar: FlatResultSeq,
        nResultsSoFar: Int,
        repRule: VariableRepetitionRule,
        nodeKey: ListenerKey,
        runner: Gll,
        prevIndex: Int
    ): Listener {
        return object : Listener {
            override fun invoke(result: ParseMessage) {
                val parsedResult = result.result
                val continueIndex = result.index()
                val newResultsSoFar = resultsSoFar.appendOrConcat(parsedResult)
                val newNResultsSoFar = nResultsSoFar + 1

                val minReached = repRule.min <= newNResultsSoFar // Collected the minimum number of parses

                if (minReached && newNResultsSoFar <= repRule.max) {
                    runner.pushSuccessMessage(nodeKey, newResultsSoFar, continueIndex)
                }

                if (runner.stopEarlyOnEmptyRepetition
                    && minReached && continueIndex == prevIndex
                ) {
                    if (resultsSoFar.isEmpty) {
                        runner.pushSuccessMessage(nodeKey, newResultsSoFar, continueIndex)
                    }
                    return
                }
                if (newNResultsSoFar < repRule.max) {
                    runner.pushListener(
                        makeKey(continueIndex, repRule.rule),
                        repListener(newResultsSoFar, newNResultsSoFar, repRule, nodeKey, runner, continueIndex)
                    )
                }
            }
        }
    }

    private fun repFullListener(
        resultsSoFar: FlatResultSeq,
        nResultsSoFar: Int,
        rule: Rule,
        minimum: Int,
        maximum: Int,
        nodeKey: ListenerKey,
        runner: Gll
    ): Listener {
        return object : Listener {
            override fun invoke(result: ParseMessage) {
                val parsedResult = result.result
                val continueIndex = result.index()
                val newResultsSoFar = resultsSoFar.appendOrConcat(parsedResult)
                val newNResultsSoFar = nResultsSoFar + 1
                if (continueIndex == runner.tramp().text.length) {
                    if (newNResultsSoFar in minimum..maximum) {
                        runner.pushSuccessMessage(nodeKey, newResultsSoFar, continueIndex)
                    } else {
                        runner.fail(nodeKey, continueIndex, this@VariableRepetitionRule, true)
                    }
                } else {
                    if (newNResultsSoFar < maximum) {
                        val listener = repFullListener(
                            newResultsSoFar, newNResultsSoFar,
                            rule, minimum, maximum, nodeKey, runner
                        )
                        runner.pushListener(makeKey(continueIndex, rule), listener)
                    } else {
                        runner.fail(nodeKey, continueIndex, this@VariableRepetitionRule, true)
                    }
                }
            }
        }
    }

    override fun withRule(rule: Rule): VariableRepetitionRule {
        return VariableRepetitionRule(isHiddenInResult, reduction, rule, min, max)
    }

    override fun withHideTag(hide: Boolean): VariableRepetitionRule {
        return if (isHiddenInResult == hide) this else VariableRepetitionRule(hide, reduction, rule, min, max)
    }

    override fun withReduction(red: Reduction): VariableRepetitionRule {
        return if (reduction === red) this else VariableRepetitionRule(isHiddenInResult, red, rule, min, max)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is VariableRepetitionRule) return false
        if (this === other) return true
        return isHiddenInResult == other.isHiddenInResult && reduction == other.reduction
                && rule == other.rule
                && min == other.min
                && max == other.max
    }

    override fun hashCode(): Int {
        return min * 31 + max * 31 + hash(isHiddenInResult, reduction, rule)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rule The inner element.
         * @param min  Minimum repetitions.
         * @param max  Maximum repetitions.
         * @return A rule.
         */
        fun create(rule: Rule, min: Int, max: Int): Rule {
            require(min in 0..max) { "Illegal repetition (min=$min, max=$max)" }
            if (min == 0 && max == 0) return EpsilonTerm.default
            if (rule is EpsilonTerm || (min == 1 && max == 1)) return rule
            if ((rule is VariableRepetitionRule) && rule.min <= 1 && min <= 1) {
                val newMin = min(
                    Int.MAX_VALUE.toLong(),
                    (rule.min.toLong()) * min
                ).toInt()
                val newMax = max(
                    Int.MAX_VALUE.toLong(),
                    (rule.max.toLong()) * max
                ).toInt()
                return create(rule.rule, newMin, newMax)
            }
            return VariableRepetitionRule(
                defaultHidden,
                defaultReduction,
                rule,
                min,
                max
            )
        }
    }
}
