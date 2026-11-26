package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class GrammarPrinterTest {
    @Test
    fun parens() {
        val p = Parsewisp.parser("S = \"a\" \"b\" | \"c\" \"d\"")
        Assertions.assertEquals(
            p.grammar, Parsewisp.parser(p.show()).grammar
        )

        val p2 = Parsewisp.parser("S = (\"a\" | \"b\") (\"c\" | \"d\")")
        Assertions.assertEquals(
            p2.grammar, Parsewisp.parser(p2.show()).grammar
        )
    }

    @Test
    fun altToString() {
        val p = Parsewisp.parser("S = \"a\" | \"b\"")
        val text = p.show()
        val p2 = Parsewisp.parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun ordToString() {
        val p = Parsewisp.parser("S = \"a\" / \"b\"")
        val text = p.show()
        val p2 = Parsewisp.parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun catToString() {
        val p = Parsewisp.parser("S = \"a\" \"b\"")
        val text = p.show()
        val p2 = Parsewisp.parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun epsToString() {
        // complicated structure because a simple `S = eps epsilon ε ...` might be optimized to `S = ε`, but we want to test all forms.
        val p = Parsewisp.parser(
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
        val p2 = Parsewisp.parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun lookToString() {
        val p = Parsewisp.parser("S = &\"a\" \"a\"*")
        val text = p.show()
        val p2 = Parsewisp.parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun negToString() {
        val p = Parsewisp.parser("S = !\"a\" \"b\"")
        val text = p.show()
        val p2 = Parsewisp.parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun ntToString() {
        val p = Parsewisp.parser("S = A\nA = \"a\" S | \"\"")
        val text = p.show()
        val p2 = Parsewisp.parser(text)
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun optToString() {
        run {
            val p = Parsewisp.parser("S = [ \"a\" ]")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = Parsewisp.parser("S = \"a\"?")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
    }

    @Test
    fun regexpToString() {
        run {
            val p = Parsewisp.parser("S = #\"a\"")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = Parsewisp.parser("S = #'a'")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
    }

    @Test
    fun literalToString() {
        run {
            val p = Parsewisp.parser("S = \"a\"")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = Parsewisp.parser("S = 'a'")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = Parsewisp.parser("S = %s\"a\"")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = Parsewisp.parser("S = %s'a'")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = Parsewisp.parser("S = %i\"a\"")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = Parsewisp.parser("S = %i'a'")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
    }

    @Test
    fun zeroOrMoreToString() {
        run {
            val p = Parsewisp.parser("S = { \"a\" }")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
        run {
            val p = Parsewisp.parser("S = \"a\"*")
            val text = p.show()
            val p2 = Parsewisp.parser(text)
            Assertions.assertEquals(p.grammar, p2.grammar)
        }
    }
}