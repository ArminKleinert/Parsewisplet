package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.testutil.PT
import de.kleinert.parsewisp.util.Transform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.List
import java.util.Map
import java.util.function.Function

internal class TransformTest {
    @Test
    fun testAdd1() {
        val tree = PT.create("S", "1", "2", "3")
        val transformFn: Function<MutableList<Any?>?, Any?> =
            Function { o: MutableList<Any?>? -> o!!.stream().mapToInt { it: Any? -> (it as String?)!!.toInt() }.sum() }
        val transformMap = Map.of<Sym?, Function<MutableList<Any?>?, Any?>?>(sym("S"), transformFn)

        Assertions.assertEquals(
            6,
            Transform.transform(tree, transformMap)
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, Function { o: Any? -> o as Int? })
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, Function { o: Any? -> o as Int? })
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
        val transformFn: Function<MutableList<Any?>?, Any?> =
            Function { o: MutableList<Any?>? -> o!!.stream().mapToInt { it: Any? -> (it as String?)!!.toInt() }.sum() }
        val transformMap = Map.of<Sym?, Function<MutableList<Any?>?, Any?>?>(sym("S"), transformFn)

        Assertions.assertEquals(
            6,
            Transform.transform(tree, transformMap)
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, Function { o: Any? -> o as Int? })
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, Function { o: Any? -> o as Int? })
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
        val transformMap = Map.of<Sym?, Function<MutableList<Any?>?, Any?>?>(
            sym("S"), Function { o: MutableList<Any?>? ->
                o!!.stream()
                    .filter { it: Any? -> it != "+" }
                    .map<String?> { it: Any? -> it as String }
                    .mapToInt { s: String? -> s!!.toInt() }
                    .sum()
            },
            sym("NUM"), Function { o: MutableList<Any?>? -> o!!.get(0) }
        )

        Assertions.assertEquals(
            6,
            Transform.transform(tree, transformMap)
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, Function { o: Any? -> o as Int? })
        )
        Assertions.assertEquals(
            6,
            Transform.transform<Int?>(tree, transformMap, Function { o: Any? -> o as Int? })
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
        val transformMap = Map.of<Sym?, Function<MutableList<Any?>?, Any?>?>(
            sym("A"), Function { o: MutableList<Any?>? ->
                o!!
                    .stream()
                    .filter { it: Any? -> it != "+" }
                    .map<String?> { it: Any? -> it as String }
                    .mapToInt { s: String? -> s!!.toInt() }
                    .sum().toString()
            },
            sym("NUM"), Function { o: MutableList<Any?>? -> o!!.get(0) }
        )

        Assertions.assertEquals(
            List.of<Any>(sym("S"), "6"),
            Transform.transform(tree, transformMap)
        )
        Assertions.assertEquals(
            List.of<Any>(sym("S"), "6"),
            Transform.transform(tree, transformMap, Function { o: Any? -> o as MutableList<*>? })
        )
    }
}