package de.kleinert.parsewisp.tests.typical.grammars

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parser_options.ParsingOptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

internal class TestGrammarYail {
    private fun parser(): Parser {
        try {
            val opts = ParserCreationOptions.getDefault()
            return parser(
                Files.readString(Path.of("testres/grammars/yail.grammar")),
                opts
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun verifySimpleProgram() {
        val text = "print 1 ;"
        Assertions.assertTrue(parser().parse(text).isSuccess())
    }

    @Test
    fun verifyVar() {
        val text = "var v = 0;"
        Assertions.assertTrue(parser().parse(text).isSuccess())
    }

    @Test
    fun verifyAssign() {
        val text = "v = 0;"
        Assertions.assertTrue(parser().parse(text).isSuccess())
    }

    @Test
    fun verifyFunctionDef() {
        val text = "//: (int32_t, int32_t) -> int64_t\nfun f(a, b) { return host(\"((int64_t)a) + b\"); }"
        //System.out.println(rule().parse(text));
        Assertions.assertTrue(parser().parse(text, ParsingOptions.getDefault()).isSuccess())
    }

    @Test
    fun verifySimpleStuff() {
        val text = """
                var v = 4;
                fun f(v) { if (v > 0) return -v; else return v; }
                print f(v) <= 0;
                """.trimIndent()
        Assertions.assertTrue(parser().parse(text).isSuccess())
    }
}
