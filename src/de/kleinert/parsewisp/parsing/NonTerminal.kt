package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.reduction.ReductionType
import java.util.*

/**
 * This type represents non-terminals.
 *
 * @since 0.9.7
 */
class NonTerminal private constructor(
    hide: Boolean,
    red: ReductionType,
    /**
     * Returns the name of the rule.
     *
     * @return The name of the rule.
     * @since 0.9.7
     */
    val keyword: Sym
) : SimpleRule(hide, red) {
    override fun parse(index: Int, runner: Gll) {
        val rule = runner.tramp().grammar.getProduction(this.keyword)
        checkNotNull(rule) { "Cannot use non terminal " + this.keyword + " Availability should be checked when initializing parser." }
        runner.pushListener(
            Rule.Companion.makeKey(index, rule),
            runner.nodeListener(Rule.Companion.makeKey(index, this))
        )
    }

    override fun fullParse(index: Int, runner: Gll) {
        val rule = runner.tramp().grammar.getProduction(this.keyword)
        checkNotNull(rule) { "Cannot use non terminal " + this.keyword + " Availability should be checked when initializing parser." }
        runner.pushFullListener(
            Rule.Companion.makeKey(index, rule),
            runner.nodeListener(Rule.Companion.makeKey(index, this))
        )
    }

    override fun withHideTag(hide: Boolean): NonTerminal {
        return if (isHiddenInResult1() == hide) this else NonTerminal(hide, red, keyword)
    }

    override fun withReduction(red: ReductionType): NonTerminal {
        return if (getReduction1() === red) this else NonTerminal(hide, red, keyword)
    }

    override fun equals(o: Any?): Boolean {
        if (o !is NonTerminal) return false
        if (this === o) return true
        return hide == o.hide && red == o.red && keyword == o.keyword
    }

    override fun hashCode(): Int {
        return Objects.hash(hide, red, keyword)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering.
         *
         * @param keyword The wrapped symbol.
         * @return A non-terminal.
         * @since 0.9.7
         */
        fun create(keyword: Sym): NonTerminal {
            return NonTerminal(Rule.Companion.defaultHidden, Rule.Companion.defaultReductionType, keyword)
        }
    }
}
