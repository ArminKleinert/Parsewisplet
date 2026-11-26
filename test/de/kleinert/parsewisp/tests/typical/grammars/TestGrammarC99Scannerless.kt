package de.kleinert.parsewisp.tests.typical.grammars

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Test(s) for the C99 grammar.
 */
internal class TestGrammarC99Scannerless {
    private fun parser(): Parser {
        try {
            return parser(
                Files.readString(Path.of("testres/grammars/c99_sl.grammar")),
                ParserCreationOptions.newWithStandardWhitespace()
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun verifySimpleProgram() {
        val text = "int i; int main (int argc, char** argv) { i = 11; while (--i) { printf(\"%d\", i); } }"
        Assertions.assertTrue(parser().parse(text).isSuccess)
    }

    @Test
    @Throws(IOException::class)
    fun verifySimpleProgram1() {
        val text = "int i;"
        //System.out.println(rule().parses(text).size());
    }
}

