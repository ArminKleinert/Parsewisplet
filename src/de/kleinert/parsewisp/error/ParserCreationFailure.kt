package de.kleinert.parsewisp.error

/**
 * [RuntimeException] which occurs if the parser could not be created.
 *
 * @see de.kleinert.parsewisp.Parsewisp.parser
 * @since 0.9.7
 */
class ParserCreationFailure : RuntimeException {
    /**
     * [RuntimeException] which occurs if the parser could not be created.
     *
     * @param exception Argument.
     * @see de.kleinert.parsewisp.Parsewisp.parser
     * @since 0.9.7
     */
    constructor(exception: IllegalArgumentException) : super(exception)

    /**
     * [RuntimeException] which occurs if the parser could not be created.
     *
     * @param exception Argument.
     * @see de.kleinert.parsewisp.Parsewisp.parser
     * @since 0.9.7
     */
    constructor(exception: IllegalGrammarException) : super(exception)

    /**
     * [RuntimeException] which occurs if the parser could not be created.
     *
     * @param message Argument.
     * @see de.kleinert.parsewisp.Parsewisp.parser
     * @since 0.9.7
     */
    constructor(message: String?) : super(message)
}
