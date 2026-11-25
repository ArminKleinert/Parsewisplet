package de.kleinert.parsewisp.tests.typical.grammars

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser.Parser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Test(s) for the bf (BrainFuck) grammar.
 *
 *
 * Grammar and tests from [esolangs.org/wiki/Brainfuck](https://esolangs.org/wiki/Brainfuck#Examples).
 */
internal class TestGrammarBF {
    private fun parser(): Parser {
        try {
            return parser(
                Files.readString(Path.of("testres/grammars/bf.grammar"))
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun helloWorld() {
        val text =
            "++++++++[>++++[>++>+++>+++>+<<<<-]>+>+>->>+[<]<-]>>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++."
        Assertions.assertTrue(parser().parse(text).isSuccess)
    }

    @Test
    fun moveValue() {
        val text = """
                Code:   Pseudo code:
                >>      Move the pointer to cell2
                [-]     Set cell2 to 0 
                <<      Move the pointer back to cell0
                [       While cell0 is not 0
                  -       Subtract 1 from cell0
                  >>      Move the pointer to cell2
                  +       Add 1 to cell2
                  <<      Move the pointer back to cell0
                ]       End while
                """.trimIndent()
        Assertions.assertTrue(parser().parse(text).isSuccess)
    }

    @Test
    fun cat() {
        val text = ",[.,]"
        Assertions.assertTrue(parser().parse(text).isSuccess)
    }

    @Test
    fun quine() {
        val text = """
                -->+++>+>+>+>+++++>++>++>->+++>++>+>>>>>>>>>>>>>>>>->++++>>>>->+++>+++>+++>+++>+
                ++>+++>+>+>>>->->>++++>+>>>>->>++++>+>+>>->->++>++>++>++++>+>++>->++>++++>+>+>++
                >++>->->++>++>++++>+>+>>>>>->>->>++++>++>++>++++>>>>>->>>>>+++>->++++>->->->+++>
                >>+>+>+++>+>++++>>+++>->>>>>->>>++++>++>++>+>+++>->++++>>->->+++>+>+++>+>++++>>>
                +++>->++++>>->->++>++++>++>++++>>++[-[->>+[>]++[<]<]>>+[>]<--[++>++++>]+[<]<<++]
                >>>[>]++++>++++[--[+>+>++++<<[-->>--<<[->-<[--->>+<<[+>+++<[+>>++<<]]]]]]>+++[>+
                ++++++++++++++<-]>--.<<<]
                """.trimIndent()
        Assertions.assertTrue(parser().parse(text).isSuccess)
    }

    @Test
    fun xkcdRandom() {
        val text = "+c+h+o+s[e-n> +b+y+ <f]a>i+r[ -d>i+c+e+ +r<o]l-l+>;\n" +
                "Guaranteed to be random."
        Assertions.assertTrue(parser().parse(text).isSuccess)
    }
}