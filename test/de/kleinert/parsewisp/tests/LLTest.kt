package de.kleinert.parsewisp.tests

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.functions.Procedure
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.testutil.TimeUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/*
Array-based:
.pDirect____ : 734.5994077222223
.pLeftRecH__ : 5513.966410888888
.pLeftRec___ : 89.11456878888889
.pRightRecEH : 5935.430014133333
.pRightRecE_ : 168.37849315555556
.pRightRecH_ : 5533.560816744444
.pRightRec__ : 160.0001591333333
.pRegex_____ : 0.1617999

Array+Marker:
.pDirect____ : 24.482446644444444
.pLeftRecH__ : 5373.383967522222
.pLeftRec___ : 98.55333840000002
.pRightRecEH : 5385.678086377778
.pRightRecE_ : 172.39012652222223
.pRightRecH_ : 5309.490438144445
.pRightRec__ : 157.92169719999998
.pRegex_____ : 0.1264512111111111

Linked:
.pDirect____ : 22.615405199999998
.pLeftRecH__ : 5353.148949677778
.pLeftRec___ : 89.34735326666667
.pRightRecEH : 5850.970126155556
.pRightRecE_ : 174.1519648
.pRightRecH_ : 5599.451224055556
.pRightRec__ : 156.4384317777778
.pRegex_____ : 0.1361259
 */
internal class LLTest {
    val reps: Int = 300
    val runsPerMeasure: Int = 100
    val s: String = "b" + "a".repeat(reps)
    val sReduced: String = "b" + "a".repeat(reps / 10)

    val pRegex: Parser = parser("S = #'ba+'")
    val pDirect: Parser = parser("S = 'b' 'a'+")
    val pLeftRec: Parser = parser("S = S1\nS1 = S1 'a' | 'b'")
    val pLeftRecH: Parser = parser("S = S1\n<S1> = S1 'a' | 'b'")
    val pRightRec: Parser = parser("S = 'b' A\nA = 'a' A | 'a'")
    val pRightRecH: Parser = parser("S = 'b' A\n<A> = 'a' A | 'a'")
    val pRightRecE: Parser = parser("S = 'b' A\nA = 'a' A | epsilon")
    val pRightRecEH: Parser = parser("S = 'b' A\n<A> = 'a' A | epsilon")

    @Test
    fun pEqualityReduced() {
        val tree = pDirect.parse(sReduced)
        Assertions.assertEquals(tree, pLeftRecH.parse(sReduced))
        Assertions.assertEquals(tree, pRightRecH.parse(sReduced))
        Assertions.assertEquals(tree, pRightRecEH.parse(sReduced))
    }

    @Test
    fun pEquality() {
        val tree = pDirect.parse(s)
        Assertions.assertEquals(tree, pLeftRecH.parse(s))
        Assertions.assertEquals(tree, pRightRecH.parse(s))
        Assertions.assertEquals(tree, pRightRecEH.parse(s))
    }

    @Test
    fun pRegex_____() {
        printStat({ pRegex.parse(s) })
    }

    @Test
    fun pDirect____() {
        printStat({ pDirect.parse(s) })
    }

    @Test
    fun pLeftRec___() {
        printStat({ pLeftRec.parse(s) })
    }

    @Test
    fun pLeftRecH__() {
        printStat({ pLeftRecH.parse(s) })
    }

    @Test
    fun pRightRec__() {
        printStat({ pRightRec.parse(s) })
    }

    @Test
    fun pRightRecH_() {
        printStat({ pRightRecH.parse(s) })
    }

    @Test
    fun pRightRecE_() {
        printStat({ pRightRecE.parse(s) })
    }

    @Test
    fun pRightRecEH() {
        printStat({ pRightRecEH.parse(s) })
    }

    private fun printStat(prod: Procedure) {
        val ste = Thread.currentThread().stackTrace
        println("." + ste[2].methodName + " : " + TimeUtil.measureTimeMillisMean(runsPerMeasure, prod)        )
    }
}
