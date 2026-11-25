package de.kleinert.parsewisp.tests.typical.grammars

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

internal class TestGrammarEdn {
    private fun parser(): Parser {
        try {
            return parser(
                Files.readString(Path.of("testres/grammars/edn.grammar"))
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun symbolList(): MutableList<String> {
        return mutableListOf<String>("a", "+", "*", "<a>", "a:", "+:", "*#", "<a>#")
    }

    private fun symbolListWithNS(): List<String> {
        return symbolList().stream().map { it: String? -> it + '/' + "sym" }.toList()
    }

    private fun keywordList(): List<String> {
        return symbolList().stream().map { it: String -> ':'.toString() + it }.toList()
    }

    private fun keywordListWithNS(): List<String> {
        return symbolListWithNS().stream().map { it: String -> ':'.toString() + it }.toList()
    }

    @Test
    @Throws(IOException::class)
    fun verifyWithFile() {
        val parser = parser()
        val text = Files.readString(Path.of("testres/other/edntest.edn"))
        Assertions.assertTrue(parser.parse(text).isSuccess())
    }

    @Test
    fun verifyLineComment() {
        val parser = parser()
        val text = "1 ;abf\n 2"
        Assertions.assertEquals(
            PT.create(
                "Edn",
                PT.create(
                    "expression",
                    PT.create("Number", PT.create("Int", "1"))
                ),
                PT.create(
                    "expression",
                    PT.create("Number", PT.create("Int", "2"))
                )
            ),
            parser.parse(text)
        )
    }

    @Test
    fun verifyLineComment2() {
        val parser = parser()
        val text = "3 #_1 4 ; abf\n"
        Assertions.assertEquals(
            PT.create(
                "Edn",
                PT.create(
                    "expression",
                    PT.create("Number", PT.create("Int", "3"))
                ),
                PT.create(
                    "expression",
                    PT.create("Number", PT.create("Int", "4"))
                )
            ),
            parser.parse(text)
        )
    }

    @Test
    fun verifyLineComment3() {
        val parser = parser()
        val text = "1.3#_2 #_4 ; #_abf\n5"
        Assertions.assertEquals(
            PT.create(
                "Edn",
                PT.create(
                    "expression",
                    PT.create("Number", PT.create("Float", "1.3"))
                ),
                PT.create(
                    "expression",
                    PT.create("Number", PT.create("Int", "5"))
                )
            ),
            parser.parse(text)
        )
    }

    @Test
    fun verifyLineCommentFollowedByEof() {
        val parser = parser()
        val text = "1.3#_2 #_4 ; #_abf"
        Assertions.assertEquals(
            PT.create(
                "Edn",
                PT.create(
                    "expression",
                    PT.create("Number", PT.create("Float", "1.3"))
                )
            ),
            parser.parse(text)
        )
    }

    @Test
    fun verifySymbol() {
        val parser = parser()
        for (s in symbolList()) {
            Assertions.assertEquals(
                PT.create(
                    "Edn",
                    PT.create(
                        "expression",
                        PT.create("Symbol", s)
                    )
                ),
                parser.parse(s)
            )
        }
    }

    @Test
    fun verifySymbolWithNS() {
        val parser = parser()
        for (s in symbolListWithNS()) {
            val parts = s.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            Assertions.assertEquals(
                PT.create(
                    "Edn",
                    PT.create(
                        "expression",
                        PT.create("Symbol", parts[0], parts[1])
                    )
                ),
                parser.parse(s)
            )
        }
    }

    @Test
    fun verifyKeyword() {
        val parser = parser()
        for (s in keywordList()) {
            Assertions.assertEquals(
                PT.create(
                    "Edn",
                    PT.create(
                        "expression",
                        PT.create("Keyword", PT.create("Symbol", s.substring(1)))
                    )
                ),
                parser.parse(s)
            )
        }
    }

    @Test
    fun verifyKeywordWithNS() {
        val parser = parser()
        for (s in keywordListWithNS()) {
            val parts = s.substring(1).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            Assertions.assertEquals(
                PT.create(
                    "Edn",
                    PT.create(
                        "expression",
                        PT.create("Keyword", PT.create("Symbol", parts[0], parts[1]))
                    )
                ),
                parser.parse(s)
            )
        }
    }

    @Test
    fun verifyList() {
        val parser = parser()

        Assertions.assertEquals(
            PT.create("Edn", PT.create("expression", PT.create("List"))),
            parser.parse("()")
        )
        Assertions.assertEquals(
            PT.create(
                "Edn",
                PT.create(
                    "expression",
                    PT.create(
                        "List",
                        PT.create(
                            "expression",
                            PT.create("Number", PT.create("Int", "12"))
                        )
                    )
                )
            ),
            parser.parse("(12)")
        )

        val tree_1_2 =
            PT.create(
                "Edn",
                PT.create(
                    "expression",
                    PT.create(
                        "List",
                        PT.create(
                            "expression",
                            PT.create("Number", PT.create("Int", "1"))
                        ),
                        PT.create(
                            "expression",
                            PT.create("Number", PT.create("Int", "2"))
                        )
                    )
                )
            )
        Assertions.assertEquals(tree_1_2, parser.parse("(1 2)"))
        Assertions.assertEquals(tree_1_2, parser.parse("( 1 2 )"))

        Assertions.assertEquals(
            PT.create(
                "Edn",
                PT.create(
                    "expression",
                    PT.create(
                        "List",
                        PT.create(
                            "expression",
                            PT.create(
                                "List",
                                PT.create(
                                    "expression",
                                    PT.create("Number", PT.create("Int", "1"))
                                )
                            )
                        ),
                        PT.create(
                            "expression",
                            PT.create("List")
                        )
                    )
                )
            ),
            parser.parse("( (1) () )")
        )
    }

    @Test
    fun verifyVector() {
        val parser = parser()

        Assertions.assertEquals(
            PT.create("Edn", PT.create("expression", PT.create("Vector"))),
            parser.parse("[]")
        )
        Assertions.assertEquals(
            PT.create(
                "Edn",
                PT.create(
                    "expression",
                    PT.create(
                        "Vector",
                        PT.create(
                            "expression",
                            PT.create("Number", PT.create("Int", "12"))
                        )
                    )
                )
            ),
            parser.parse("[12]")
        )

        val tree_1_2 =
            PT.create(
                "Edn",
                PT.create(
                    "expression",
                    PT.create(
                        "Vector",
                        PT.create(
                            "expression",
                            PT.create("Number", PT.create("Int", "1"))
                        ),
                        PT.create(
                            "expression",
                            PT.create("Number", PT.create("Int", "2"))
                        )
                    )
                )
            )
        Assertions.assertEquals(tree_1_2, parser.parse("[1 2]"))
        Assertions.assertEquals(tree_1_2, parser.parse("[ 1 2 ]"))

        Assertions.assertEquals(
            PT.create(
                "Edn",
                PT.create(
                    "expression",
                    PT.create(
                        "Vector",
                        PT.create(
                            "expression",
                            PT.create(
                                "Vector",
                                PT.create(
                                    "expression",
                                    PT.create("Number", PT.create("Int", "1"))
                                )
                            )
                        ),
                        PT.create(
                            "expression",
                            PT.create("Vector")
                        )
                    )
                )
            ),
            parser.parse("[ [1] [] ]")
        )
    }

    @Test
    fun verifyMap() {
        val parser = parser()
        Assertions.assertTrue(parser.parse("{}").isSuccess())
        Assertions.assertTrue(parser.parse("{12}").isFailure())
        Assertions.assertTrue(parser.parse("{1 2}").isSuccess())
        Assertions.assertTrue(parser.parse("{ (1) []}").isSuccess())
    }

    @Test
    fun verifyTypeDispatch() {
        val parser = parser()
        Assertions.assertTrue(parser.parse("#m/n {}").isSuccess())
        Assertions.assertTrue(parser.parse("#m/n {:name \"abc\"}").isSuccess())
    }

    @Test
    fun verifyInts() {
        val parser = parser()
        Assertions.assertTrue(parser.parse("0").isSuccess())
        Assertions.assertTrue(parser.parse("+0").isSuccess())
        Assertions.assertTrue(parser.parse("-0").isSuccess())
        Assertions.assertTrue(parser.parse("1").isSuccess())
        Assertions.assertTrue(parser.parse("+1").isSuccess())
        Assertions.assertTrue(parser.parse("-1").isSuccess())
    }

    @Test
    fun verifyFloat() {
        val parser = parser()
        Assertions.assertTrue(parser.parse("1.0").isSuccess())
        Assertions.assertTrue(parser.parse("+1.0").isSuccess())
        Assertions.assertTrue(parser.parse("-1.0").isSuccess())
    }

    @Test
    fun verifyFloatExpFormat() {
        val parser = parser()
        Assertions.assertTrue(parser.parse("1e+1").isSuccess())
        Assertions.assertTrue(parser.parse("1e-1").isSuccess())
        Assertions.assertTrue(parser.parse("1E+1").isSuccess())
        Assertions.assertTrue(parser.parse("1E-1").isSuccess())
        Assertions.assertTrue(parser.parse("1.0e+1").isSuccess())
        Assertions.assertTrue(parser.parse("1.0e-1").isSuccess())
        Assertions.assertTrue(parser.parse("1.0E+1").isSuccess())
        Assertions.assertTrue(parser.parse("1.0E-1").isSuccess())

        Assertions.assertTrue(parser.parse("+1e+1").isSuccess())
        Assertions.assertTrue(parser.parse("+1e-1").isSuccess())
        Assertions.assertTrue(parser.parse("+1E+1").isSuccess())
        Assertions.assertTrue(parser.parse("+1E-1").isSuccess())
        Assertions.assertTrue(parser.parse("+1.0e+1").isSuccess())
        Assertions.assertTrue(parser.parse("+1.0e-1").isSuccess())
        Assertions.assertTrue(parser.parse("+1.0E+1").isSuccess())
        Assertions.assertTrue(parser.parse("+1.0E-1").isSuccess())

        Assertions.assertTrue(parser.parse("-1e+1").isSuccess())
        Assertions.assertTrue(parser.parse("-1e-1").isSuccess())
        Assertions.assertTrue(parser.parse("-1E+1").isSuccess())
        Assertions.assertTrue(parser.parse("-1E-1").isSuccess())
        Assertions.assertTrue(parser.parse("-1.0e+1").isSuccess())
        Assertions.assertTrue(parser.parse("-1.0e-1").isSuccess())
        Assertions.assertTrue(parser.parse("-1.0E+1").isSuccess())
        Assertions.assertTrue(parser.parse("-1.0E-1").isSuccess())
    }
}
