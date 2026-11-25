package de.kleinert.parsewisp.functions

import de.kleinert.parsewisp.result.success.ParseMessage

/**
 * A functional type which takes a [ParseMessage] as an input and returns nothing.
 * This is equivalent to a [java.util.function.Consumer] taking an [ParseMessage], but the name is a bit clearer.
 *
 * @since 0.9.7
 */
fun interface Listener {
    /**
     * Runs the function.
     *
     * @param o The input.
     * @since 0.9.7
     */
    fun execute(o: ParseMessage)
}
