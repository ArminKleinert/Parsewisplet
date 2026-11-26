package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class OrderedChoiceRuleTest {
    @Test
    fun orderWhenEverythingIsEps() {
        val p = Parsewisp.parser(
            """
                S = A / B / C / D / E
                A = ε
                B = ε
                C = ε
                D = ε
                E = ε
                
                """.trimIndent()
        )

        Assertions.assertEquals(
            PT.create("S", PT.create("A")),
            p.parse("")
        )

        Assertions.assertEquals(
            listOf(
                PT.create("S", PT.create("A")),
                PT.create("S", PT.create("B")),
                PT.create("S", PT.create("C")),
                PT.create("S", PT.create("D")),
                PT.create("S", PT.create("E"))
            ),
            p.parses("")
        )
    }
}
