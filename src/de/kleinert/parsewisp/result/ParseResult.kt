package de.kleinert.parsewisp.result

/**
 * A class for the results of single parses. This typically covers [ParseTree] and [ParseFailure].
 *
 * @since 0.9.7
 */
interface ParseResult {
    val isSuccess: Boolean
        /**
         * True if this is the result of a successful parse.
         *
         * @return True if this is the result of a successful parse.
         * @since 0.9.7
         */
        get() = this is ParseTree

    val isFailure: Boolean
        /**
         * True if this is the result of a failed parse.
         *
         * @return True if this is the result of a failed parse.
         * @since 0.9.7
         */
        get() = !this.isSuccess

    /**
     * Casts the result to a [ParseTree].
     *
     * @return A [ParseTree].
     * @since 0.9.7
     */
    fun castToParseSuccess(): ParseTree {
        return this as ParseTree
    }

    /**
     * Casts the result to an instance of [ParseFailure].
     *
     * @return An instance of [ParseFailure].
     * @since 0.9.7
     */
    fun castToParseFailure(): ParseFailure {
        return this as ParseFailure
    }

    companion object {
        /**
         * Takes an object and returns an appropriate subtype associated with its type.
         *
         * @param o The input.
         * @return Output as [ParseResult].
         * @since 0.9.7
         */
        fun make(o: Any): ParseResult {
            if (o is ParseTree || o is ParseFailure) {
                return o
            }
            throw IllegalArgumentException(o.javaClass.toString())
        }
    }
}
