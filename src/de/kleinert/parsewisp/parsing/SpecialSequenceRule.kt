package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.trampoline.ListenerKey
import java.util.*
import java.util.function.Function

/**
 * Represents a special sequence. The typical EBNF-notation is `?...?` where `...` stands for some free-form text.
 * For example, `S = ?any whitespace except newline?` means what it says. This allows some interaction with the program around the parser.
 *
 *
 * You should not use this class unless it is very important!
 *
 * @since 0.9.7
 */
class SpecialSequenceRule private constructor(
    hide: Boolean,
    red: ReductionType,
    /**
     * Returns the description of the rule.
     *
     * @return The description as a string.
     * @since 0.9.7
     */
    val description: String,
    /**
     * The function wrapped in this rule. You should only use this kind of rule if you are absolutely sure that you know what you are doing. The same is true for this method.
     *
     * @return The wrapped function.
     * @since 0.9.7
     */
    val function: Function<String, Optional<String>>
) : SimpleRule(hide, red) {
    override fun parse(index: Int, runner: Gll) {
        val nodeKey: ListenerKey = Rule.Companion.makeKey(index, this)
        val res = function.apply(runner.tramp().getText1().substring(index))

        if (res.isEmpty()) {
            runner.fail(nodeKey, index, this, false)
            return
        }

        runner.pushSuccessMessage(nodeKey, res.get(), index + res.get().length)
    }

    override fun fullParse(index: Int, runner: Gll) {
        val nodeKey: ListenerKey = Rule.Companion.makeKey(index, this)
        val res = function.apply(runner.tramp().getText1().substring(index))

        if (res.isEmpty()) {
            runner.fail(nodeKey, index, this, true)
            return
        }

        runner.pushSuccessMessage(nodeKey, res.get(), index + res.get().length)
    }

    override fun withHideTag(hide: Boolean): SpecialSequenceRule {
        return if (isHiddenInResult1() == hide) this else SpecialSequenceRule(hide, red, description, function)
    }

    override fun withReduction(red: ReductionType): SpecialSequenceRule {
        return if (getReduction1() === red) this else SpecialSequenceRule(hide, red, description, function)
    }

    override fun equals(o: Any?): Boolean {
        if (o !is SpecialSequenceRule) return false
        if (this === o) return true
        return hide == o.hide && red == o.red
                && function == o.function
    }

    override fun hashCode(): Int {
        return Objects.hash(hide, red, function)
    }

    override fun toString(): String {
        return description
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param description The description of the special sequence.
         * @param function    The function which does what the description says.
         * @return A rule.
         * @since 0.9.7
         */
        fun create(
            description: String,
            function: Function<String, Optional<String>>
        ): Rule {
            return SpecialSequenceRule(
                defaultHidden,
                defaultReductionType,
                description,
                function
            )
        }
    }
}
