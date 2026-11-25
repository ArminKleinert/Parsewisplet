package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.collections.FlatResultSeq
import de.kleinert.parsewisp.collections.FlatResultSeq.Companion.make
import de.kleinert.parsewisp.functions.Listener
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.result.success.ParseMessage
import de.kleinert.parsewisp.trampoline.ListenerKey
import java.util.*

/**
 * This class represents a concatenation of productions, written as `p1 p2 p3 ...` (where p1, p2, etc. are instances of [Rule]).
 * When parsing, it tries to match p1, then p2, then p3 and so on.
 *
 * @since 0.9.7
 */
class ConcatRule private constructor(
    hide: Boolean,
    red: ReductionType,
    parsers: List<Rule>
) : RuleWithManyChildren(hide, red, parsers) {
    override fun parse(index: Int, runner: Gll) {
        val parsers = rules
        runner.pushListener(
            Rule.Companion.makeKey(index, parsers.get(0)),
            catListener(make(), parsers.subList(1, parsers.size), makeKey(index, this), runner)
        )
    }

    override fun fullParse(index: Int, runner: Gll) {
        val parsers = rules
        runner.pushListener(
            Rule.Companion.makeKey(index, parsers.get(0)),
            catFullListener(make(), parsers.subList(1, parsers.size), Rule.Companion.makeKey(index, this), runner)
        )
    }

    private fun catListener(
        resultsSoFar: FlatResultSeq,
        parserSequence: List<Rule>,
        nodeKey: ListenerKey,
        runner: Gll
    ): Listener {
        return Listener { result: ParseMessage? ->
            val continueIndex = result!!.index()
            val newResultsSoFar = resultsSoFar.appendOrConcat(result.result)
            if (parserSequence.isEmpty()) {
                runner.pushSuccessMessage(nodeKey, newResultsSoFar, continueIndex)
            } else {
                runner.pushListener(
                    Rule.Companion.makeKey(continueIndex, parserSequence.get(0)),
                    catListener(
                        newResultsSoFar,
                        parserSequence.subList(1, parserSequence.size),
                        nodeKey,
                        runner
                    )
                )
            }
        }
    }

    private fun catFullListener(
        resultsSoFar: FlatResultSeq,
        parserSequence: List<Rule>,
        nodeKey: ListenerKey,
        runner: Gll
    ): Listener {
        return Listener { result: ParseMessage? ->
            val continueIndex = result!!.index()
            val newResultsSoFar = resultsSoFar.appendOrConcat(result.result)
            if (parserSequence.isNotEmpty()) {
                val listenerKey: ListenerKey = Rule.Companion.makeKey(continueIndex, parserSequence.get(0))
                val listener = catFullListener(
                    newResultsSoFar,
                    parserSequence.subList(1, parserSequence.size),
                    nodeKey, runner
                )

                if (parserSequence.size == 1) runner.pushFullListener(listenerKey, listener)
                else runner.pushListener(listenerKey, listener)
            } else {
                runner.pushSuccessMessage(nodeKey, newResultsSoFar, continueIndex)
            }
        }
    }

    override fun withHideTag(hide: Boolean): Rule {
        return if (isHiddenInResult == hide) this else ConcatRule(hide, reduction, rules)
    }

    override fun withReduction(red: ReductionType): Rule {
        return if (reduction === red) this else ConcatRule(isHiddenInResult, red, rules)
    }

    override fun withRules(rules: List<Rule>): ConcatRule {
        return ConcatRule(isHiddenInResult, reduction, rules)
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

            val compressedResult = ArrayList<Rule>()

            for (rule in rules) {
                if (rule is ConcatRule && !rule.isHiddenInResult1() && rule.red == Rule.Companion.defaultReductionType) {
                    compressedResult.addAll(rule.rules)
                } else {
                    compressedResult.add(rule)
                }
            }

            return ConcatRule(
                Rule.Companion.defaultHidden, Rule.Companion.defaultReductionType,
                compressedResult
            )
        }

        /**
         * Like [.create], except it assumes that (1) the input includes no epsilons and (2) there are at least two rules in the input.
         * This makes the method perform fewer optimizations, saving a miniscule amount of time.
         *
         * @param rules The rules.
         * @return A concatenation rule.
         * @since 0.9.7
         */
        fun createDirect(
            rules: List<Rule>
        ): ConcatRule {
            return ConcatRule(Rule.Companion.defaultHidden, Rule.Companion.defaultReductionType, rules)
        }
    }
}
