package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

internal class ParseTreeSpanTest {
    @Test
    fun testBasics() {
        val p = parser("S = 'b' A 'n'\nA = 'Aa'")
        val tree = p.parse("bAan").castToParseSuccess()

        Assertions.assertEquals(
            PT.create("S", "b", PT.create("A", "Aa"), "n"),
            tree
        )
        Assertions.assertEquals(0, tree.spanStart)
        Assertions.assertEquals(4, tree.spanEndExclusive)

        val spannedStringInTree = tree.containedString("bAan")
        Assertions.assertTrue(spannedStringInTree.isPresent())
        Assertions.assertEquals("bAan", spannedStringInTree.get())
    }

    @Test
    fun testInSubTree() {
        val p = parser("S = 'b' A 'n'\nA = 'Aa'")
        val tree = p.parse("bAan").castToParseSuccess()
        val subTree = tree.getNode(1).tree()

        Assertions.assertEquals(PT.create("A", "Aa"), subTree)
        Assertions.assertEquals(1, subTree.spanStart)
        Assertions.assertEquals(3, subTree.spanEndExclusive)

        val spannedStringInSubTree = subTree.containedString("bAan")
        Assertions.assertTrue(spannedStringInSubTree.isPresent())
        Assertions.assertEquals("Aa", spannedStringInSubTree.get())
    }

    @Test
    fun testEmptySpan() {
        val p = parser("S = ε")
        val tree = p.parse("").castToParseSuccess()

        Assertions.assertEquals(0, tree.spanStart)
        Assertions.assertEquals(0, tree.spanEndExclusive)
    }

    @Test
    fun testNoSpan() {
        val tree = PT.create("S", "a", "b", "cdef")

        Assertions.assertTrue(tree.spanStart < 0)
        Assertions.assertTrue(tree.spanEndExclusive < 0)
        Assertions.assertTrue(tree.containedString("abcdef").isEmpty())
    }

    @Test
    fun testInvalidStringCheck() {
        val p = parser("S = 'b' A 'n'\nA = 'Aa'")
        val tree = p.parse("bAan").castToParseSuccess()
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            Executable { tree.containedString("ban") })
    }

    @Test
    fun testOtherString() {
        val p = parser("S = 'b' A 'n'\nA = 'Aa'")
        val tree = p.parse("bAan").castToParseSuccess()
        Assertions.assertEquals("aaaa", tree.containedString("aaaaaaan").orElseThrow())
    }
}
