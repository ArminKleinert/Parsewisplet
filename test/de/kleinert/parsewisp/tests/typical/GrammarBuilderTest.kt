package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.grammar.GrammarBuilder
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.List
import java.util.function.BiFunction
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.Stream

internal class GrammarBuilderTest {
    @Test
    fun equivalentToStringGrammar() {
        val gFromString = parser(
            """
                                S = NUMBER NUMBER*
                                NUMBER = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
                                
                                """.trimIndent()
        )
            .grammar
        val gFromGB = object : GrammarBuilder() {
            override fun make() {
                addProduction(
                    "NUMBER",
                    altUnsafe(
                        Stream.of<String?>("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
                            .map<Rule?> { string: String? -> this.stringCS(string!!) }.toList()
                    )
                )
                addProduction("S", cat(nt("NUMBER"), rep(nt(sym("NUMBER")))))
                setStartProduction(sym("S"))
            }
        }.build()

        Assertions.assertEquals(gFromString, gFromGB)

        val pFromString = parser(gFromString, ParserCreationOptions.getDefault().withStartProduction(sym("S")))
        val pFromGB = parser(gFromGB, ParserCreationOptions.getDefault().withStartProduction(sym("S")))
        val text = "0123456789"
        Assertions.assertEquals(pFromString.parse(text), pFromGB.parse(text))
    }

    @Test
    fun equivalentToMoreExplicitGrammar() {
        val pGrammarList = LinkedHashMap<Sym?, Rule?>()
        pGrammarList.put(
            sym("S"),
            ConcatRule.create(
                List.of<Rule>(
                    NonTerminal.create(sym("NUMBER")),
                    ZeroOrMoreRule.create(NonTerminal.create(sym("NUMBER")))
                )
            )
        )
        pGrammarList.put(
            sym("NUMBER"),
            AlternationRule.create(
                Stream.of<String?>("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
                    .map<Rule?> { it: String? -> StringTerm.create(it!!, true) }.toList()
            )
        )
        val pFromGrammar = parser(
            Grammar(sym("S"), pGrammarList),
            ParserCreationOptions.getDefault().withStartProduction(sym("S"))
        )
            .grammar

        val pFromGB = object : GrammarBuilder() {
            override fun make() {
                addProduction("S", cat(nt("NUMBER"), zeroOrMore(nt(sym("NUMBER")))))
                addProduction(
                    "NUMBER",
                    altUnsafe(
                        Stream.of<String?>("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
                            .map<Rule?> { string: String? -> this.stringCI(string!!) }.toList()
                    )
                )
            }
        }.build()

        Assertions.assertEquals(pFromGrammar, pFromGB)
    }

    @Test
    fun testGrammarBuilderFeatures() {
        // A grammar which has 9 different ways to match at least one number/underscore.
        val gFromGB = object : GrammarBuilder() {
            override fun make() {
                addProduction(
                    "S", ordAlt(
                        List.of<Rule?>(
                            nt("A"), nt("B"), nt("C"), nt("D"),
                            nt("E"), nt("F"), nt("G"), nt("H"),
                            nt("I"), nt("J"), nt("K"), eof()
                        )
                    )
                )
                addProduction("A", cat(rep(regex("[0-9_]"), 1, Int.MAX_VALUE)))
                addProduction("B", cat(regex("[0-9_]"), repMax(string(""), 0), repMax(regex("[0-9_]"), Int.MAX_VALUE)))
                addProduction("C", cat(repMin(regex("[0-9_]"), 2)))
                addProduction("D", cat(regex("[0-9_]"), zeroOrMore(regex("[0-9_]"))))
                addProduction("E", cat(rep(regex("[0-9_]"), 1), rep(regex("[0-9_]"), 0), zeroOrMore(regex("[0-9_]"))))
                addProduction("F", repMin(alt(numVal('0'.code, '9'.code), numVal(0x5F), numVal(0x60)), 1))
                addProduction(
                    "G", onceOrMore(
                        altUnsafe(
                            Stream.concat<Int?>(
                                IntStream.range('0'.code, '9'.code + 1).boxed(),
                                Stream.of<Int?>('_'.code)
                            )
                                .map<String> { i: Int? -> i!!.toChar().toString() }
                                .map<Rule?> { s: String? -> StringTerm.create(s!!, false) }
                                .collect(Collectors.toList()))))
                addProduction(
                    "H", onceOrMore(
                        altUnsafe(
                            Stream.concat<Int?>(
                                IntStream.range('0'.code, '9'.code + 1).boxed(),
                                Stream.of<Int?>('_'.code)
                            )
                                .map<String> { i: Int? -> i!!.toChar().toString() }
                                .map<Rule?> { s: String? -> StringTerm.create(s!!, false) }
                                .collect(Collectors.toList()))))
                addProduction("I", cat(regex("[0-9_]"), opt(onceOrMore(regex("[0-9_]")))))
                addProduction("J", onceOrMore(oneOf("0123456789")))
                addProduction("K", onceOrMore(oneOf("0123456789abcdefABCDEF")))
            }
        }.build()
        val p = parser(gFromGB, ParserCreationOptions.getDefault())

        Assertions.assertEquals(
            List.of<ParseTree?>(
                PT.create("S", PT.create("A", "9", "9")),
                PT.create("S", PT.create("B", "9", "9")),
                PT.create("S", PT.create("C", "9", "9")),
                PT.create("S", PT.create("D", "9", "9")),
                PT.create("S", PT.create("E", "9", "9")),
                PT.create("S", PT.create("F", "9", "9")),
                PT.create("S", PT.create("G", "9", "9")),
                PT.create("S", PT.create("H", "9", "9")),
                PT.create("S", PT.create("I", "9", "9")),
                PT.create("S", PT.create("J", "9", "9")),
                PT.create("S", PT.create("K", "9", "9"))
            ),
            parser(gFromGB, ParserCreationOptions.getDefault()).parses("99")
        )
        Assertions.assertEquals(List.of<ParseTree?>(PT.create("S")), p.parses(""))
    }

    @Test
    fun testWithTransform() {
        val gFromGB = object : GrammarBuilder() {
            override fun make() {
                addProduction(
                    "S",
                    zeroOrMore(
                        transform(
                            cat(regex("[0-9]+"), opt(string(" ")).hideInResult()),
                            BiFunction { r: MutableList<Any?>?, i: Int? -> List.of<Any?>((r!!.get(0) as String?)!!.toLong()) })
                    )
                )
                setStartProduction(sym("S"))
            }
        }.build()

        val p = parser(gFromGB, ParserCreationOptions.getDefault().withStartProduction(sym("S")))
        val text = "123 456 789"
        Assertions.assertEquals(
            PT.create("S", 123L, 456L, 789L),
            p.parse(text)
        )
    }
}