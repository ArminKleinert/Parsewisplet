package de.kleinert.parsewisp.util

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.result.Node
import de.kleinert.parsewisp.result.Node.NodeParseTree
import de.kleinert.parsewisp.result.ParseResult
import de.kleinert.parsewisp.result.ParseTree
import java.util.function.Function

/**
 * This class provides methods for transforming parse trees via tag-to-function tables.
 *
 * @since 0.9.7
 */
object Transform {
    private fun transform(
        node: Node,
        transformMap: Map<Sym, Function<List<Any?>, Any?>>
    ): Any {
        if (node is NodeParseTree) return Transform.transform(node.content, transformMap)
        return node.content()
    }

    private fun transform(
        parseTree: ParseTree,
        transformMap: Map<Sym, Function<List<Any?>, Any?>>
    ): Any {
        val tagSym = parseTree.tag.content
        val transformFn = transformMap.get(tagSym)
        val transformedNodes = parseTree.content.stream()
            .map<Any?> { node: Node? -> transform(node!!, transformMap) }.toList()
        return if (transformFn != null) {
            transformFn.apply(transformedNodes)!!
        } else {
            ParseTree.create(parseTree.tag.content, transformedNodes)
        }
    }

    /**
     * Transforms a [ParseTree] through application of a transformation-map.
     * If a tag is not in the Map, a parse tree is constructed.
     * In this case, all nodes in the content MUST have a corresponding [Node] wrapper ([ParseTree] or [String])!
     *
     * @param parseResult  The parse result (expected to be a [ParseTree]).
     * @param transformMap Map of tags (symbols) to functions.
     * @param finalizer    Casting from raw Object to specific output type.
     * @param <T>          The output type.
     * @return Transformed content.
     * @throws IllegalArgumentException If the parseResult is not a parse tree.
     * @since 0.9.7
     */
    fun <T> transform(
        parseResult: ParseResult,
        transformMap: Map<Sym, Function<List<Any?>, Any?>>,
        finalizer: Function<Any?, T?>
    ): T? {
        return finalizer.apply(transform(parseResult, transformMap))
    }

    /**
     * Same as [.transform] but without a finalizer. The output is a plain object.
     *
     * @param parseResult  The parse result (expected to be a [ParseTree]).
     * @param transformMap Map of tags (symbols) to functions.
     * @return Transformed content.
     * @throws IllegalArgumentException If the parseResult is not a parse tree.
     * @since 0.9.7
     */
    fun transform(
        parseResult: ParseResult,
        transformMap: Map<Sym, Function<List<Any?>, Any?>>
    ): Any {
        require(parseResult is ParseTree) { parseResult.toString() }
        val tagSym = parseResult.tag.content
        val transformedNodes = parseResult
            .content.stream()
            .map<Any?> { node: Node? -> transform(node!!, transformMap) }
            .toList()
        val transformFn = transformMap.get(tagSym)
        val result: Any
        if (transformFn != null) {
            result = transformFn.apply(transformedNodes)!!
        } else {
            val resultL = ArrayList<Any?>()
            resultL.add(tagSym)
            resultL.addAll(transformedNodes)
            result = resultL
        }
        return result
    }
}
