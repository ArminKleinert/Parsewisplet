package de.kleinert.parsewisp.result

/**
 * A class for the results of single parses. This typically covers [ParseTree] and [ParseFailure].
 */
interface ParseResult {
    val isSuccess: Boolean
        /**
         * True if this is the result of a successful parse.
         *
         * @return True if this is the result of a successful parse.
         */
        get() = this is ParseTree

    val isFailure: Boolean
        /**
         * True if this is the result of a failed parse.
         *
         * @return True if this is the result of a failed parse.
         */
        get() = !this.isSuccess

    /**
     * Casts the result to a [ParseTree].
     *
     * @return A [ParseTree].
     */
    fun castToParseSuccess(): ParseTree {
        return this as ParseTree
    }

    /**
     * Casts the result to an instance of [ParseFailure].
     *
     * @return An instance of [ParseFailure].
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
         */
        fun make(o: Any): ParseResult {
            if (o is ParseTree || o is ParseFailure) {
                return o
            }
            throw IllegalArgumentException(o.javaClass.toString())
        }
    }
}
