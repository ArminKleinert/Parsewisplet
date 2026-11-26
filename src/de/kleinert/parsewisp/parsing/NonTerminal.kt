package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.reduction.ReductionType

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
            makeKey(index, rule),
            runner.nodeListener(makeKey(index, this))
        )
    }

    override fun fullParse(index: Int, runner: Gll) {
        val rule = runner.tramp().grammar.getProduction(this.keyword)
        checkNotNull(rule) { "Cannot use non terminal " + this.keyword + " Availability should be checked when initializing parser." }
        runner.pushFullListener(
            makeKey(index, rule),
            runner.nodeListener(makeKey(index, this))
        )
    }

    override fun withHideTag(hide: Boolean): NonTerminal {
        return if (isHiddenInResult1() == hide) this else NonTerminal(hide, red, keyword)
    }

    override fun withReduction(red: ReductionType): NonTerminal {
        return if (getReduction1() === red) this else NonTerminal(hide, red, keyword)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NonTerminal) return false
        if (this === other) return true
        return hide == other.hide && red == other.red && keyword == other.keyword
    }

    override fun hashCode(): Int {
        return hash(hide, red, keyword)
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
            return NonTerminal(defaultHidden, defaultReductionType, keyword)
        }
    }
}
