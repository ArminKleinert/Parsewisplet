package de.kleinert.parsewisp.tests

import de.kleinert.parsewisp.CfgGrammar.Companion.makeCfg
import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.grammar.GrammarPrinter
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parser_options.ParserCreationOptions.Companion.newWithStandardWhitespace
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.parsing.AlternationRule
import de.kleinert.parsewisp.parsing.ConcatRule
import de.kleinert.parsewisp.parsing.StringTerm.Companion.create
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

internal class NewFeaturesTest {
    @Test
    fun t10() {
        val regex = Regex("[^, \\r\\t\\n<>(){}\\[\\]+*?:=|'\"#&!;./%\\-0-9][^, \\r\\t\\n<>(){}\\[\\]+*?:=|'\"#&!;./%]*")
        println(regex.matchAt("1 = 'a'", 0)?.value)
    }

    @Test
    fun test00() {
        val p = parser(
            "S = P S | eps ; P = \"(\" S* \")\" ;",
            ParserCreationOptions.default.withPrinter(GrammarPrinter.default)
        )
        println(p.show()) // Use default printer.
        println(GrammarPrinter.default.toString(p.grammar)) // Use specific printer.

        println(p.printer.escape("\"a88b\nc\"\\", '\''.code))

        val rule0 =
            AlternationRule.create(
                listOf(
                    ConcatRule.create(
                        listOf(
                            create("a", false),
                            create("b", false)
                        )
                    ),
                    ConcatRule.create(
                        listOf(
                            create("c", false),
                            create("d", false)
                        )
                    )
                )
            )
        println(p.printer.ruleToString(rule0))
        val rule1 = ConcatRule.create(
            listOf(
                AlternationRule.create(
                    listOf(
                        create("a", false),
                        create("b", false)
                    )
                ),
                AlternationRule.create(
                    listOf(
                        create("c", false),
                        create("d", false)
                    )
                )
            )
        )
        println(p.printer.ruleToString(rule1))
    }

    @Test
    fun test0() {
        val p = parser(
            """
                S = &(A !'b') 'a'* B
                A = ('a' A 'b')?
                B = ('b' B 'c')?
                
                """.trimIndent()
        )
        println(p.parse("aabbcc"))
        println(p.parse("aaabbbccc"))
        val n = 5
        for (a in 0..<n) {
            for (b in 0..<n) {
                for (c in 0..<n) {
                    val s = "a".repeat(a) + "b".repeat(b) + "c".repeat(c)
                    if (p.parse(s).isSuccess) println(s)
                }
            }
        }
    }

    @Test
    fun test1() {
        val grammar = """
                S = 'a' A
                A = epsilon A | epsilon
                
                """.trimIndent()
        val p = parser(grammar)
        println(p.parse("a"))
        println(p.parses("a").stream().limit(5).toList())
    }

    @Test
    fun testExampleFromWikipedia() {
        val grammar = """
                S = digit digit-excluding-zero*
                digit = "0" | digit-excluding-zero ;
                digit-excluding-zero = "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
                
                """.trimIndent()
        val p = parser(grammar)
        println(p.parse("91"))
        println(p.parse("091"))
    }

    @Test
    fun testArithmetic() {
        val p = parser(
            """
                sum          = sum ('+'|'-') sum | product
                product      = power ('*'|'/') product | power
                power        = paren-or-val '^' power | paren-or-val
                paren-or-val = '(' sum ')' | number
                number       = ('+'|'-')? ('0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9')+
                
                """.trimIndent(), newWithStandardWhitespace()
        )
        println(p.parse("1"))
        println(p.parse("1+2"))
        println(p.parse("1 + 2"))
        println(p.parse("1*2"))
        println(p.parse("1+2*3^4"))
        println(p.parse("1*2+3*4"))
        println(p.parse("1*(2+-3)^4"))
    }

    @Test
    fun negativeEpsilon() {
        val p = parser("S = !epsilon epsilon 'a'")
        println(p.parse("a"))
    }

    @Test
    fun zeroOrMoreRuleCausesInfiniteEpsilonProblem() {
        val p = parser("S = (S epsilon)*", ParserCreationOptions.default)
        println(p.grammar.analyze().definedTerminals())
        println(p.parses("").stream().limit(5).toList())
    }

