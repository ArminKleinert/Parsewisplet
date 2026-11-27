package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.grammar.GrammarPrinter
import de.kleinert.parsewisp.reduction.Reduction
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * A class representing the right-hand sides of productions.
 */
abstract class Rule
protected constructor(
    /**
     * Whether to hide the rule in the output.
     */
    val isHiddenInResult: Boolean,
    /**
     * See [Reduction].
     */
    val reduction: Reduction
) {

    /**
     * Runs the rule from the provided index. The text is in the arguments.
     *
     *
     * Results (successes and failures) are saved using [Gll.pushSuccessMessage] or [Gll.fail] or some similar function.
     *
     * @param index  The start index.
     * @param runner Helper structure.
     */
    abstract fun parse(index: Int, runner: Gll)

    /**
     * Runs the rule from the provided index. The text is in the arguments. Unlike [parse], this method tries to parse the text from the index until the end. If the string can't be matched to the end, results in a failure.
     *
     *
     * Results (successes and failures) are saved using [Gll.pushSuccessMessage] or [Gll.fail] or some similar function.
     *
     * @param index  The start index.
     * @param runner Helper structure.
     */
    abstract fun fullParse(index: Int, runner: Gll)

    /**
     * Hides or unhides content in output.
     *
     * @param hide Whether to hide the content.
     * @return An instance of the same class with the hide tag set to the parameter.
     */
    abstract fun withHideTag(hide: Boolean): Rule

    /**
     * Creates an instance of this class with the reduction type set.
     *
     * @param red The reduction type.
     * @return An instance of the same class with the reduction type set to the parameter.
     */
    abstract fun withReduction(red: Reduction): Rule

    /**
     * Hide content in output.
     *
     * @return An instance of the same class with the hide tag set to true.
     * @see .withHideTag
     */
    fun hideInResult(): Rule {
        return withHideTag(true)
    }

    /**
     * Unhide content in output.
     *
     * @return An instance of the same class with the hide tag set to false.
     * @see .withHideTag
     */
    open fun includeInResult(): Rule {
        return withHideTag(false)
    }

    /**
     * Hide the tag associated with this rule.
     * Wrap this rule around the entire right-hand side.
     *
     * @return A new instance of the same class.
     */
    fun mergeToTop(): Rule {
        return withReduction(Reduction.merging())
    }

    // Force children to override this.
    abstract override fun equals(other: Any?): Boolean

    // Force children to override this.
    abstract override fun hashCode(): Int

    override fun toString(): String {
        return GrammarPrinter.default.ruleToString(this)
    }

    /**
     * A wrapper for the constructor of [ListenerKey].
     *
     * @param index The index.
     * @param rule  The rule.
     * @return A new [ListenerKey].
     */
    protected fun makeKey(index: Int, rule: Rule): ListenerKey {
        return ListenerKey(index, rule)
    }

    companion object {
        /**
         * Default value for [isHiddenInResult].
         */
        protected const val defaultHidden: Boolean = false

        fun hash(vararg any: Any): Int = any.contentHashCode()
    }
}

/**
 * Default value for [Rule.reduction].
 */
internal val defaultReduction: Reduction = Reduction.initial()
