package de.kleinert.parsewisp.result

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.collections.FlatResultSeq
import de.kleinert.parsewisp.result.Node.NodeParseTree
import de.kleinert.parsewisp.result.Node.NodeTreeTag

/**
 * This class represents parse trees. Throughout the documentation, trees are typically notated as lists.
 */
class ParseTree private constructor(
    /**
     * The head of the tree. That is the name of the production used to generate the tree.
     *
     *
     * E.g. for the tree `[:S, "a", "b", "c"]`, the tag is `:S`.
     *
     * <pre>
     * `var pt = Parsewisp.parser("S := 'a' 'b' 'c'").parse("abc");     println(pt); // [:S, a, b, c]     println(pt.castToParseSuccess().getTag().content()); // :S `
    </pre> *
     *
     * @return The tag of the tree.
     */
    val tag: NodeTreeTag,
    /**
     * The content of the tree, without the tag.
     *
     * <pre>
     * `var pt = Parsewisp.parser("S := 'a' 'b' 'c'").parse("abc");     println(pt); // [:S, a, b, c]     println(pt.castToParseSuccess().getContent()); // [a, b, c] `
    </pre> *
     *
     * @return The content of the tree.
     */
    val content: List<Node>,
    /**
     * The starting index in the input string of the parse (inclusive).
     *
     * @return The starting index in the input string of the parse (inclusive).
     */
    val spanStart: Int,
    /**
     * The end index in the input string of the parse (exclusive).
     *
     * @return The end index in the input string of the parse (exclusive).
     */
    val spanEndExclusive: Int
) : List<Node>, ParseResult {
    private var hashCode = 0

    init {
        require(!((spanStart == -1 && spanEndExclusive != -1) || (spanStart != -1 && spanStart > spanEndExclusive))) { "Invalid span $spanStart to $spanEndExclusive." }
    }

    /**
     * Equivalent to `content.get(index)`.
     *
     * @param index The index in content.
     * @return A node.
     */
    fun getNode(index: Int): Node {
        return this.content[index]
    }

    /**
     * Returns the tag ([ParseTree.tag]) and content ([ParseTree.content]) into a single list.
     * This method is not recursive, ie does not apply to subtrees.
     *
     * @return The tree as a list of nodes.
     */
    fun toList(): List<Node> {
        val alist = mutableListOf<Node>()
        if (tag != NULL_TAG) alist.add(tag)
        alist.addAll(content)
        return alist.toList()
    }

    /**
     * Converts the tree into a nested list. Unlike [.toList], this method is recursive.
     *
     * @return A nested list of Objects.
     */
    fun toRawList(): List<Any?> {
        var i = 0

        val l: Array<Any?>
        if (tag == NULL_TAG) {
            l = arrayOfNulls(content.size)
        } else {
            l = arrayOfNulls(content.size + 1)
            l[i++] = tag.content
        }

        for (node in content) {
            val c = node.content()
            if (c is ParseTree) {
                l[i++] = c.toRawList()
            } else {
                l[i++] = c
            }
        }

        return l.toList()
    }

    /**
     * Given a string (hopefully the one this tree was parsed from), will output the substring which is covered by this tree.
     * <pre>
     * `var p = Parsewisp.parser("S = 'b' A 'n'\nA = 'Aa'");     var tree = p.parse("bAan").castToParseSuccess();     var subTree = (ParseTree) tree.getContent().get(1).content();     System.out.println(tree);                            // [:S, b, [:A, Aa], n]     System.out.println(subTree);                         // [:A, Aa]     System.out.println(subTree.getSpanStart());          // 1     System.out.println(subTree.getSpanEndExclusive());   // 3     System.out.println(subTree.containedString("bAan")); // Optional[Aa] `
    </pre> *
     *
     * @param s Input string.
     * @return An [Optional] containing the string if present or an empty [Optional] if the string is not covered by this tree.
     * @throws IllegalArgumentException if this tree has no span specified.
     */
    fun containedString(s: String): String? {
        if (spanEndExclusive == -1) return null
        require(s.length >= spanEndExclusive)
        return s.substring(spanStart, spanEndExclusive)
    }

    override fun toString(): String {
        return toList().toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is List<*>) {
            return false
        }
        if (other is ParseTree) {
            return this.tag == other.tag && this.content == other.content
        }
        val otherIter = other.iterator()
        if (!otherIter.hasNext()) return false

        for (thisNext in this) {
            if (!otherIter.hasNext()) return false
            val otherNext: Any = otherIter.next()!!
            if (thisNext != otherNext) return false
        }

        return !otherIter.hasNext()
    }

    /* SECTION: Methods that allow treating the tree like a list. */
    override val size: Int
        get() = content.size + 1

    override fun isEmpty(): Boolean {
        return false
    }

    override fun contains(element: Node): Boolean {
        return tag == element || content.contains(element)
    }

    override fun iterator(): Iterator<Node> {
        return object : Iterator<Node> {
            var delegate: Iterator<Node>? = null

            override fun hasNext(): Boolean {
                return delegate == null || delegate!!.hasNext()
            }

            override fun next(): Node {
                if (delegate == null) {
                    delegate = content.iterator()
                    return tag
                } else {
                    return delegate!!.next()
                }
            }
        }
    }

    override fun containsAll(elements: Collection<Node>): Boolean {
        return toList().containsAll(elements)
    }

    override fun hashCode(): Int {
        if (hashCode != 0) return hashCode
        var hc = 1
        for (e in this) hc = hc * 31 + e.hashCode()
        hashCode = hc
        return hc
    }

    override fun get(index: Int): Node {
        if (index == 0) return tag
        return content[index - 1]
    }

    override fun indexOf(element: Node): Int {
        if (tag == element) return 0
        val i = content.indexOf(element)
        if (i < 0) return i
        return i + 1
    }

    override fun lastIndexOf(element: Node): Int {
        val i = content.lastIndexOf(element)
        if (i >= 0) return i + 1
        if (tag == element) return 0
        return -1
    }

    override fun listIterator(): ListIterator<Node> {
        return toList().listIterator()
    }

    override fun listIterator(index: Int): ListIterator<Node> {
        return toList().listIterator()
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Node> {
        if (fromIndex == 1 && toIndex == content.size + 1) return content
        return toList().subList(fromIndex, toIndex)
    }

    companion object {
        /**
         * A technically invalid tag. It is used to mark trees "without" a tag. Such trees should always be subtrees of other parse trees and are "flattened" when used. See [.create]
         */
        var NULL_TAG: NodeTreeTag = NodeTreeTag(Sym.sym("\u0000\u0000\u0000\u0000"))

        /**
         * Creates a parse tree from a tag and content.
         *
         *
         * If {@param content} is null, the content list of the tree will be empty. If {@param content} is a [FlatResultSeq], it becomes the content of the tree. Otherwise, {@param content} becomes a singleton list.
         *
         * @param tag       The tag as a node.
         * @param content   The content as a node.
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
            val afs = when (content) {
                null -> listOf()
                is FlatResultSeq -> content.toList().map { o: Any? -> Node.of(o) }
                is String, is TotalParsesFailureNode,is ParseFailureNode,is ParseTree -> listOf(Node.of(content))
                is List<*> -> content.map { o: Any? -> Node.of(o) }
                else -> listOf(Node.of(content))
            }
            return create(NodeTreeTag(tag), afs, spanStart, spanEnd)
        }

        /**
         * Creates a parse tree from a tag and content.
         *
         *
         * This method also "flattens" nested trees if necessary. Trees need to be flattened if the content includes any subtrees that have the [NULL_TAG].
         *
         *
         * E.g. `[:S, "A", [:S, "A"]]` is flat, but `[:S, [NULL_TAG, "A"], [:S, "A"]]` is not.
         * This happens when hide-tags are used, for example in the grammar `"S := A S | EPS \n <A> := 'A'"`.
         *
         * @param tag       The tag as a node.
         * @param content   The content as a node.
         * @param spanStart Starting index in the input (inclusive).
         * @param spanEnd   End index in the input (exclusive).
         * @return A new parse tree.
         */
        fun create(
            tag: NodeTreeTag,
            content: List<Node>,
            spanStart: Int,
            spanEnd: Int
        ): ParseTree {
            // If the content contains a ParseTree which has the NULL_TAG, it has to be "flattened".
            var isFlat = true
            for (node in content) {
                if (node is NodeParseTree
                    && node.content.tag == NULL_TAG
                ) {
                    // Subtree with NULL_TAG found. Must merge.
                    isFlat = false
                    break
                }
            }
            if (isFlat) {
                return ParseTree(tag, content, spanStart, spanEnd)
            }

            // The rest of this method handles the case that there is an unflattened subtree.
            val entries = mutableListOf<Node>()
            for (e in content) {
                if (e !is NodeParseTree) {
                    entries.add(e)
                    continue
                }

                val subTree = e.content()

                if (subTree.tag == NULL_TAG) {
                    entries.addAll(subTree.content)
                } else {
                    entries.add(NodeParseTree(subTree))
                }
            }

            return ParseTree(tag, entries, spanStart, spanEnd)
        }
    }
}
