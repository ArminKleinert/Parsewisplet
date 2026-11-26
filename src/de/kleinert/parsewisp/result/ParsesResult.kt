package de.kleinert.parsewisp.result

import de.kleinert.parsewisp.collections.LazySupplierList
import de.kleinert.parsewisp.collections.PretenderList
import java.util.*
import java.util.function.IntFunction

/**
 * A class for the results of multiple parses. This typically covers [LazyResultList] and [ParsesFailureResult].
 *
 * @since 0.9.7
 */
interface ParsesResult : List<ParseTree> {
    val isSuccess: Boolean
        /**
         * True if this is the result of a successful parse.
         *
         * @return True if this is the result of a successful parse.
         * @since 0.9.7
         */
        get() = this is LazyResultList

    /**
     * Casts the result to a list of [ParseTree].
     *
     * @return A list of [ParseTree].
     * @since 0.9.7
     */
    fun castToParsesSuccess(): LazyResultList {
        if (this !is LazyResultList) throw ClassCastException("Cannot cast failure to success.")
        return this
    }

    /**
     * Casts the result to an instance of [ParsesFailureResult].
     *
     * @return An instance of [ParsesFailureResult].
     * @since 0.9.7
     */
    fun castToParsesFailure(): ParsesFailureResult {
        return this as ParsesFailureResult
    }

    /**
     * Converts the parse forest input into a list, each parse tree is converted using [ParseTree.toRawList].
     *
     * @return The parse forest as a list of lists.
     * @since 0.9.7
     */
    fun toRawList(): List<List<Any?>> {
        if (!this.isSuccess) throw ClassCastException("Cannot cast failure to success.")
        return stream().map { obj: ParseTree -> obj.toRawList() }.toList()
    }

    /**
     * A class for successful results of parses.
     */
    class LazyResultList
    /**
     * Creates a new instance (a list of parse trees).
     *
     * @param nextFn     The function.
     * @param maxResults Maximum number of results.
     * @since 0.9.7
     */
        (nextFn: IntFunction<ParseTree?>, maxResults: Int) : LazySupplierList<ParseTree>(nextFn, maxResults),
        ParsesResult

    /**
     * This class is used for [de.kleinert.parsewisp.parser.Parser.parses] to represent the failure.
     *
     * @since 0.9.7
     */
    class ParsesFailureResult internal constructor(val parseFailure: ParseFailure) : ParsesResult,
        PretenderList<ParseTree> {
        /**
         * Extracts the wrapped failure object.
         *
         * @return The wrapped failure.
         * @since 0.9.7
         */
        fun asFailure(): ParseFailure {
            return parseFailure
        }

        override fun get(index: Int): ParseTree {
            Objects.checkIndex(index, 0)
            throw IllegalStateException()
        }

        override val size: Int
            get() = 0

        override fun equals(other: Any?): Boolean {
            if (other !is ParsesFailureResult) return false
            if (!super.equals(other)) return false
            return parseFailure == other.parseFailure
        }

        override fun hashCode(): Int {
            return Objects.hash(parseFailure)
        }

        override fun toString(): String {
            return parseFailure.toString()
        }
    }

    companion object {
        /**
         * Takes an object and returns an appropriate subtype associated with its type.
         *
         * @param o The input.
         * @return Output as [ParsesResult].
         * @since 0.9.7
         */
        fun make(o: Any): ParsesResult {
            if (o is TotalParsesFailureNode) return o
            if (o is LazyResultList) return o
            if (o is ParsesFailureResult) return o
            if (o is ParseFailure) return ParsesFailureResult(o)
            throw IllegalArgumentException(o.javaClass.toString())
        }
    }
}
