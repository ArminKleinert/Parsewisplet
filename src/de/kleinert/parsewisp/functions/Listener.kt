package de.kleinert.parsewisp.functions

import de.kleinert.parsewisp.result.success.ParseMessage

/**
 * A functional type which takes a [ParseMessage] as an input and returns nothing.
 * This is equivalent to a [kotlin.jvm.functions.Function1] taking an [ParseMessage], but the name is a bit clearer.
 *
 */
fun interface Listener {
    /**
     * Runs the function.
     *
     * @param result The input.
     */
    operator fun invoke(result: ParseMessage)
}
