package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType
import java.util.*

/**
 * This class should almost never be used, but can have its uses occasionally.
 *
 *
 * Format: `"EOF"`
 *
 * @since 0.9.7
 */
class EOFTerm private constructor(hide: Boolean, red: ReductionType) : Terminal(hide, red) {
    override fun parse(index: Int, runner: Gll) {
        fullParse(index, runner)
    }

    override fun fullParse(index: Int, runner: Gll) {
        if (index == runner.tramp()
                .getText1().length
        ) runner.pushSuccessMessageWithoutValue(Rule.Companion.makeKey(index, this), index)
        else runner.fail(Rule.Companion.makeKey(index, this), index, this, true)
    }

    override fun withHideTag(hide: Boolean): EOFTerm {
        return if (isHiddenInResult == hide) this else EOFTerm(hide, red)
    }

    override fun withReduction(red: ReductionType): EOFTerm {
        return if (reduction === red) this else EOFTerm(hide, red)
    }

    override fun equals(o: Any?): Boolean {
        if (o !is EOFTerm) return false
        if (this === o) return true
        return hide == o.hide && red == o.red
    }

    override fun hashCode(): Int {
        return Objects.hash(hide, red)
    }

    companion object {
        /**
         * Default text for EOF.
         *
         * @return The text that identifies an EOF rule in a grammar.
         * @since 0.9.7
         */
        fun text(): String {
            return "EOF"
        }

        val default: EOFTerm
            /**
             * Default EOF rule. Can be buffered.
             *
             * @return Default EOF rule. Can be buffered.
             * @since 0.9.7
             */
            get() = EOFTerm(defaultHidden, defaultReductionType)
    }
}
