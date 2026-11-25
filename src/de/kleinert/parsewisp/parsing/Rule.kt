package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.grammar.GrammarPrinter
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * A class representing the right-hand sides of productions.
 *
 * @since 0.9.7
 */
/**
 * The constructor. Do not call directly unless you are maintaining this library.
 *
 * @param hide The hide option.
 * @param red  The reduction type for this rule.
 * @since 0.9.7
 */
abstract class Rule
protected constructor(
    /**
     * Whether to hide the rule in the output.
     *
     * @since 0.9.7
     */
    /**
     * Check whether the content is hidden in the output.
     *
     * @return true if the content is hidden in the output, false otherwise.
     * @since 0.9.7
     */
    val isHiddenInResult: Boolean,
    /**
     * See [ReductionType].
     *
     * @since 0.9.7
     */
    /**
     * Get the used reduction type.
     *
     * @return The current reduction type.
     * @since 0.9.7
     */
    val reduction: ReductionType
) {
    fun getReduction1() = reduction
    fun isHiddenInResult1() = isHiddenInResult

    val red: ReductionType
        get()= reduction

    val hide: Boolean
        get() = isHiddenInResult

    /**
     * Runs the rule from the provided index. The text is in the arguments.
     *
     *
     * Results (successes and failures) are saved using [Gll.pushSuccessMessage] or [Gll.fail] or some similar function.
     *
     * @param index  The start index.
     * @param runner Helper structure.
     * @since 0.9.7
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
     * @since 0.9.7
     */
    abstract fun fullParse(index: Int, runner: Gll)

    /**
     * Hides or unhides content in output.
     *
     * @param hide Whether to hide the content.
     * @return An instance of the same class with the hide tag set to the parameter.
     * @since 0.9.7
     */
    abstract fun withHideTag(hide: Boolean): Rule

    /**
     * Creates an instance of this class with the reduction type set.
     *
     * @param red The reduction type.
     * @return An instance of the same class with the reduction type set to the parameter.
     * @since 0.9.7
     */
    abstract fun withReduction(red: ReductionType): Rule

    /**
     * Hide content in output.
     *
     * @return An instance of the same class with the hide tag set to true.
     * @see .withHideTag
     * @since 0.9.7
     */
    fun hideInResult(): Rule {
        return withHideTag(true)
    }

    /**
     * Unhide content in output.
     *
     * @return An instance of the same class with the hide tag set to false.
     * @see .withHideTag
     * @since 0.9.7
     */
    open fun includeInResult(): Rule {
        return withHideTag(false)
    }

    /**
     * Hide the tag associated with this rule.
     * Wrap this rule around the entire right-hand side.
     *
     * @return A new instance of the same class.
     * @since 0.9.7
     */
    fun mergeToTop(): Rule {
        return withReduction(ReductionType.Companion.merging())
    }

    // Force children to override this.
    abstract override fun equals(o: Any?): Boolean

    // Force children to override this.
    abstract override fun hashCode(): Int

    override fun toString(): String {
        return GrammarPrinter.default.ruleToString(this)
    }

    companion object {
        /**
         * Default value for [isHiddenInResult1].
         *
         * @since 0.9.7
         */
        protected const val defaultHidden: Boolean = false

        /**
         * Default value for [Rule.getReduction1].
         *
         * @since 0.9.7
         */
        @JvmStatic
        protected val defaultReductionType: ReductionType = ReductionType.Companion.initial()

        /**
         * A wrapper for the constructor of [ListenerKey].
         *
         * @param index The index.
         * @param rule  The rule.
         * @return A new [ListenerKey].
         */
        @JvmStatic
        protected fun makeKey(index: Int, rule: Rule): ListenerKey {
            return ListenerKey(index, rule)
        }
    }
}
