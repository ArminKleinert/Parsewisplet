package de.kleinert.parsewisp.result

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.collections.FlatResultSeq
import de.kleinert.parsewisp.result.Node.NodeParseTree
import de.kleinert.parsewisp.result.Node.NodeTreeTag
import java.util.Optional

/**
 * This class represents parse trees. Throughout the documentation, trees are typically notated as lists.
 *
 * @since 0.9.7
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
     * @since 0.9.7
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
     * @since 0.9.7
     */
    val content: List<Node>,
    spanStart: Int,
    spanEndExclusive: Int
) : List<Node>, ParseResult {
    private var hashCode = 0

    /**
     * The starting index in the input string of the parse (inclusive).
     * <pre>
     * `var p = Parsewisp.parser("S = 'b' A 'n'\nA = 'A'");     var tree = p.parse("bAn").castToParseSuccess();     System.out.println(tree.toString());            // [:S, b, [:A, A], n]     System.out.println(tree.getSpanStart());        // 0     System.out.println(tree.getSpanEndExclusive()); // 3 `
    </pre> *
     *
     * @return The starting index in the input string of the parse (inclusive).
     * @since 0.9.7
     */
    @JvmField
    val spanStart: Int

    /**
     * The end index in the input string of the parse (exclusive).
     * <pre>
     * `var p = Parsewisp.parser("S = 'b' A 'n'\nA = 'A'");     var tree = p.parse("bAn").castToParseSuccess();     System.out.println(tree.toString());            // [:S, b, [:A, A], n]     System.out.println(tree.getSpanStart());        // 0     System.out.println(tree.getSpanEndExclusive()); // 3 `
    </pre> *
     *
     * @return The end index in the input string of the parse (exclusive).
     * @since 0.9.7
     */
    @JvmField
    val spanEndExclusive: Int

    init {
        require(!((spanStart == -1 && spanEndExclusive != -1) || (spanStart != -1 && spanStart > spanEndExclusive))) { "Invalid span " + spanStart + " to " + spanEndExclusive + "." }

        this.spanStart = spanStart
        this.spanEndExclusive = spanEndExclusive
    }

    /**
     * Equivalent to `getContent().get(index)`.
     *
     * @param index The index in content.
     * @return A node.
     * @since 0.9.7
     */
    fun getNode(index: Int): Node {
        return this.content.get(index)
    }

    /**
     * Returns the tag ([.getTag] and content ([.getContent]) into a single list.
     * This method is not recursive, ie does not apply to subtrees.
     *
     * @return The tree as a list of nodes.
     * @since 0.9.7
     */
    fun toList(): List<Node> {
        val alist: MutableList<Node> = ArrayList()
        if (tag != NULL_TAG) alist.add(tag)
        alist.addAll(content)
        return alist.toList()
    }

    /**
     * Converts the tree into a nested list. Unlike [.toList], this method is recursive.
     *
     * @return A nested list of Objects.
     * @since 0.9.7
     */
    fun toRawList(): List<Any?> {
        var i = 0

        val l: Array<Any?>
        if (tag == NULL_TAG) {
            l = arrayOfNulls<Any>(content.size)
        } else {
            l = arrayOfNulls<Any>(content.size + 1)
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
     * @since 0.9.7
     */
    fun containedString(s: String): Optional<String> {
        if (spanEndExclusive == -1) return Optional.empty<String>()
        require(s.length >= spanEndExclusive)
        return Optional.of<String>(s.substring(spanStart, spanEndExclusive))
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

    override fun contains(o: Node): Boolean {
        return tag == o || content.contains(o)
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

    override fun get(i: Int): Node {
        if (i == 0) return tag
        return content.get(i - 1)
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

    override fun listIterator(i: Int): ListIterator<Node> {
        return toList().listIterator()
    }

    override fun subList(i: Int, i1: Int): List<Node> {
        if (i == 1 && i1 == content.size + 1) return content
        return toList().subList(i, i1)
    }

    companion object {
        /**
         * A technically invalid tag. It is used to mark trees "without" a tag. Such trees should always be subtrees of other parse trees and are "flattened" when used. See [.create]
         *
         * @since 0.9.7
         */
        var NULL_TAG: NodeTreeTag = NodeTreeTag(sym("\u0000\u0000\u0000\u0000"))

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
         * @since 0.9.7
         */
        /**
         * Creates a parse tree from a tag and content.
         *
         *
         * If {@param content} is null, the content list of the tree will be empty. If {@param content} is a [FlatResultSeq], it becomes the content of the tree. Otherwise, {@param content} becomes a singleton list.
         *
         * @param tag     The tag as a node.
         * @param content The content as a node.
         * @return A new parse tree.
         * @since 0.9.7
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            tag: Sym,
            content: Any?,
            spanStart: Int = -1,
            spanEnd: Int = -1
        ): ParseTree {
            val afs: List<Node>
            if (content == null) {
                afs = listOf<Node>()
            } else if (content is FlatResultSeq) {
                afs = content.stream().map { o: Any? -> Node.Companion.of(o) }.toList()
            } else if (content is String) {
                afs = listOf<Node>(Node.Companion.of(content))
            } else if (content is TotalParsesFailureNode) {
                afs = listOf<Node>(Node.Companion.of(content))
            } else if (content is ParseFailureNode) {
                afs = listOf<Node>(Node.Companion.of(content))
            } else if (content is ParseTree) {
                afs = listOf<Node>(Node.Companion.of(content))
            } else if (content is MutableList<*>) {
                afs = content.map { o: Any? -> Node.Companion.of(o) }
            } else {
                //throw new IllegalArgumentException(content.getClass().toString());
                afs = listOf<Node>(Node.Companion.of(content))
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
         * @since 0.9.7
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
            val entries = ArrayList<Node>()
            for (e in content) {
                if (e !is NodeParseTree) {
                    entries.add(e)
                    continue
                }

                val subTree = e.content() as ParseTree

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
