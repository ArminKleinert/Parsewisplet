package de.kleinert.parsewisp.functions

/**
 * Equivalent type to a [Procedure], but the name is clearer. This type is used for negative lookaheads when parsing.
 *
 * @since 0.9.7
 */
fun interface NegativeListener {
    /**
     * Executes the function.
     *
     * @since 0.9.7
     */
    operator fun invoke()
}
