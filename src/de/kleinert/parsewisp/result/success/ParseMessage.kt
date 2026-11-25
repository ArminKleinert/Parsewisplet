package de.kleinert.parsewisp.result.success

import de.kleinert.parsewisp.collections.FlatResultSeq
import de.kleinert.parsewisp.result.ParseFailureNode
import de.kleinert.parsewisp.result.ParseTree
import java.util.*

/**
 * A parsed "result" consists of an index and an object carrying more information.
 * Usually, the wrapped object is a [ParseTree] or a string. But this class is also used to transport other types of objects.
 * For specifics, see [ParseMessage.result].
 *
 * @since 0.9.7
 */
class ParseMessage
/**
 * Simple, abstract constructor taking only the index. The wrapped object is defined in the specific subclasses.
 *
 * @param index The index.
 * @since 0.9.7
 */ private constructor(
    private val index: Int,
    /**
     * The wrapped object.
     *
     *
     * This is always `null` or an instance of [String], [ParseTree], [ParseFailureNode] or [FlatResultSeq].
     * If the object is a [FlatResultSeq], the contents are any of the classes mentioned above.
     *
     * @return The wrapped object.
     * @since 0.9.7
     */
    val result: Any?
) {
    override fun equals(o: Any?): Boolean {
        if (o !is ParseMessage) return false
        return index == o.index && this.result == o.result
    }

    override fun hashCode(): Int {
        return Objects.hash(index, this.result)
    }

    /**
     * The index of the message object.
     *
     * @return The index as an integer.
     * @since 0.9.7
     */
    fun index(): Int {
        return index
    }

    /**
     * Creates a new instance with the current index.
     *
     * @return A new instance with the current index.
     * @since 0.9.7
     */
    fun reset(): ParseMessage {
        return create(index)
    }

    override fun toString(): String {
        return StringJoiner(", ", ParseMessage::class.java.getSimpleName() + "[", "]")
            .add("index=" + index)
            .add("result=" + result)
            .toString()
    }

    companion object {
        /**
         * Creates an instance wrapping a ParseTree.
         *
         * @param index  The last index of the previous parse, exclusive.
         * @param result The [ParseTree].
         * @return The [ParseTree] wrapped into an instance of this class.
         * @since 0.9.7
         */
        fun create(index: Int, result: ParseTree): ParseMessage {
            return ParseMessage(index, result)
        }

        /**
         * Creates an instance wrapping a String.
         *
         * @param index  The last index of the previous parse, exclusive.
         * @param result The String.
         * @return The String wrapped into an instance of this class.
         * @since 0.9.7
         */
        fun create(index: Int, result: String): ParseMessage {
            return ParseMessage(index, result)
        }

        /**
         * Creates an instance wrapping a nothing. This is done when a rule finished successfully, but does not need to carry information.
         *
         * @param index The last index of the previous parse, exclusive.
         * @return An instance wrapping null.
         * @since 0.9.7
         */
        fun create(index: Int): ParseMessage {
            return ParseMessage(index, null)
        }

        /**
         * Creates an instance wrapping a FlatSeq.
         *
         * @param index  The last index of the previous parse, exclusive.
         * @param result The [FlatResultSeq].
         * @return The [FlatResultSeq] wrapped into an instance of this class.
         * @since 0.9.7
         */
        fun create(index: Int, result: FlatResultSeq): ParseMessage {
            return ParseMessage(index, result)
        }

        /**
         * Creates an instance wrapping any type that is not covered by any of the other variants of this factory-method..
         *
         * @param index  The last index of the previous parse, exclusive.
         * @param result The object.
         * @return The object wrapped into an instance of this class.
         * @since 0.9.7
         */
        fun create(index: Int, result: Any): ParseMessage {
            return ParseMessage(index, result)
        }

        /**
         * Creates an instance wrapping a ParseFailureNode.
         *
         * @param index  The last index of the previous parse, exclusive.
         * @param result The [ParseFailureNode].
         * @return The [ParseFailureNode] wrapped into an instance of this class.
         * @since 0.9.7
         */
        fun create(index: Int, result: ParseFailureNode): ParseMessage {
            return ParseMessage(index, result)
        }
    }
}
