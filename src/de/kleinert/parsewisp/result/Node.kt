package de.kleinert.parsewisp.result

import de.kleinert.parsewisp.Sym

/**
 * This type is used for the parts of a [ParseTree]. An instance of this class wraps an object that can be visible inside a parse tree. Namely, these types are:
 *
 *  * [NodeString] wraps String.
 *  * [NodeParseTree] wraps [ParseTree].
 *  * [NodeImpl] wraps [ParseFailureNode]. This happens only if failures are embedded in parse trees.
 *  * [NodeTreeTag] wraps [String] (or anything else that represents text) and must be the head of a parse tree.
 *
 *
 * @since 0.9.7
 */
interface Node {
    /**
     * The inner object.
     *
     * @return The inner object.
     * @since 0.9.7
     */
    fun content(): Any

    /**
     * Casts the node to a [NodeString] and returns its content, treated as a String.
     *
     * @return The content as a string.
     * @throws ClassCastException If the node is not a [NodeString].
     * @since 0.9.7
     */
    fun string(): String {
        return content() as String
    }

    /**
     * Casts the node to a [NodeParseTree] and returns its content, treated as a [ParseTree].
     *
     * @return The content as a [ParseTree].
     * @throws ClassCastException If the node is not a [NodeParseTree].
     * @since 0.9.7
     */
    fun tree(): ParseTree {
        return content() as ParseTree
    }

    /**
     * Represents the tag of a tree. This is the left hand side of a production. A tag can not exist without a tree and a tree can not exist without a tag.
     *
     * @param content The inner object.
     * @since 0.9.7
     */
    data class NodeTreeTag(val content: Sym) : Node {
        override fun toString(): String {
            return this.content.toString()
        }

        override fun content() = content
    }

    /**
     * Wraps a [ParseTree].
     *
     * @param content The inner object.
     * @since 0.9.7
     */
    data class NodeParseTree(val content: ParseTree) : Node {
        override fun toString(): String {
            return this.content.toString()
        }

        override fun content() = content
    }

    /**
     * Wraps a [String].
     *
     * @param content The inner object.
     * @since 0.9.7
     */
    data class NodeString(val content: String) : Node {
        override fun toString(): String {
            return '"'.toString() + this.content + '"'
        }

        override fun content() = content
    }

    /**
     * Wraps an object and can be used to embed failures into the tree.
     *
     * @param content The inner object.
     * @since 0.9.7
     */
    data class NodeImpl(val content: Any) : Node {
        override fun toString(): String {
            return this.content.toString()
        }

        override fun content() = content
    }

    companion object {
        /**
         * Takes an object and returns a node representing that object.
         *
         * @param o The object.
         * @return A node.
         * @since 0.9.7
         */
        fun of(o: Any?): Node {
            requireNotNull(o) { "Input cannot be null." }
            if (o is ParseTree) return NodeParseTree(o)
            if (o is String) return NodeString(o)
            if (o is Node) return o
            return NodeImpl(o)
        }
    }
}
