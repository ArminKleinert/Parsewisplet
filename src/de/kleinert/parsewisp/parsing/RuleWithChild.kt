package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType

/**
 * Abstraction of [Rule] types which wrap one other [Rule]. Examples: repetitions, optionals, lookahead.
 */
abstract class RuleWithChild
internal constructor(
    hide: Boolean,
    red: ReductionType,
    /**
     * The rule which is wrapped by this rule.
     */
    /**
     * Get the inner [Rule] used for parsing. For example, for a repetition `P+`, returns `P`.
     *
     * @return The inner [Rule].
     */
    val rule: Rule
) : Rule(hide, red) {
    private var bufferedHashCode = Long.MIN_VALUE

    /**
     * Set the inner [Rule] used for parsing and returns an instance of the same class.
     *
     * @param rule The new inner [Rule].
     * @return A new instance.
     */
    abstract fun withRule(rule: Rule): RuleWithChild

    override fun includeInResult(): Rule {
        return (withHideTag(false) as RuleWithChild).withRule(rule.includeInResult())
    }

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        val that = other as RuleWithChild
        if (isHiddenInResult != that.isHiddenInResult) return false
        if (reduction != that.reduction) return false
        return this.rule == that.rule
    }

    override fun hashCode(): Int {
        if (bufferedHashCode == Long.MIN_VALUE) bufferedHashCode =
            hash(javaClass, reduction, isHiddenInResult, this.rule).toLong()
        return bufferedHashCode.toInt()
    }
}
