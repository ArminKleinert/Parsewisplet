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
        transformMap: MutableMap<Sym?, Function<MutableList<Any?>?, Any?>?>
    ): Any {
        if (node is NodeParseTree) return Transform.transform(node.content, transformMap)
        return node.content()
    }

    private fun transform(
        parseTree: ParseTree,
        transformMap: MutableMap<Sym?, Function<MutableList<Any?>?, Any?>?>
    ): Any {
        val tagSym = parseTree.getTag().content
        val transformFn = transformMap.get(tagSym)
        val transformedNodes = parseTree.getContent().stream()
            .map<Any?> { node: Node? -> Transform.transform(node!!, transformMap) }.toList()
        if (transformFn != null) {
            return transformFn.apply(transformedNodes)!!
        } else {
            return ParseTree.create(parseTree.getTag().content, transformedNodes)
        }
    }

    /**
     * Transforms a [ParseTree] through application of a transformation-map.
     *
     * <pre>
     * `var p = Parsewisp.parser("""             S := NUM ('+' NUM)*             NUM := #'\\d+'             """);     var tree = p.parse("1+2+3").castToParseSuccess();     Map<Sym,Function<List<Object>, Object>> transformMap = Map.of(             Sym.sym("S"), o -> o.stream()                     .filter(it -> !it.equals("+")) // If the token is "+", ignore.                     .map(it -> (String) it) // Treat each as String.                     .mapToInt(Integer::parseInt) // Parse each String to Int.                     .sum(), // Sum.             Sym.sym("NUM"), List::getFirst     );     Function<Object, Integer> finalizer = (o) -> (Integer)o;     // The tree is [:S, [:NUM, "1"], "+", [:NUM, "2"], "+", [:NUM, "3"]].     // The transformation sees the tag :S and looks up the function. It starts walking the content.     //   The content is [[:NUM, "1"], "+", [:NUM, "2"], "+", [:NUM, "3"]].     //   First, `.filter(it -> !it.equals("+"))` removes all occurences of "+".     //   The content is now [[:NUM, "1"], [:NUM, "2"], [:NUM, "3"]].     //   While walking, `List::getFirst` is applied to the content of each subtree.     //   The content is now ["1", "2", "3"].     //   The function applies `Integer::parseInt` to each element.     //   The content is now [1, 2, 3].     //   The function now sums up the numbers: 1+2+3 = 6.     //   Lastly, the finalizer is applied, casting the default to an Integer.     Assertions.assertEquals(             Integer.valueOf(6),             Transform.transform(tree, transformMap, finalizer)); `
    </pre> *
     * Simplified pseudocode for the above:
     * <pre>
     * `Map<Sym,Function<List<Object>, Object>> transformMap = Map.of(             Sym.sym("S"), o -> o.filter(it != "+").map(it.toInt()).sum(),             Sym.sym("NUM"), List::getFirst     ); `
    </pre> *
     *
     *
     * If a tag is not in the Map, a parse tree is constructed.
     * In this case, all nodes in the content MUST have a corresponding [Node] wrapper ([ParseTree] or [String])!
     * <pre>
     * `var p = Parsewisp.parser("""             S := A             A := NUM ('+' NUM)*             NUM := #'\\d+'             """);     var tree = p.parse("1+2+3").castToParseSuccess();     Map<Sym,Function<List<Object>, Object>> transformMap = Map.of(             Sym.sym("A"), o -> String.valueOf(o.stream()                     .filter(it -> !it.equals("+")) // If the token is "+", ignore.                     .map(it -> (String) it) // Treat each as String.                     .mapToInt(Integer::parseInt) // Parse each String to Int.                     .sum()), // Sum.             Sym.sym("NUM"), List::getFirst     );     Function<Object, Integer> finalizer = (o) -> (Integer)o;     Assertions.assertEquals(             ParseTree.create("S", "6"),             Transform.transform(tree, transformMap)); `
    </pre> *
     *
     * @param parseResult  The parse result (expected to be a [ParseTree]).
     * @param transformMap Map of tags (symbols) to functions.
     * @param finalizer    Casting from raw Object to specific output type.
     * @param <T>          The output type.
     * @return Transformed content.
     * @throws IllegalArgumentException If the parseResult is not a parse tree.
     * @since 0.9.7
    </T> */
    fun <T> transform(
        parseResult: ParseResult,
        transformMap: MutableMap<Sym?, Function<MutableList<Any?>?, Any?>?>,
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
        transformMap: MutableMap<Sym?, Function<MutableList<Any?>?, Any?>?>
    ): Any {
        require(parseResult is ParseTree) { parseResult.toString() }
        val tagSym = parseResult.getTag().content
        val transformedNodes = parseResult
            .getContent().stream()
            .map<Any?> { node: Node? -> Transform.transform(node!!, transformMap) }
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
