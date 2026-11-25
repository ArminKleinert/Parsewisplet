package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType
import java.util.*

/**
 * Abstraction of [Rule] types which wrap one other [Rule]. Examples: repetitions, optionals, lookahead.
 *
 * @since 0.9.7
 */
abstract class RuleWithChild
internal constructor(
    hide: Boolean,
    red: ReductionType,
    /**
     * The rule which is wrapped by this rule.
     *
     * @since 0.9.7
     */
    /**
     * Get the inner [Rule] used for parsing. For example, for a repetition `P+`, returns `P`.
     *
     * @return The inner [Rule].
     * @since 0.9.7
     */
    val rule: Rule
) : Rule(hide, red) {
    private var bufferedHashCode = Long.MIN_VALUE

    /**
     * Set the inner [Rule] used for parsing and returns an instance of the same class.
     *
     * @param rule The new inner [Rule].
     * @return A new instance.
     * @since 0.9.7
     */
    abstract fun withRule(rule: Rule): RuleWithChild

    override fun includeInResult(): Rule {
        return (withHideTag(false) as RuleWithChild).withRule(rule.includeInResult())
    }

    override fun equals(o: Any?): Boolean {
        if (javaClass != o?.javaClass) return false
        if (hashCode() != o.hashCode()) return false
        val that = o as RuleWithChild
        if (isHiddenInResult != that.isHiddenInResult) return false
        if (reduction != that.reduction) return false
        return this.rule == that.rule
    }

    override fun hashCode(): Int {
        if (bufferedHashCode == Long.MIN_VALUE) bufferedHashCode =
            Objects.hash(javaClass, reduction, isHiddenInResult, this.rule).toLong()
        return bufferedHashCode.toInt()
    }
}
