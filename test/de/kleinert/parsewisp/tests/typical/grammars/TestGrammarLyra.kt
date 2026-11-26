package de.kleinert.parsewisp.tests.typical.grammars

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Test(s) for the Lyra grammar.
 *
 *
 * Grammar and tests from [esolangs.org/wiki/Brainfuck](https://esolangs.org/wiki/Brainfuck#Examples).
 */
internal class TestGrammarLyra {
    private fun parser(): Parser {
        try {
            return Parsewisp.parser(
                Files.readString(Path.of("testres/grammars/lyra.grammar")),
                ParserCreationOptions.newWithStandardWhitespace()
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun helloWorld() {
        val text = "(println! \"Hello World!\")"
        //System.out.println(rule().parse(text));
    }
}