package de.kleinert.parsewisp.tests.typical.rules_available

import de.kleinert.parsewisp.Parsewisp.parser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class RuleAlternativesTests {
    @Test
    fun oneOrMoreRepetitionReplacements() {
        val p1 = parser("S = \"a\"+")
        val p2 = parser("S = \"a\" {\"a\"}")
        val p3 = parser(
            """
                S   = "a" | "a" A
                <A> = "a" A | ε
                
                """.trimIndent()
        )

        Assertions.assertTrue(p1.parse("").isFailure())
        Assertions.assertTrue(p2.parse("").isFailure())
        Assertions.assertTrue(p3.parse("").isFailure())

        Assertions.assertEquals(p1.parse("aa"), p2.parse("aa"))
        Assertions.assertEquals(p1.parse("aa"), p3.parse("aa"))
    }

    @Test
    fun optionalRepetitionReplacements() {
        val p1 = parser("S = {\"a\"}")
        val p2 = parser(
            """
                S   = ε | A
                <A> = "a" A | ε
                
                """.trimIndent()
        )
        Assertions.assertEquals(p1.parse(""), p2.parse(""))
        Assertions.assertEquals(p1.parse("aa"), p2.parse("aa"))
    }

    @Test
    fun valueRangeReplacements() {
        val p2 = parser("S = #\"[\\x{41}-\\x{5a}]\"")
        val p3 =
            parser("S = \"A\" | \"B\" | \"C\" | \"D\" | \"E\" | \"F\" | \"G\" | \"H\" | \"I\" | \"J\" | \"K\" | \"L\" | \"M\" | \"N\" | \"O\" | \"P\" | \"Q\" | \"R\" | \"S\" | \"T\" | \"U\" | \"V\" | \"W\" | \"X\" | \"Y\" | \"Z\"")
        Assertions.assertEquals(p2.parse("D"), p3.parse("D"))
    }

    @Test
    fun optionReplacements() {
        val p1 = parser("S = \"a\"?")
        val p2 = parser("S = ε | \"a\"")
        Assertions.assertEquals(p1.parse("a"), p2.parse("a"))
    }


    @Test
    fun variableRepeatMin() {
        val p1 = parser("S = \"a\"{4,}")
        val p2 = parser("S = \"a\" \"a\" \"a\" \"a\" {\"a\"}")
        Assertions.assertEquals(p1.parse("aaaa"), p2.parse("aaaa"))
        Assertions.assertEquals(p1.parse("aaaaaa"), p2.parse("aaaaaa"))
    }

    @Test
    fun variableRepeatMax() {
        val p1 = parser("S = \"a\"{,4}")
        val p2 = parser("S = \"a\"? \"a\"? \"a\"? \"a\"?")
        Assertions.assertEquals(p1.parse(""), p2.parse(""))
        Assertions.assertEquals(p1.parse("aa"), p2.parse("aa"))
        Assertions.assertEquals(p1.parse("aaaa"), p2.parse("aaaa"))
    }

    @Test
    fun variableRepeatMinMax() {
        val p1 = parser("S = \"a\"{2,4}")
        val p2 = parser("S = \"a\" \"a\" \"a\"? \"a\"?")
        Assertions.assertEquals(p1.parse("aa"), p2.parse("aa"))
        Assertions.assertEquals(p1.parse("aaaa"), p2.parse("aaaa"))
    }

    @Test
    fun variableRepeatFree() {
        val p1 = parser("S = \"a\"*")
        val p2 = parser("S = {\"a\"}")
        Assertions.assertEquals(p1.parse(""), p2.parse(""))
        Assertions.assertEquals(p1.parse("aaaa"), p2.parse("aaaa"))
    }
}
