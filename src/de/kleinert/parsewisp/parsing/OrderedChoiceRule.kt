package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * This class is an alternative to the [AlternationRule].
 *
 *
 * It represents the ABNF choice operator `(p1 / p2)` (where p1 and p2 are instances of [Rule])
 * and should work like the PEG extension which makes it "ordered".
 *
 *
 * As of now, it does not work right,
 * so it can be considered a worse alternative to the [AlternationRule].
 */
class OrderedChoiceRule private constructor(
    hide: Boolean, red: ReductionType,
    private val rule1: Rule,
    private val rule2: Rule
) : RuleWithManyChildren(hide, red, listOf(rule1, rule2)) {
    private constructor(
        rules: List<Rule>,
        hide: Boolean,
        red: ReductionType
    ) : this(hide, red, setupParsers(rules).rule1, setupParsers(rules).rule2)

    private constructor(
        rule1: Rule,
        rule2: Rule
    ) : this(defaultHidden, ReductionType.initial(), rule1, rule2)

    override fun parse(index: Int, runner: Gll) {
        val nodeKeyForComb1: ListenerKey = makeKey(index, this.rule1)
        val listener = runner.nodeListener(makeKey(index, this))
        runner.pushListener(nodeKeyForComb1, listener)
        runner.pushNegativeListener(
            nodeKeyForComb1,
            { runner.pushListener(makeKey(index, this.rule2), listener) })
    }

    override fun fullParse(index: Int, runner: Gll) {
        val nodeKeyForComb1: ListenerKey = makeKey(index, this.rule1)
        val listener = runner.nodeListener(makeKey(index, this))
        runner.pushFullListener(nodeKeyForComb1, listener)
        runner.pushNegativeListener(
            nodeKeyForComb1,
            { runner.pushFullListener(makeKey(index, this.rule2), listener) })
    }

    override fun withHideTag(hide: Boolean): OrderedChoiceRule {
        return if (isHiddenInResult1() == hide) this else OrderedChoiceRule(hide, this.getReduction1(), rule1, rule2)
    }

    override fun withReduction(red: ReductionType): OrderedChoiceRule {
        return if (getReduction1() === red) this else OrderedChoiceRule(isHiddenInResult1(), red, rule1, rule2)
    }

    override fun withRules(rules: List<Rule>): OrderedChoiceRule {
        return OrderedChoiceRule(rules, isHiddenInResult1(), getReduction1())
    }

    companion object {
        private fun setupParsers(
            parsers: List<Rule>
        ): OrderedChoiceRule {
            require(parsers.size >= 2)

            if (parsers.size == 2) return OrderedChoiceRule(parsers.get(0), parsers.get(1))

            val restParsers = parsers.subList(1, parsers.size)
            return OrderedChoiceRule(parsers.get(0), setupParsers(restParsers))
        }

        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rules The wrapped symbol.
         * @return A rule.
         */
        fun create(rules: List<Rule>): Rule {
            if (rules.isEmpty()) return EpsilonTerm.default

            val distinctRules = rules.distinct()

            if (distinctRules.size == 1) return distinctRules.get(0)
            val setup: OrderedChoiceRule = setupParsers(distinctRules)

            return OrderedChoiceRule(
                defaultHidden, defaultReductionType,
                setup.rule1, setup.rule2
            )
        }
    }
}
