package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.result.ParseResult
import de.kleinert.parsewisp.util.Transform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.function.Function
import java.util.function.IntFunction

internal class ParserAsFunctionTest {
    @Test
    fun asMapper() {
        val p = parser("S = #'[A-Z]'")
        val strings =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ".chars().mapToObj(IntFunction { it: Int -> it.toChar().toString() })
                .toList()
        Assertions.assertEquals(
            strings.stream().map<ParseResult?> { it: String -> p.parse(it, ParsingOptions.default) }.toList(),
            strings.stream().map<ParseResult?> { text: String -> p.parse(text) }.toList()
        )
    }

    @Test
    fun asMapper2() {
        val transformMap = mapOf<Sym, Function<List<Any?>, Any?>>(
            sym("S") to Function { o ->
                o.map { it as String? }
                    .map { it!!.get(0) }
                    .map { i -> i.code }
                    .first()
            }
        )
        val p = parser("S = #'[A-Z]'")
        val strings =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ".chars().mapToObj(IntFunction { it: Int -> it.toChar().toString() })
                .toList()
        val ints = strings.stream().map<Int?> { s: String -> s.get(0).code }.toList()

        Assertions.assertEquals(
            ints,
            strings.stream().map<ParseResult?> { text: String -> p.parse(text) }
                .map<Any?> { it: ParseResult? -> Transform.transform(it!!, transformMap) }.toList()
        )
    }
}
