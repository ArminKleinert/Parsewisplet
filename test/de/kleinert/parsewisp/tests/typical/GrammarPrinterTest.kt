package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class GrammarPrinterTest {
    @Test
    fun parens() {
        val p = parser("S = \"a\" \"b\" | \"c\" \"d\"")
        Assertions.assertEquals(
            p.grammar, parser(p.show()).grammar
        )

        val p2 = parser("S = (\"a\" | \"b\") (\"c\" | \"d\")")
        Assertions.assertEquals(
            p2.grammar, parser(p2.show()).grammar
        )
    }

    @Test
    fun altToString() {
        val p = parser("S = \"a\" | \"b\"")
        val text = p.show()
        val p2 = parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun ordToString() {
        val p = parser("S = \"a\" / \"b\"")
        val text = p.show()
        val p2 = parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun catToString() {
        val p = parser("S = \"a\" \"b\"")
        val text = p.show()
        val p2 = parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun epsToString() {
        // complicated structure because a simple `S = eps epsilon ε ...` might be optimized to `S = ε`, but we want to test all forms.
        val p = parser(
            """
                S = E1 E2 E3 E4 E5 E6
                E1 = eps
                E2 = epsilon
                E3 = Epsilon
                E4 = EPSILON
                E5 = ε
                E6 = ""
                
                """.trimIndent()
        )
        val text = p.show()
        val p2 = parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun lookToString() {
        val p = parser("S = &\"a\" \"a\"*")
        val text = p.show()
        val p2 = parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun negToString() {
        val p = parser("S = !\"a\" \"b\"")
        val text = p.show()
        val p2 = parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun ntToString() {
        val p = parser("S = A\nA = \"a\" S | \"\"")
        val text = p.show()
        val p2 = parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun optToString() {
        run {
            val p = parser("S = [ \"a\" ]")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = parser("S = \"a\"?")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
    }

    @Test
    fun regexpToString() {
        run {
            val p = parser("S = #\"a\"")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = parser("S = #'a'")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
    }

    @Test
    fun literalToString() {
        run {
            val p = parser("S = \"a\"")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = parser("S = 'a'")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = parser("S = %s\"a\"")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = parser("S = %s'a'")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = parser("S = %i\"a\"")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = parser("S = %i'a'")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
    }

    @Test
    fun zeroOrMoreToString() {
        run {
            val p = parser("S = { \"a\" }")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = parser("S = \"a\"*")
            val text = p.show()
            val p2 = parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
    }
}