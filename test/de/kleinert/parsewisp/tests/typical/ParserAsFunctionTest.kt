package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.result.ParseResult
import de.kleinert.parsewisp.util.Transform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Map
import java.util.function.Function
import java.util.function.IntFunction

internal class ParserAsFunctionTest {
    @Test
    fun asMapper() {
        val p = parser("S = #'[A-Z]'")
        val strings =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ".chars().mapToObj<String>(IntFunction { it: Int -> it.toChar().toString() })
                .toList()
        Assertions.assertEquals(
            strings.stream().map<ParseResult?> { it: String -> p.parse(it, ParsingOptions.default) }.toList(),
            strings.stream().map<ParseResult?> { text: String -> p.parse(text) }.toList()
        )
    }

    @Test
    fun asMapper2() {
        val transformMap = Map.of<Sym?, Function<MutableList<Any?>?, Any?>?>(
            sym("S"), Function { o: MutableList<Any?>? ->
                o!!.stream()
                    .map<String> { it: Any? -> it as String? }
                    .map<Char> { it: String -> it.get(0) }
                    .map<Int> { i: Char -> i.code }
                    .findFirst().orElseThrow()
            }
        )
        val p = parser("S = #'[A-Z]'")
        val strings =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ".chars().mapToObj<String>(IntFunction { it: Int -> it.toChar().toString() })
                .toList()
        val ints = strings.stream().map<Int?> { s: String -> s.get(0).code }.toList()

        Assertions.assertEquals(
            ints,
            strings.stream().map<ParseResult?> { text: String -> p.parse(text) }
                .map<Any?> { it: ParseResult? -> Transform.transform(it!!, transformMap) }.toList()
        )
    }
}
