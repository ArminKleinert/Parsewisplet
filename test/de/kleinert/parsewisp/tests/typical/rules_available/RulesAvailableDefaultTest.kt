package de.kleinert.parsewisp.tests.typical.rules_available

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.error.IllegalGrammarException
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class RulesAvailableDefaultTest {
    @Test
    fun abnfCoreNotAvailableTest() {
        Assertions.assertThrows<IllegalGrammarException?>(IllegalGrammarException::class.java, {
            parser(
                "S = ALPHA BIT CHAR CR CRLF CTL DIGIT DQUOTE HEXDIG HTAB LF LWSP OCTET SP VCHAR WSP"
            )
        })
    }

    @Test
    fun alternationSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = \"a\" | \"b\"") })

        Assertions.assertEquals(
            setOf(
                PT.create("S", PT.create("A", "a")),
                PT.create("S", PT.create("B", "a"))
            ),
            HashSet<ParseTree?>(parser("S = A | B\nA = \"a\"\nB = \"a\"").parses("a"))
        )
    }

    @Test
    fun exclusionSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = #'[0-9]+' - '1'") })
        Assertions.assertDoesNotThrow({ parser("S = #'[0-9]+' - ('1' | '11' | '111')") })
        Assertions.assertDoesNotThrow({ parser("S = #'[0-9]+' - (#'[1]+' - '11')") })

        var p = parser("S = #'[0-9]+' - '1'")
        Assertions.assertTrue(p.parse("1").isFailure)
        Assertions.assertEquals(PT.create("S", "11"), p.parse("11"))
        Assertions.assertEquals(PT.create("S", "1111"), p.parse("1111"))
        Assertions.assertEquals(PT.create("S", "2"), p.parse("2"))

        p = parser("S = #'[0-9]+' - ('1' | '11' | '111')")
        Assertions.assertTrue(p.parse("1").isFailure)
        Assertions.assertTrue(p.parse("11").isFailure)
        Assertions.assertEquals(PT.create("S", "1111"), p.parse("1111"))
        Assertions.assertEquals(PT.create("S", "2"), p.parse("2"))

        p = parser("S = #'[0-9]+' - (#'[1]+' - '11')")
        Assertions.assertTrue(p.parse("1").isFailure)
        Assertions.assertEquals(PT.create("S", "11"), p.parse("11"))
        Assertions.assertTrue(p.parse("1111").isFailure)
        Assertions.assertEquals(PT.create("S", "2"), p.parse("2"))
    }

    @Test
    fun explicitStringCaseSensitivitySimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = %i\"a\"") })
        Assertions.assertDoesNotThrow({ parser("S = %s\"a\"") })

        val parserI = parser("S = %i\"a\"")
        Assertions.assertEquals(PT.create("S", "a"), parserI.parse("a"))
        Assertions.assertEquals(PT.create("S", "a"), parserI.parse("A"))

        val parserS = parser("S = %s\"a\"")
        Assertions.assertEquals(PT.create("S", "a"), parserS.parse("a"))
        Assertions.assertTrue(parserS.parse("A").isFailure)
    }

    @Test
    fun extendedIdentifiersSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = \"a\"") })
        Assertions.assertDoesNotThrow({ parser("\uD83C\uDF81 = \"a\"") })
    }

    @Test
    fun lookaheadSimpleTestSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = &\"a\" \"a\"") })

        Assertions.assertEquals(
            PT.create("S", "a"),
            parser("S = &\"a\" \"a\"").parse("a")
        )
        Assertions.assertTrue(
            parser("S = &\"b\" \"a\"").parse("a").isFailure
        )
    }

    @Test
    fun negativeLookaheadSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = !\"b\" \"a\"") })

        Assertions.assertEquals(
            PT.create("S", "a"),
            parser("S = !\"b\" \"a\"").parse("a")
        )
        Assertions.assertTrue(
            parser("S = !\"a\" \"a\"").parse("a").isFailure
        )
    }

    @Test
    fun optionalSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = [\"a\"]") })

        val p = parser("S = [\"a\"]")
        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
    }

    @Test
    fun optionalQuerySimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = \"a\"?") })

        val p = parser("S = \"a\"?")
        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
    }

    @Test
    fun optionalRepetitionSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = {\"a\"}") })

        val p = parser("S = {\"a\"}")
        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
    }

    @Test
    fun optionalRepetitionStarSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = \"a\"*") })

        val p = parser("S = \"a\"*")
        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
    }

    @Test
    fun orderedChoiceSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = \"a\" / \"b\"") })

        Assertions.assertEquals(
            setOf(
                PT.create("S", PT.create("A", "a")),
                PT.create("S", PT.create("B", "a"))
            ),
            HashSet<ParseTree?>(parser("S = A / B\nA = \"a\"\nB = \"a\"").parses("a"))
        )
    }

    @Test
    fun plusSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = \"a\"+") })

        val p = parser("S = \"a\"+")
        Assertions.assertTrue(p.parse("").isFailure)
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
    }

    @Test
    fun regexSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = #\"a\"") })
        Assertions.assertDoesNotThrow({ parser("S = #'a'") })

        val p = parser("S = #\"[0-9]\"")
        Assertions.assertTrue(p.parse("").isFailure)
        Assertions.assertEquals(PT.create("S", "0"), p.parse("0"))
        Assertions.assertEquals(PT.create("S", "5"), p.parse("5"))
    }

    @Test
    fun singleQuotesForStringTerminalsSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = 'abc'") })

        Assertions.assertEquals(
            parser("S = \"abc\""),
            parser("S = 'abc'")
        )
    }

    @Test
    fun valueRangeSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = %x41-5a") })
        Assertions.assertDoesNotThrow({ parser("S = %d65-90") })
        Assertions.assertDoesNotThrow({ parser("S = %b1000001-1011010") })

        val pHex = parser("S = %x41-5a")
        val pDec = parser("S = %d65-90")
        val pBin = parser("S = %b1000001-1011010")
        Assertions.assertEquals(pHex, pDec)
        Assertions.assertEquals(pHex, pBin)
    }

    @Test
    fun variableRepetitionSimpleTest() {
        Assertions.assertDoesNotThrow({ parser("S = \"a\"{1,5}") })
        Assertions.assertDoesNotThrow({ parser("S = \"a\"{1,}") })
        Assertions.assertDoesNotThrow({ parser("S = \"a\"{,5}") })
        Assertions.assertDoesNotThrow({ parser("S = \"a\"{}") })

        val p = parser("S = \"a\"{1,3} \"b\"")
        Assertions.assertTrue(p.parse("a").isFailure)
        Assertions.assertTrue(p.parse("b").isFailure)
        Assertions.assertEquals(PT.create("S", "a", "b"), p.parse("ab"))
        Assertions.assertEquals(PT.create("S", "a", "a", "b"), p.parse("aab"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "b"), p.parse("aaab"))
        Assertions.assertTrue(p.parse("aaaab").isFailure)
    }
}
