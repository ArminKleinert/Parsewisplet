package de.kleinert.parsewisp.error

/**
 * Exception which occurs if a grammar is invalid.
 *
 * @since 0.9.7
 */
class IllegalGrammarException(message: String?) : RuntimeException(message)
