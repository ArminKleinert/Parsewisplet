package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.collections.FlatResultSeq
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class FlatResultSeqTest {
    @Test
    fun emptyIsEmpty() {
        Assertions.assertTrue(FlatResultSeq.make().isEmpty)
    }

    @Test
    fun emptyLength() {
        Assertions.assertEquals(0, FlatResultSeq.make().size())
    }

    @Test
    fun appendSingle() {
        val seq = FlatResultSeq.make().appendOrConcat("a")
        Assertions.assertEquals(1, seq.size())
        val seq2 = seq.appendOrConcat("b")
        Assertions.assertEquals(1, seq.size())
        Assertions.assertEquals(2, seq2.size())
        val seq3 = seq2.appendOrConcat("c")
        Assertions.assertEquals(1, seq.size())
        Assertions.assertEquals(2, seq2.size())
        Assertions.assertEquals(3, seq3.size())
    }

    @Test
    fun appendMulti() {
        val seq = FlatResultSeq.make().appendOrConcat("a").appendOrConcat("b")
        val seq2 = FlatResultSeq.make().appendOrConcat("c").appendOrConcat("d")
        val seqSeq = seq.appendOrConcat(seq2)
        Assertions.assertEquals(2, seq.size())
        Assertions.assertEquals(4, seqSeq.size())
        val seq3 = FlatResultSeq.make().appendOrConcat("e").appendOrConcat("f")
        val seq4 = FlatResultSeq.make().appendOrConcat("g")
        val seqSeqSeqG = seq.appendOrConcat(seq2).appendOrConcat(seq3).appendOrConcat(seq4)
        Assertions.assertEquals(7, seqSeqSeqG.size())
    }
}
