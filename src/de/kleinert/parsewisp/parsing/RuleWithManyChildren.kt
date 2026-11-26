package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType

/**
 * Abstraction of [Rule] types which wrap multiple other [Rule] instances. Examples: choices, concatenations.
 */
abstract class RuleWithManyChildren
 protected constructor(
    hide: Boolean,
    red: ReductionType,
    /**
     * The rules wrapped by this rule.
     */
    /**
     * Set the inner [Rule] list used for parsing and returns an instance of the same class.
     *
     * @return The inner [Rule] list.
     */
    val rules: List<Rule>
) : Rule(hide, red) {
    private var bufferedHashCode = Long.MIN_VALUE

    override fun includeInResult(): RuleWithManyChildren {
        return (withHideTag(false) as RuleWithManyChildren).withRules(
            this.rules
                .map { obj: Rule -> obj.includeInResult() })
    }

    /**
     * Set the inner [Rule] list used for parsing and returns an instance of the same class.
     *
     * @param rules The new inner [Rule].
     * @return A new instance.
     */
    abstract fun withRules(rules: List<Rule>): RuleWithManyChildren

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        if (hashCode() != other.hashCode()) return false
        val that = other as RuleWithManyChildren
        if (isHiddenInResult != that.isHiddenInResult) return false
        if (reduction != that.reduction) return false
        return this.rules == that.rules
    }

    override fun hashCode(): Int {
        if (bufferedHashCode == Long.MIN_VALUE) bufferedHashCode =
            hash(javaClass, reduction, isHiddenInResult, this.rules).toLong()
        return bufferedHashCode.toInt()
    }
}