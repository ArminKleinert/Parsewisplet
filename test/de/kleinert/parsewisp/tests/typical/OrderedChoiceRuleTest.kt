package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.List

internal class OrderedChoiceRuleTest {
    @Test
    fun orderWhenEverythingIsEps() {
        val p = parser(
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
            List.of<ParseTree?>(
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
