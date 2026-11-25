package de.kleinert.parsewisp.functions

/**
 * A functional type for a procedure (no arguments, void output). [Runnable] does the same thing, but has a different purpose.
 *
 * @since 0.9.7
 */
fun interface Procedure {
    /**
     * Runs the procedure.
     *
     * @since 0.9.7
     */
    fun execute()
}
