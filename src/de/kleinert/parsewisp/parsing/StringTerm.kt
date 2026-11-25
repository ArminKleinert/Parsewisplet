package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.trampoline.ListenerKey
import java.util.*

/**
 * Represents string terminals (both case-sensitive or case-insensitive).<br></br>
 * Syntax: can be written with double quotes or single quotes, so `"..."` and `'...'` are equivalent.
 *
 * @since 0.9.7
 */
class StringTerm private constructor(
    hide: Boolean,
    red: ReductionType,
    /**
     * Returns the string.
     *
     * @return The string.
     * @since 0.9.7
     */
    val string: String,
    /**
     * True if the casing doesn't matter, false if it does matter.
     *
     * @return True if the casing doesn't matter, false if it does matter.
     * @since 0.9.7
     */
    val isCaseInsensitive: Boolean
) : Terminal(hide, red) {
    override fun parse(index: Int, runner: Gll) {
        val text = runner.tramp().getText1()
        val end = Integer.min(text.length, index + this.string.length)
        val head = text.substring(index, end)

        val nodeKey: ListenerKey = Rule.Companion.makeKey(index, this)
        if (if (this.isCaseInsensitive) this.string.equals(head, ignoreCase = true) else (this.string == head)) {
            runner.pushSuccessMessage(nodeKey, this.string, end)
        } else {
            runner.fail(nodeKey, index, this, false)
        }
    }

    override fun fullParse(index: Int, runner: Gll) {
        val text = runner.tramp().getText1()
        val end = Integer.min(text.length, this.string.length + index)
        val head = text.substring(index, end)
        val nodeKey: ListenerKey = Rule.Companion.makeKey(index, this)
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
        return if (isHiddenInResult1() == hide) this else StringTerm(hide, red, string, this.isCaseInsensitive)
    }

    override fun withReduction(red: ReductionType): StringTerm {
        return if (getReduction1() === red) this else StringTerm(hide, red, string, this.isCaseInsensitive)
    }

    override fun equals(o: Any?): Boolean {
        if (o !is StringTerm) return false
        if (this === o) return true
        return hide == o.hide && red == o.red
                && string == o.string
                && (string.isEmpty() || this.isCaseInsensitive == o.isCaseInsensitive)
    }

    override fun hashCode(): Int {
        return Objects.hash(hide, red, string, this.isCaseInsensitive)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         * The implementation is allowed to create other types of rules, like [RegexTerm] or [EpsilonTerm].
         *
         * @param string          The string to match.
         * @param caseInsensitive True if the casing doesn't matter, false if it does matter.
         * @return A rule.
         * @since 0.9.7
         */
        @JvmStatic
        fun create(string: String, caseInsensitive: Boolean): Rule {
            if (string.isEmpty()) {
                return EpsilonTerm.default
            }
            return StringTerm(
                Rule.Companion.defaultHidden,
                Rule.Companion.defaultReductionType,
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
         * @since 0.9.7
         */
        fun createDirect(string: String, caseInsensitive: Boolean): StringTerm {
            return StringTerm(
                Rule.Companion.defaultHidden,
                Rule.Companion.defaultReductionType,
                string,
                caseInsensitive
            )
        }
    }
}
