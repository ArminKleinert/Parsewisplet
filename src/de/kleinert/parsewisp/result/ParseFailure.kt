package de.kleinert.parsewisp.result

import de.kleinert.parsewisp.result.failure.FailureUtil
import de.kleinert.parsewisp.result.failure.ParseFailureReason

/**
 * Represents failures that occurred while parsing.
 *
 * @param index      The index where parsing failed.
 * @param reasonList The list of expected productions or other reasons why parsing failed.
 * @param line       The line where parsing failed, 1-indexed. This can be inferred from the string and index.
 * @param column     The column where parsing failed, 1-indexed. This can be inferred from the string and index.
 * @param text       The text that could not be matched.
 * @since 0.9.7
 */
data class ParseFailure(
    val index: Int,
    val reasonList: List<ParseFailureReason>,
    val line: Int,
    val column: Int,
    val text: String?
) : ParseResult {
    /**
     * An alternative, shorter constructor. Other information will be added later separately.
     *
     * @param index      The index where parsing failed.
     * @param reasonList The list of expected things or other reasons why parsing failed.
     * @since 0.9.7
     */
    constructor(index: Int, reasonList: List<ParseFailureReason>) : this(index, reasonList, -1, -1, null)

    override fun toString(): String {
        return FailureUtil.pprintFailure(this)
    }

    /**
     * Formats the failure into a string with all information included clearly. This is useful for debugging.
     *
     * @return A string for easy viewing.
     * @since 0.9.7
     */
    fun contentsToString(): String {
        return "[$index, $reasonList, $line, $column, $text]"
    }
}
