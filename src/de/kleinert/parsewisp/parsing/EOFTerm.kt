package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.Reduction

/**
 * This class should almost never be used, but can have its uses occasionally.
 *
 * Format: `"EOF"`
 */
class EOFTerm private constructor(hide: Boolean, red: Reduction) : Terminal(hide, red) {
    override fun parse(index: Int, runner: Gll) {
        fullParse(index, runner)
    }

    override fun fullParse(index: Int, runner: Gll) {
        if (index == runner.tramp()
                .text.length
        ) runner.pushSuccessMessageWithoutValue(makeKey(index, this), index)
        else runner.fail(makeKey(index, this), index, this, true)
    }

    override fun withHideTag(hide: Boolean): EOFTerm {
        return if (isHiddenInResult == hide) this else EOFTerm(hide, reduction)
    }

    override fun withReduction(red: Reduction): EOFTerm {
        return if (reduction === red) this else EOFTerm(isHiddenInResult, red)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EOFTerm) return false
        if (this === other) return true
        return isHiddenInResult == other.isHiddenInResult && reduction == other.reduction
    }

    override fun hashCode(): Int {
        return hash(isHiddenInResult, reduction)
    }

    companion object {
        /**
         * Default text for EOF.
         *
         * @return The text that identifies an EOF rule in a grammar.
         */
        fun text(): String {
            return "EOF"
        }

        val default: EOFTerm
            /**
             * Default EOF rule. Can be buffered.
             *
             * @return Default EOF rule. Can be buffered.
             */
            get() = EOFTerm(defaultHidden, defaultReduction)
    }
}
