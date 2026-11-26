package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.testutil.PT
import de.kleinert.parsewisp.util.Transform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.function.Function

internal class TransformTest {
    @Test
    fun testAdd1() {
        val tree = PT.create("S", "1", "2", "3")
        val transformFn: Function<List<Any?>, Any?> =
            Function { o -> o.stream().mapToInt { it: Any? -> (it as String?)!!.toInt() }.sum() }
        val transformMap = mapOf(sym("S")to transformFn)

        Assertions.assertEquals(
            6,
            Transform.transform(tree, transformMap)
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, { o: Any? -> o as Int? })
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, { o: Any? -> o as Int? })
        )
    }

    @Test
    fun testAdd2() {
        val p = parser(
            """
                S     = NUM (<'+'> NUM)*
                <NUM> = #'\d+'
                
                """.trimIndent()
        )
        val tree = p.parse("1+2+3").castToParseSuccess()
        val transformFn: Function<List<Any?>, Any?> =
            { o -> o.stream().mapToInt { it: Any? -> (it as String?)!!.toInt() }.sum() }
        val transformMap = mapOf(sym("S")to transformFn)

        Assertions.assertEquals(
            6,
            Transform.transform(tree, transformMap)
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, { o: Any? -> o as Int? })
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, { o: Any? -> o as Int? })
        )
    }

    @Test
    fun testAdd3() {
        val p = parser(
            """
                S   = NUM ('+' NUM)*
                NUM = #'\d+'
                
                """.trimIndent()
        )
        val tree = p.parse("1+2+3").castToParseSuccess()
        val transformMap:Map<Sym, Function<List<Any?>, Any?>> = mapOf(
            sym("S")to Function{ o: List<Any?> ->
                o.filter { it != "+" }
                    .map { it as String }
                    .sumOf { s -> s.toInt() } as Any?
            },
            sym("NUM")to Function{ o: List<Any?> -> o.get(0) }
        )

        Assertions.assertEquals(
            6,
            Transform.transform(tree, transformMap)
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, { o: Any? -> o as Int? })
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, { o: Any? -> o as Int? })
        )
    }

    @Test
    fun testAdd4() {
        val p = parser(
            """
                S   = A
                A   = NUM ('+' NUM)*
                NUM = #'\d+'
                
                """.trimIndent()
        )
        val tree = p.parse("1+2+3").castToParseSuccess()
        val transformMap: Map<Sym, Function<List<Any?>, Any?>> = mapOf(
            sym("A")to Function { o ->
                o.filter { it != "+" }
                    .map { it as String }
                    .sumOf { s -> s.toInt() }
                    .toString()
            },
            sym("NUM")to Function { o -> o.get(0) }
        )

        Assertions.assertEquals(
            listOf(sym("S"), "6"),
            Transform.transform(tree, transformMap)
        )
        Assertions.assertEquals(
            listOf(sym("S"), "6"),
            Transform.transform(tree, transformMap, { o: Any? -> o as MutableList<*>? })
        )
    }
}