package de.kleinert.parsewisp.result.failure

import de.kleinert.parsewisp.grammar.GrammarPrinter
import de.kleinert.parsewisp.parsing.*

/**
 * A failure lists reasons for the failure. This class represents the possible reasons.
 *
 * @param rule            The rule was parsed when the failure appeared.
 * @param reasonString    A string describing the failure. Please prefer [failureReasonString].
 * @param untilEndOfInput Whether the production that failed covered the entire input from beginning to end. When showing the object as a string, this adds the note "(followed by end of string)" or something similar.
 * @since 0.9.7
 */
data class ParseFailureReason(
    val rule: Rule,
    val reasonString: String?,
    val untilEndOfInput: Boolean
) {
    /**
     * Representation of the failure reasonList as a string.
     *
     * @return A representation of the expected production as a string.
     * @since 0.9.7
     */
    fun failureReasonString(): String {
        if (reasonString != null) return reasonString
        return GrammarPrinter.default.ruleToString(rule)
    }

    /**
     * Whether the production that failed covered the entire input from beginning to end. When showing the object as a string, this adds the note "(followed by end of string)" or something similar.
     *
     * @return Whether the production that failed covered the entire input from beginning to end. When showing the object as a string, this adds the note "(followed by end of string)" or something similar.
     * @since 0.9.7
     */
    fun untilEndOfInput(): Boolean {
        return untilEndOfInput
    }

    companion object {
        /**
         * Creates a [ParseFailureReason] for the given inputs.
         *
         * @param rule     The rule.
         * @param untilEnd Whether the rule is followed by end-of-string.
         * @param printer  The printer used to create a string-representation of the failure.
         * @return A failure-reason based on the parameters.
         * @since 0.9.7
         */
        fun creatureFailReason(rule: Rule, untilEnd: Boolean, printer: GrammarPrinter): ParseFailureReason {
            val str: String?

            if (rule is EpsilonTerm || rule is EOFTerm) {
                str = "end-of-string"
            } else if (rule is LookaheadRule) {
                str = if (untilEnd) "end-of-string" else null
            } else if (rule is NegativeLookaheadRule) {
                str = "NOT " + printer.ruleToString(rule.rule)
            } else if (rule is ExclusionRule) {
                str = printer.ruleToString(rule.expected) + " but NOT " + printer.ruleToString(rule.excluded)
            } else {
                str = printer.ruleToString(rule)
            }

            return ParseFailureReason(rule, str, untilEnd)
        }
    }
}
