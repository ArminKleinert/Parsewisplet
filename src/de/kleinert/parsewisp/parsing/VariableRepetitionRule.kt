package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.collections.FlatResultSeq
import de.kleinert.parsewisp.collections.FlatResultSeq.Companion.make
import de.kleinert.parsewisp.functions.Listener
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.result.success.ParseMessage
import de.kleinert.parsewisp.trampoline.ListenerKey
import java.lang.Long
import java.util.*
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.require

/**
 * A class representing the ABNF counted repetition operator.
 * There are multiple variants, depending on the syntax (where n and m are integers, and p is an instance of [Rule]):
 *
 *  * Repeat a minimum of `n` times and a maximum of `m` times: `p{n,m}`
 *  * Repeat a minimum of `n` times: `p{n,}`
 *  * Repeat a maximum of `m` times: `p{,m}`
 *  * Repeat exactly of `n` times: `p{n}`
 *
 *
 * @since 0.9.7
 */
class VariableRepetitionRule private constructor(
    hide: Boolean,
    red: ReductionType,
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
            runner.pushSuccessMessageWithoutValue(Rule.Companion.makeKey(index, this), index)
        }
        if (max == 0) {
            return
        }
        runner.pushListener(
            Rule.Companion.makeKey(index, rule),
            repListener(make(), 0, this, Rule.Companion.makeKey(index, this), runner, index)
        )
    }

    override fun fullParse(index: Int, runner: Gll) {
        if (min == 0) {
            runner.pushSuccessMessageWithoutValue(Rule.Companion.makeKey(index, this), index)
        }
        if (max == 0) {
            return
        }
        runner.pushListener(
            Rule.Companion.makeKey(index, rule),
            repFullListener(make(), 0, rule, min, max, Rule.Companion.makeKey(index, this), runner)
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
        return Listener { result: ParseMessage? ->
            val parsedResult = result!!.result
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
                return@Listener
            }
            if (newNResultsSoFar < repRule.max) {
                runner.pushListener(
                    Rule.Companion.makeKey(continueIndex, repRule.rule),
                    repListener(newResultsSoFar, newNResultsSoFar, repRule, nodeKey, runner, continueIndex)
                )
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
        return Listener { result: ParseMessage? ->
            val parsedResult = result!!.result
            val continueIndex = result.index()
            val newResultsSoFar = resultsSoFar.appendOrConcat(parsedResult)
            val newNResultsSoFar = nResultsSoFar + 1
            if (continueIndex == runner.tramp().getText1().length) {
                if (minimum <= newNResultsSoFar && newNResultsSoFar <= maximum) {
                    runner.pushSuccessMessage(nodeKey, newResultsSoFar, continueIndex)
                } else {
                    runner.fail(nodeKey, continueIndex, this, true)
                }
            } else {
                if (newNResultsSoFar < maximum) {
                    val listener = repFullListener(
                        newResultsSoFar, newNResultsSoFar,
                        rule, minimum, maximum, nodeKey, runner
                    )
                    runner.pushListener(Rule.Companion.makeKey(continueIndex, rule), listener)
                } else {
                    runner.fail(nodeKey, continueIndex, this, true)
                }
            }
        }
    }

    override fun withRule(rule: Rule): VariableRepetitionRule {
        return VariableRepetitionRule(hide, red, rule, min, max)
    }

    override fun withHideTag(hide: Boolean): VariableRepetitionRule {
        return if (isHiddenInResult1() == hide) this else VariableRepetitionRule(hide, red, rule, min, max)
    }

    override fun withReduction(red: ReductionType): VariableRepetitionRule {
        return if (getReduction1() === red) this else VariableRepetitionRule(hide, red, rule, min, max)
    }

    override fun equals(o: Any?): Boolean {
        if (o !is VariableRepetitionRule) return false
        if (this === o) return true
        return hide == o.hide && red == o.red
                && rule == o.rule
                && min == o.min
                && max == o.max
    }

    override fun hashCode(): Int {
        return min * 31 + max * 31 + Objects.hash(hide, red, rule)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rule The inner element.
         * @param min  Minimum repetitions.
         * @param max  Maximum repetitions.
         * @return A rule.
         * @since 0.9.7
         */
        fun create(rule: Rule, min: Int, max: Int): Rule {
            require(!(min < 0 || min > max)) { "Illegal repetition (min=" + min + ", max=" + max + ")" }
            if (min == 0 && max == 0) return EpsilonTerm.default
            if (rule is EpsilonTerm || (min == 1 && max == 1)) return rule
            if ((rule is VariableRepetitionRule) && rule.min <= 1 && min <= 1) {
                val newMin = Long.min(
                    Int.MAX_VALUE.toLong(),
                    (rule.min.toLong()) * min
                ).toInt()
                val newMax = Long.min(
                    Int.MAX_VALUE.toLong(),
                    (rule.max.toLong()) * max
                ).toInt()
                return create(rule.rule, newMin, newMax)
            }
            return VariableRepetitionRule(
                Rule.Companion.defaultHidden,
                Rule.Companion.defaultReductionType,
                rule,
                min,
                max
            )
        }
    }
}
