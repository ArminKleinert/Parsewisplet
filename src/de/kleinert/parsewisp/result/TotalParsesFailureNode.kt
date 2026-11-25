package de.kleinert.parsewisp.result

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.collections.PretenderList

/**
 * This class represents parse failure embedded into a parse tree when generating a parse forest.
 *
 * @param text  The text.
 * @param key   The production name which failed.
 * @param start Start index of the failure.
 * @param end   End index of the failure.
 * @since 0.9.7
 */
data class TotalParsesFailureNode(
    val text: String, val key: Sym,
    val start: Int, val end: Int
) : ParsesResult, PretenderList<ParseTree> {
    override fun toString(): String {
        return "[$key, could not parse \"$text\" at $start..$end]"
    }
}
