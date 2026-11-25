package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.error.IllegalGrammarException
import de.kleinert.parsewisp.error.ParserCreationFailure
import de.kleinert.parsewisp.grammar.GrammarPrinter
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.io.File
import java.io.IOException
import java.nio.file.Files

internal class ParsewispParserCreationTest {
    @Test
    fun parserFromString() {
        val p = parser("S = '1'")
        val p2 = parser("S = '1'")
        Assertions.assertEquals(p.grammar, p2.grammar)
    }

    @Test
    fun parserFromFile() {
        try {
            val text = "aaaaabbbaaaabb"
            val grammarFile = File("testres/grammars/as_and_bs.grammar")
            val p = parser(Files.readString(grammarFile.toPath()))
            val grammarText = Files.readString(grammarFile.toPath())

            Assertions.assertEquals(
                parser(grammarText).parse(text),
                p.parse(text)
            )
            Assertions.assertEquals(
                parser(grammarText).grammar,
                p.grammar
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        try {
            val grammarFile = File("testres/grammars/as_and_bs.grammar")
            val p = parser(Files.readString(grammarFile.toPath()))
            Assertions.assertEquals(
                p.grammar,
                parser(Files.readString(grammarFile.toPath())).grammar
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        try {
            val grammarFile = File("testres/grammars/c99.grammar")
            val pFromString = parser(
                Files.readString(grammarFile.toPath()),
                ParserCreationOptions.newWithStandardWhitespace()
            )
            val pFromFile = parser(
                Files.readString(grammarFile.toPath()),
                ParserCreationOptions.newWithStandardWhitespace()
            )
            val text = "void a(){}"
            Assertions.assertEquals(pFromString.parses(text), pFromFile.parses(text))
            Assertions.assertEquals(pFromString.grammar, pFromFile.grammar)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun parserFromStringWithOptions() {
    }

    @Test
    fun parserFromFileWithOptions() {
    }

    @Test
    fun parserFromGrammarWithOptions() {
    }

    @Test
    fun failBecauseOfUndefinedNT() {
        // Error: Illegal grammar
        val grammar = "S := A"
        Assertions.assertThrows<IllegalGrammarException?>(
            IllegalGrammarException::class.java,
            Executable { parser(grammar) })
    }

    @Test
    fun failBecauseNoTerminals() {
        // Error: Illegal grammar
        val grammar = "S := S"
        Assertions.assertThrows<IllegalGrammarException?>(
            IllegalGrammarException::class.java,
            Executable { parser(grammar) })
    }

    @Test
    fun parserCreationFail() {
        // Error: Starting symbol not in grammar
        val grammar = "S = 'abc'"
        val options = ParserCreationOptions.create(
            null, sym("C"),
            null,
            true,
            GrammarPrinter.getDefault()
        )
        Assertions.assertThrows<ParserCreationFailure?>(
            ParserCreationFailure::class.java,
            Executable { parser(grammar, options) })
    }
}