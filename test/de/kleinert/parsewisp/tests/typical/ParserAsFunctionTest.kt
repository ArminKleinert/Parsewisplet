package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.result.ParseResult
import de.kleinert.parsewisp.util.Transform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ParserAsFunctionTest {
    @Test
    fun asMapper() {
        val p = parser("S = #'[A-Z]'")
        val strings =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray().map { it.toString() }
        Assertions.assertEquals(
            strings.map { it: String -> p.parse(it, ParsingOptions.default) },
            strings.map { text: String -> p.parse(text) }
        )
    }

    @Test
    fun asMapper2() {
        val transformMap = mapOf<Sym, (List<Any?>)-> Any?>(
            sym("S") to { o ->
                o.map { it as String? }
                    .map { it!![0] }
                    .map { i -> i.code }
                    .first()
            }
        )
        val p = parser("S = #'[A-Z]'")
        val strings =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray().map { it.toString() }
        val ints = strings.stream().map<Int?> { s: String -> s[0].code }.toList()

        Assertions.assertEquals(
            ints,
            strings.stream().map<ParseResult?> { text: String -> p.parse(text) }
                .map<Any?> { it: ParseResult? -> Transform.transform(it!!, transformMap) }.toList()
        )
    }
}
