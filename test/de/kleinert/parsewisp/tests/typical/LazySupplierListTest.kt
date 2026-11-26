package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.collections.LazySupplierList
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

internal class LazySupplierListTest {
    @Test
    fun simpleTest() {
        val lsl = LazySupplierList<Int?>({ i: Int -> if (i < 15) i else null }, Int.MAX_VALUE)

        Assertions.assertFalse(lsl.isFullyEvaluated)

        Assertions.assertEquals(0, lsl.first())
        Assertions.assertEquals(5, lsl.get(5))
        Assertions.assertEquals(15, lsl.size)
        Assertions.assertEquals(5, lsl.get(5))

        Assertions.assertTrue(lsl.isFullyEvaluated)

        Assertions.assertThrows<IndexOutOfBoundsException?>(
            IndexOutOfBoundsException::class.java,
            Executable { lsl.get(-1) })
        Assertions.assertThrows<IndexOutOfBoundsException?>(
            IndexOutOfBoundsException::class.java,
            Executable { lsl.get(15) })
    }

    @Test
    fun emptyTest() {
        val lsl = LazySupplierList<Any?>({ null }, Int.MAX_VALUE)

        Assertions.assertFalse(lsl.isFullyEvaluated)
        Assertions.assertNull(lsl.getOrNull(0))
        Assertions.assertTrue(lsl.isFullyEvaluated)
        Assertions.assertEquals(0, lsl.size)
        Assertions.assertTrue(lsl.isFullyEvaluated)

        Assertions.assertThrows<IndexOutOfBoundsException?>(
            IndexOutOfBoundsException::class.java,
            Executable { lsl.get(1) })
    }

    @Test
    fun streamOnInfinite() {
        // The list is not infinite, but it would still be too big to fit into an array, so it's practically infinite.

        val lsl = LazySupplierList<Int?>({ i: Int -> i }, Int.MAX_VALUE)

        Assertions.assertEquals(mutableListOf<Int?>(0, 1, 2, 3, 4), lsl.stream().limit(5).toList())
    }
}
