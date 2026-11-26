package de.kleinert.parsewisp.result

import de.kleinert.parsewisp.Sym

/**
 * This class represents parse failure embedded into a parse tree when parsing only for the first parse.
 *
 * @param text  The text.
 * @param key   The production name which failed.
 * @param start Start index of the failure.
 * @param end   End index of the failure.
 */
data class ParseFailureNode(
    val text: String,
    val key: Sym,
    val start: Int,
    val end: Int
) : ParseResult {
    override fun toString(): String {
        return "[" + key + ", could not parse \"" + text + "\" at " + start + ".." + end + "]"
    }
}
