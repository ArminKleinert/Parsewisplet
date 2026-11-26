package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * Represents string terminals (both case-sensitive or case-insensitive).<br></br>
 * Syntax: can be written with double quotes or single quotes, so `"..."` and `'...'` are equivalent.
 */
class StringTerm private constructor(
    hide: Boolean,
    red: ReductionType,
    /**
     * Returns the string.
     *
     * @return The string.
     */
    val string: String,
    /**
     * True if the casing doesn't matter, false if it does matter.
     *
     * @return True if the casing doesn't matter, false if it does matter.
     */
    val isCaseInsensitive: Boolean
) : Terminal(hide, red) {
    override fun parse(index: Int, runner: Gll) {
        val text = runner.tramp().text
        val end = Integer.min(text.length, index + this.string.length)
        val head = text.substring(index, end)

        val nodeKey: ListenerKey = makeKey(index, this)
        if (if (this.isCaseInsensitive) this.string.equals(head, ignoreCase = true) else (this.string == head)) {
            runner.pushSuccessMessage(nodeKey, this.string, end)
        } else {
            runner.fail(nodeKey, index, this, false)
        }
    }

    override fun fullParse(index: Int, runner: Gll) {
        val text = runner.tramp().text
        val end = Integer.min(text.length, this.string.length + index)
        val head = text.substring(index, end)
        val nodeKey: ListenerKey = makeKey(index, this)
        if (text.length == end && (if (this.isCaseInsensitive) this.string.equals(
                head,
                ignoreCase = true
            ) else (this.string == head))
        ) {
            runner.pushSuccessMessage(nodeKey, this.string, end)
        } else {
            runner.fail(nodeKey, index, this, true)
        }
    }

    override fun withHideTag(hide: Boolean): StringTerm {
        return if (isHiddenInResult == hide) this else StringTerm(hide, reduction, string, this.isCaseInsensitive)
    }

    override fun withReduction(red: ReductionType): StringTerm {
        return if (reduction === red) this else StringTerm(isHiddenInResult, red, string, this.isCaseInsensitive)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is StringTerm) return false
        if (this === other) return true
        return isHiddenInResult == other.isHiddenInResult && reduction == other.reduction
                && string == other.string
                && (string.isEmpty() || this.isCaseInsensitive == other.isCaseInsensitive)
    }

    override fun hashCode(): Int {
        return hash(isHiddenInResult, reduction, string, this.isCaseInsensitive)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         * The implementation is allowed to create other types of rules, like [RegexTerm] or [EpsilonTerm].
         *
         * @param string          The string to match.
         * @param caseInsensitive True if the casing doesn't matter, false if it does matter.
         * @return A rule.
         */
        fun create(string: String, caseInsensitive: Boolean): Rule {
            if (string.isEmpty()) {
                return EpsilonTerm.default
            }
            return StringTerm(
                defaultHidden,
                defaultReductionType,
                string,
                caseInsensitive
            )
        }

        /**
         * Create a new instance. Depending on the implementation, allows for buffering.
         * Unlike [.create], the output must be a [StringTerm] and can not be replaced by other types.
         *
         * @param string          The string to match.
         * @param caseInsensitive True if the casing doesn't matter, false if it does matter.
         * @return A rule.
         */
        fun createDirect(string: String, caseInsensitive: Boolean): StringTerm {
            return StringTerm(
                defaultHidden,
                defaultReductionType,
                string,
                caseInsensitive
            )
        }
    }
}
