package de.kleinert.parsewisp.testutil

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.result.ParseTree

/**
 * Helper functions for creating and handling parse trees in test cases.
 */
object PT {
    /**
     * Creates a parse tree from a tag and content.
     *
     *
     * If {@param content} is null, the content list of the tree will be empty. If {@param content} is a [de.kleinert.parsewisp.collections.FlatResultSeq], it becomes the content of the tree. Otherwise, {@param content} becomes a singleton list.
     *
     * @param tag     The tag as a node.
     * @param content The content as a node.
     * @param spanStart Starting index in the input (inclusive).
     * @param spanEnd   End index in the input (exclusive).
     * @return A new parse tree.
     */
    fun create(
        tag: Sym,
        content: Any?,
        spanStart: Int = -1,
        spanEnd: Int = -1
    ): ParseTree {
        return ParseTree.create(tag, content, spanStart, spanEnd)
    }

    /**
     * Convenience method for creating trees.
     *
     * @param tag     The tag as a string.
     * @param content The content as variadic arguments.
     * @return A new parse tree.
     */
    fun create(tag: String, vararg content: Any): ParseTree {
        return create(sym(tag), content.toList())
    }
}
