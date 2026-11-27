package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.reduction.Reduction

/**
 * This type represents non-terminals.
 */
class NonTerminal private constructor(
    hide: Boolean,
    red: Reduction,
    /**
     * Returns the name of the rule.
     *
     * @return The name of the rule.
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
        return if (isHiddenInResult == hide) this else NonTerminal(hide, reduction, keyword)
    }

    override fun withReduction(red: Reduction): NonTerminal {
        return if (reduction === red) this else NonTerminal(isHiddenInResult, red, keyword)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NonTerminal) return false
        if (this === other) return true
        return isHiddenInResult == other.isHiddenInResult && reduction == other.reduction && keyword == other.keyword
    }

    override fun hashCode(): Int {
        return hash(isHiddenInResult, reduction, keyword)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering.
         *
         * @param keyword The wrapped symbol.
         * @return A non-terminal.
         */
        fun create(keyword: Sym): NonTerminal {
            return NonTerminal(defaultHidden, defaultReduction, keyword)
        }
    }
}