    @Test
    @Throws(IOException::class)
    fun bigTest() {
        val p = parser(
            Files.readString(Path.of("testres/grammars/c99.grammar")),
            newWithStandardWhitespace()
        )
        val res = p.parse(
            """
                int utf8_from_codepoint(utf8_code_pt c, utf8_chr *const buff) {
                  if (c < 0x80) {
                    buff[0] = ((c >> 0) & 0x7F) | 0x00;
                    return 1;
                  } else if (c < 0x0800) {
                    buff[0] = ((c >> 6) & 0x1F) | 0xC0;
                    buff[1] = ((c >> 0) & 0x3F) | 0x80;
                    return 2;
                  } else if (c < 0x010000) {
                    buff[0] = ((c >> 12) & 0x0F) | 0xE0;
                    buff[1] = ((c >> 6) & 0x3F) | 0x80;
                    buff[2] = ((c >> 0) & 0x3F) | 0x80;
                    return 3;
                  } else if (c < 0x110000) {
                    buff[0] = ((c >> 18) & 0x07) | 0xF0;
                    buff[1] = ((c >> 12) & 0x3F) | 0x80;
                    buff[2] = ((c >> 6) & 0x3F) | 0x80;
                    buff[3] = ((c >> 0) & 0x3F) | 0x80;
                    return 4;
                  }
                  return -1;
                }
                
                """.trimIndent().repeat(156)
        )
        println(res.castToParseSuccess().spanStart)
        println(res.castToParseSuccess().spanEndExclusive)
    }

    @Test
    fun orderedChoiceTest() {
        run {
            val p = parser(
                """
                    S = A / B / C / D / E
                    A = ε
                    B = ε
                    C = ε
                    D = ε
                    E = ε
                    
                    """.trimIndent()
            )
            println(p)
            val ps = p.parses("", ParsingOptions.default).stream().toList()
            println()
            println("Expect: [[:S, [:A]], [:S, [:B]], [:S, [:C]], [:S, [:D]], [:S, [:E]]]")
            println("Have:   " + ps)
        }
        run {
            val grammar = """
                    S = (r1 / r2)*
                    r1 = 'a'
                    r2 = 'a'
                    
                    """.trimIndent()
            val text = "aa"
            val p = parser(grammar)
            val possibleParses = listOf(
                PT.create("S", PT.create("r1", "a"), PT.create("r1", "a")),
                PT.create("S", PT.create("r2", "a"), PT.create("r1", "a")),
                PT.create("S", PT.create("r1", "a"), PT.create("r2", "a")),
                PT.create("S", PT.create("r2", "a"), PT.create("r2", "a"))
            )
            println("Expect: " + possibleParses)
            println("Have:   " + p.parses(text))
        }
    }

    @Test
    fun wsExample1() {
        val whitespace = parser(
            """
                        whitespace = #'\s+'
                        
                        """.trimIndent()
        )
        val auto_whitespace_example = parser(
            """
                        S = A B
                        <A> = 'foo'
                        <B> = #'\d+'
                        
                        """.trimIndent(),
            newWithStandardWhitespace()
        )

        val tree = PT.create("S", "foo", "123")

        Assertions.assertEquals(tree, auto_whitespace_example.parse("foo 123"))
    }

    @Test
    fun simple() {
        val g = """
                Expression = Term , { ( '+' | '-' ) , Term } ;
                Term       = Factor , { ( '*' | '/' ) , Factor } ;
                Factor     = Number | '(', Expression, ')' ;
                Number     = ['+' | '-' ] Digit , { Digit } ;
                Digit      = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ;
                
                """.trimIndent()
        val p = parser(g)
        println(p.parse("(8-9)*-20/18+1"))
    }

    @Test
    fun repRepTest() {
        val p = parser("S = 0*4A\n<A> = 'a'")
        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
        Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa"))
        Assertions.assertEquals(PT.create("S", "a", "a", "a", "a"), p.parse("aaaa"))
    }

    @Test
    fun exclusionFullTest1() {
        val p6 = parser("S := #'[0-9]+' - ('11' | '13')")
        println(p6)
        println("---")
        println(p6.parse("12"))
        println("---")
        println(p6.parse("11"))
    }

    @Test
    fun exclusionFullTest() {
        val p6 = parser("S := #'[0-9]+' - '11'")
        println(p6)
        println("---")
        println(p6.parse("12"))
        println("---")
        println(p6.parse("11"))
    }

    @Test
    fun exclusionTest() {
        val p6 = parser("S := #'[0-9]+' - '11' 'a'")
        println(p6)
        println("---")
        println(p6.parse("12a"))
        println("---")
        println(p6.parse("12"))
        println("---")
        println(p6.parse("11a"))
        println("---")
        println(p6.parse("ba"))
    }
}
