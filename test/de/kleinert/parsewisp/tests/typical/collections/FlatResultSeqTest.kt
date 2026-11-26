package de.kleinert.parsewisp.tests.typical.collections

import de.kleinert.parsewisp.collections.FlatResultSeq
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class FlatResultSeqTest {
    @Test
    fun isEmpty() {
        Assertions.assertTrue(FlatResultSeq.make().isEmpty)

        val frs12 = FlatResultSeq.make().appendOrConcat(1).appendOrConcat(2)
        Assertions.assertFalse(frs12.isEmpty)

        val frs1212 = frs12.appendOrConcat(frs12)
        Assertions.assertFalse(frs1212.isEmpty)

        val frs12123 = frs1212.appendOrConcat(3)
        Assertions.assertFalse(frs12123.isEmpty)
    }

    @Test
    fun size() {
        Assertions.assertEquals(0, FlatResultSeq.make().size())

        val frs12 = FlatResultSeq.make().appendOrConcat(1).appendOrConcat(2)
        Assertions.assertEquals(2, frs12.size())

        val frs1212 = frs12.appendOrConcat(frs12)
        Assertions.assertEquals(4, frs1212.size())

        val frs12123 = frs1212.appendOrConcat(3)
        Assertions.assertEquals(5, frs12123.size())
    }

    @Test
    fun stream() {
        Assertions.assertEquals(ArrayList<Any?>(), FlatResultSeq.make().toList())

        val frs12 = FlatResultSeq.make().appendOrConcat(1).appendOrConcat(2)
        val frs121231212 = frs12.appendOrConcat(frs12).appendOrConcat(3).appendOrConcat(frs12).appendOrConcat(frs12)

        Assertions.assertEquals(
            mutableListOf<Int?>(2, 2, 2, 2),
            frs121231212.toList().filter { it: Any? -> (it as Int?)!! % 2 == 0 }
        )

        Assertions.assertEquals(
            mutableListOf<Int?>(1, 2, 1, 2, 3, 1, 2, 1, 2),
            frs121231212.toList()
        )

        Assertions.assertEquals(
            frs121231212.toList(),
            frs121231212.toList()
        )
    }

    @Test
    fun bigTest() {
        val numElements = 30000
        var frs = FlatResultSeq.make()
        val lst = ArrayList<Any?>()
        for (i in 0..<numElements) {
            frs = frs.appendOrConcat(i)
            lst.add(i)
        }
        Assertions.assertEquals(lst, frs.toList())
    }
}