package de.kleinert.parsewisp.functions

/**
 * Equivalent type to a [Procedure], but the name is clearer. This type is used for negative lookaheads when parsing.
 */
fun interface NegativeListener {
    /**
     * Executes the function.
     */
    operator fun invoke()
}
