package de.kleinert.parsewisp.tests.typical.rules_available

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.error.IllegalGrammarException
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class RulesAvailableDefaultTest {
    @Test
    fun abnfCoreNotAvailableTest() {
        Assertions.assertThrows<IllegalGrammarException?>(IllegalGrammarException::class.java, {
            Parsewisp.parser(
                "S = ALPHA BIT CHAR CR CRLF CTL DIGIT DQUOTE HEXDIG HTAB LF LWSP OCTET SP VCHAR WSP"
            )
        })
    }

    @Test
    fun alternationSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = \"a\" | \"b\"") })

        Assertions.assertEquals(
            setOf(
                PT.create("S", PT.create("A", "a")),
                PT.create("S", PT.create("B", "a"))
            ),
            HashSet<ParseTree?>(Parsewisp.parser("S = A | B\nA = \"a\"\nB = \"a\"").parses("a"))
        )
    }

    @Test
    fun exclusionSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = #'[0-9]+' - '1'") })
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = #'[0-9]+' - ('1' | '11' | '111')") })
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = #'[0-9]+' - (#'[1]+' - '11')") })

        var p = Parsewisp.parser("S = #'[0-9]+' - '1'")
        Assertions.assertTrue(p.parse("1").isFailure)
        Assertions.assertEquals(PT.create("S", "11"), p.parse("11"))
        Assertions.assertEquals(PT.create("S", "1111"), p.parse("1111"))
        Assertions.assertEquals(PT.create("S", "2"), p.parse("2"))

        p = Parsewisp.parser("S = #'[0-9]+' - ('1' | '11' | '111')")
        Assertions.assertTrue(p.parse("1").isFailure)
        Assertions.assertTrue(p.parse("11").isFailure)
        Assertions.assertEquals(PT.create("S", "1111"), p.parse("1111"))
        Assertions.assertEquals(PT.create("S", "2"), p.parse("2"))

        p = Parsewisp.parser("S = #'[0-9]+' - (#'[1]+' - '11')")
        Assertions.assertTrue(p.parse("1").isFailure)
        Assertions.assertEquals(PT.create("S", "11"), p.parse("11"))
        Assertions.assertTrue(p.parse("1111").isFailure)
        Assertions.assertEquals(PT.create("S", "2"), p.parse("2"))
    }

    @Test
    fun explicitStringCaseSensitivitySimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = %i\"a\"") })
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = %s\"a\"") })

        val parserI = Parsewisp.parser("S = %i\"a\"")
        Assertions.assertEquals(PT.create("S", "a"), parserI.parse("a"))
        Assertions.assertEquals(PT.create("S", "a"), parserI.parse("A"))

        val parserS = Parsewisp.parser("S = %s\"a\"")
        Assertions.assertEquals(PT.create("S", "a"), parserS.parse("a"))
        Assertions.assertTrue(parserS.parse("A").isFailure)
    }

    @Test
    fun extendedIdentifiersSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = \"a\"") })
        Assertions.assertDoesNotThrow({ Parsewisp.parser("\uD83C\uDF81 = \"a\"") })
    }

    @Test
    fun lookaheadSimpleTestSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = &\"a\" \"a\"") })

        Assertions.assertEquals(
            PT.create("S", "a"),
            Parsewisp.parser("S = &\"a\" \"a\"").parse("a")
        )
        Assertions.assertTrue(
            Parsewisp.parser("S = &\"b\" \"a\"").parse("a").isFailure
        )
    }

    @Test
    fun negativeLookaheadSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = !\"b\" \"a\"") })

        Assertions.assertEquals(
            PT.create("S", "a"),
            Parsewisp.parser("S = !\"b\" \"a\"").parse("a")
        )
        Assertions.assertTrue(
            Parsewisp.parser("S = !\"a\" \"a\"").parse("a").isFailure
        )
    }

    @Test
    fun optionalSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = [\"a\"]") })

        val p = Parsewisp.parser("S = [\"a\"]")
        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
    }

    @Test
    fun optionalQuerySimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = \"a\"?") })

        val p = Parsewisp.parser("S = \"a\"?")
        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
    }

    @Test
    fun optionalRepetitionSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = {\"a\"}") })

        val p = Parsewisp.parser("S = {\"a\"}")
        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
    }

    @Test
    fun optionalRepetitionStarSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = \"a\"*") })

        val p = Parsewisp.parser("S = \"a\"*")
        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
    }

    @Test
    fun orderedChoiceSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = \"a\" / \"b\"") })

        Assertions.assertEquals(
            setOf(
                PT.create("S", PT.create("A", "a")),
                PT.create("S", PT.create("B", "a"))
            ),
            HashSet<ParseTree?>(Parsewisp.parser("S = A / B\nA = \"a\"\nB = \"a\"").parses("a"))
        )
    }

    @Test
    fun plusSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = \"a\"+") })

        val p = Parsewisp.parser("S = \"a\"+")
        Assertions.assertTrue(p.parse("").isFailure)
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
    }

    @Test
    fun regexSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = #\"a\"") })
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = #'a'") })

        val p = Parsewisp.parser("S = #\"[0-9]\"")
        Assertions.assertTrue(p.parse("").isFailure)
        Assertions.assertEquals(PT.create("S", "0"), p.parse("0"))
        Assertions.assertEquals(PT.create("S", "5"), p.parse("5"))
    }

    @Test
    fun singleQuotesForStringTerminalsSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = 'abc'") })

        Assertions.assertEquals(
            Parsewisp.parser("S = \"abc\""),
            Parsewisp.parser("S = 'abc'")
        )
    }

    @Test
    fun valueRangeSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = %x41-5a") })
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = %d65-90") })
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = %b1000001-1011010") })

        val pHex = Parsewisp.parser("S = %x41-5a")
        val pDec = Parsewisp.parser("S = %d65-90")
        val pBin = Parsewisp.parser("S = %b1000001-1011010")
        Assertions.assertEquals(pHex, pDec)
        Assertions.assertEquals(pHex, pBin)
    }

    @Test
    fun variableRepetitionSimpleTest() {
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = \"a\"{1,5}") })
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = \"a\"{1,}") })
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = \"a\"{,5}") })
        Assertions.assertDoesNotThrow({ Parsewisp.parser("S = \"a\"{}") })

        val p = Parsewisp.parser("S = \"a\"{1,3} \"b\"")
        Assertions.assertTrue(p.parse("a").isFailure)
        Assertions.assertTrue(p.parse("b").isFailure)
        Assertions.assertEquals(PT.create("S", "a", "b"), p.parse("ab"))
        Assertions.assertEquals(PT.create("S", "a", "a", "b"), p.parse("aab"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "b"), p.parse("aaab"))
        Assertions.assertTrue(p.parse("aaaab").isFailure)
    }
}
