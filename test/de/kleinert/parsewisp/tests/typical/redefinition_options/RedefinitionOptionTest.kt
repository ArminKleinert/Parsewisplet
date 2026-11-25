package de.kleinert.parsewisp.tests.typical.redefinition_options

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parser_options.RedefinitionOption
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

internal class RedefinitionOptionTest {
    @Test
    fun optionOverrideTest() {
        val redefinitionOpts = ParserCreationOptions
            .getDefault()
            .withRedefinitionOption(RedefinitionOption.OVERRIDE)
        val p = parser("S = 'A'\nS = 'B'\nS = 'C'", redefinitionOpts)
        Assertions.assertTrue(p.parse("A").isFailure())
        Assertions.assertTrue(p.parse("B").isFailure())
        Assertions.assertEquals(PT.create("S", "C"), p.parse("C"))
    }

    @Test
    fun optionErrorTest() {
        val redefinitionOpts = ParserCreationOptions
            .getDefault()
            .withRedefinitionOption(RedefinitionOption.ERROR)

        // Only one production -> No duplicates, no problem
        Assertions.assertEquals(
            PT.create("S", "A"),
            parser("S = 'A'", redefinitionOpts).parse("A")
        )

        // Duplicate name, different lhs -> Problem
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            Executable { parser("S = 'A'\nS = 'B'\nS = 'C'", redefinitionOpts) })

        // Fails even if the production does not change
        Assertions.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            Executable { parser("S = 'A'\nS = 'A'\nS = 'A'", redefinitionOpts) })
    }

    @Test
    fun optionChoiceTest() {
        val redefinitionOpts = ParserCreationOptions
            .getDefault()
            .withRedefinitionOption(RedefinitionOption.CHOICE)
        val p = parser("S = 'A'\nS = 'B'\nS = 'C'", redefinitionOpts)
        Assertions.assertEquals(PT.create("S", "A"), p.parse("A"))
        Assertions.assertEquals(PT.create("S", "B"), p.parse("B"))
        Assertions.assertEquals(PT.create("S", "C"), p.parse("C"))
    }

    @Test
    fun optionKeepTest() {
        val redefinitionOpts = ParserCreationOptions
            .getDefault()
            .withRedefinitionOption(RedefinitionOption.KEEP)
        val p = parser("S = 'A'\nS = 'B'\nS = 'C'", redefinitionOpts)
        Assertions.assertEquals(PT.create("S", "A"), p.parse("A"))
        Assertions.assertTrue(p.parse("B").isFailure())
        Assertions.assertTrue(p.parse("C").isFailure())
    }
}