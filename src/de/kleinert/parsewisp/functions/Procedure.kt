package de.kleinert.parsewisp.functions

/**
 * A functional type for a procedure (no arguments, void output).
 *
 * @since 0.9.7
 */
fun interface Procedure {
    /**
     * Runs the procedure.
     *
     * @since 0.9.7
     */
    operator fun invoke()
}
