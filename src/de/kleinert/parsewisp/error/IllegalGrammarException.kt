package de.kleinert.parsewisp.error

/**
 * Exception which occurs if a grammar is invalid.
 *
 * @since 0.9.7
 */
class IllegalGrammarException
/**
 * Exception which occurs if a grammar is invalid.
 *
 * @param message Message.
 * @since 0.9.7
 */
    (message: String?) : RuntimeException(message)
