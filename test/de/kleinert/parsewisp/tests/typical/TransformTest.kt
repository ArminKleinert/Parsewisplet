package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.testutil.PT
import de.kleinert.parsewisp.util.Transform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TransformTest {
    @Test
    fun testAdd1() {
        val tree = PT.create("S", "1", "2", "3")
        val transformFn: (List<Any?>) -> Any? = { o -> o.sumOf { (it as String).toInt() } }
        val transformMap = mapOf(Sym.sym("S") to transformFn)

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
        val p = Parsewisp.parser(
            """
                S     = NUM (<'+'> NUM)*
                <NUM> = #'\d+'
                
                """.trimIndent()
        )
        val tree = p.parse("1+2+3").castToParseSuccess()
        val transformFn: (List<Any?>) -> Any? = { o -> o.sumOf { it: Any? -> (it as String?)!!.toInt() } }
        val transformMap = mapOf(Sym.sym("S") to transformFn)

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
        val p = Parsewisp.parser(
            """
                S   = NUM ('+' NUM)*
                NUM = #'\d+'
                
                """.trimIndent()
        )
        val tree = p.parse("1+2+3").castToParseSuccess()
        val transformMap: Map<Sym, (List<Any?>) -> Any?> = mapOf(
            Sym.sym("S") to { o ->
                o.filter { it != "+" }
                    .map { it as String }
                    .sumOf { s -> s.toInt() } as Any?
            },
            Sym.sym("NUM") to { o -> o[0] }
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
        val p = Parsewisp.parser(
            """
                S   = A
                A   = NUM ('+' NUM)*
                NUM = #'\d+'
                
                """.trimIndent()
        )
        val tree = p.parse("1+2+3").castToParseSuccess()
        val transformMap: Map<Sym, (List<Any?>) -> Any?> = mapOf(
            Sym.sym("A") to { o ->
                o.filter { it != "+" }
                    .map { it as String }
                    .sumOf { s -> s.toInt() }
                    .toString()
            },
            Sym.sym("NUM") to { o -> o[0] }
        )

        Assertions.assertEquals(
            listOf(Sym.sym("S"), "6"),
            Transform.transform(tree, transformMap)
        )
        Assertions.assertEquals(
            listOf(Sym.sym("S"), "6"),
            Transform.transform(tree, transformMap, { o: Any? -> o as MutableList<*>? })
        )
    }
}