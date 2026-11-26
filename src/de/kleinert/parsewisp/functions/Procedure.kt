package de.kleinert.parsewisp.functions

/**
 * A functional type for a procedure (no arguments, void output).
 */
fun interface Procedure {
    /**
     * Runs the procedure.
     */
    operator fun invoke()
}
