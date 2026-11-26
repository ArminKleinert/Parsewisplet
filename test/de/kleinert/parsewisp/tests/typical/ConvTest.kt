package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.result.ParseFailureNode
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ConvTest {
    @Test
    fun listToTreeTest() {
        val l = listOf(Sym.sym("S"), "A", listOf(Sym.sym("S"), "A"))
        val pt: ParseTree = toParseTree(l)
        Assertions.assertEquals(
            PT.create("S", "A", PT.create("S", "A")),
            pt
        )
    }

    @Test
    fun mapToTreeTest() {
        val l = mapOf(
            Sym.sym("tag") to Sym.sym("S"),
            Sym.sym("content") to listOf(
                "A",
                mapOf(Sym.sym("tag") to Sym.sym("S"), Sym.sym("content") to listOf<String?>("A"))
            )
        )
        val pt: ParseTree = toParseTree(l)
        Assertions.assertEquals(
            PT.create("S", "A", PT.create("S", "A")),
            pt
        )
    }

    @Test
    fun mixedToTreeTest() {
        val pt1: ParseTree = toParseTree(
            mapOf(
                Sym.sym("tag") to Sym.sym("S"),
                Sym.sym("content") to listOf("A", listOf(Sym.sym("S"), "A"))
            )
        )
        Assertions.assertEquals(
            PT.create("S", "A", PT.create("S", "A")),
            pt1
        )

        val pt2: ParseTree = toParseTree(
            listOf(
                Sym.sym("S"),
                "A",
                mapOf(
                    Sym.sym("tag") to Sym.sym("S"),
                    Sym.sym("content") to mutableListOf<String?>("A")
                )
            )
        )
        Assertions.assertEquals(
            PT.create("S", "A", PT.create("S", "A")),
            pt2
        )
    }

    companion object {
        private fun toParseTreeHelper(o: List<*>): List<Any?> {
            return o.map {
                when (it) {
                    is String, is ParseFailureNode, is ParseTree -> return@map it
                    is MutableList<*> -> return@map toParseTree(it)
                    is MutableMap<*, *> -> return@map toParseTree(it)
                    else -> throw IllegalArgumentException()
                }
            }.toList()
        }

        /**
         * Transforms Lists into parse trees.
         *
         *  * Format `[Sym, ...]` becomes an equivalent parse tree.
         *  * Format `[...]` (where the first is not a [Sym]) becomes a parse tree with the tag [ParseTree.NULL_TAG].
         *
         * All elements after the first must be Maps, Lists, or any type which have wrappers in the [de.kleinert.parsewisp.result.Node] interface.
         *
         * @param l The list.
         * @return A parse tree.
         */
        fun toParseTree(l: List<*>): ParseTree {
            if (l.isEmpty()) return ParseTree.create(ParseTree.NULL_TAG.content, mutableListOf<Any?>())
            if (l[0] !is Sym) return ParseTree.create(ParseTree.NULL_TAG.content, toParseTreeHelper(l))
            return ParseTree.create((l[0] as Sym?)!!, toParseTreeHelper(l.subList(1, l.size)))
        }

        /**
         * Converts a Map to a ParseTree. The format is as follows: `{:tag Sym, :content List<Object>}`
         *
         * @param m The map.
         * @return A parse tree.
         */
        fun toParseTree(m: Map<*, *>): ParseTree {
            require(!(m.size != 2 || (m[Sym.sym("tag")] !is Sym) || (m[Sym.sym("content")] !is MutableList<*>))) { "Cannot handle Map $m" }
            return ParseTree.create(
                m[Sym.sym("tag")] as Sym,
                toParseTreeHelper(m[Sym.sym("content")] as MutableList<*>)
            )
        }
    }
}
