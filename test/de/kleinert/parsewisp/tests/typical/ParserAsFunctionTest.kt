package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.util.Transform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ParserAsFunctionTest {
    @Test
    fun asMapper() {
        val p = Parsewisp.parser("S = #'[A-Z]'")
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
            Sym.sym("S") to { o ->
                o.map { it as String? }
                    .map { it!![0] }
                    .map { i -> i.code }
                    .first()
            }
        )
        val p = Parsewisp.parser("S = #'[A-Z]'")
        val strings =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray().map { it.toString() }
        val ints = strings.map { s -> s[0].code }

        Assertions.assertEquals(
            ints,
            strings
                .map { text -> p.parse(text) }
                .map { Transform.transform(it, transformMap) }
        )
    }
}
