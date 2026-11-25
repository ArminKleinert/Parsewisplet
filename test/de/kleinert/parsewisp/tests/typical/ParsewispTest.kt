package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.error.ParserCreationFailure
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.parser_options.UnhideOptions
import de.kleinert.parsewisp.result.Node
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.util.List
import java.util.Set

internal class ParsewispTest {
    @Test
    fun simplifiedParseTreeCreation() {
        val pt1 = PT.create("S", "a", "a")
        val pt2 = PT.create(sym("S"), List.of<Node>(Node.of("a"), Node.of("a")))
        Assertions.assertEquals(pt2, pt1)
    }

    @Test
    fun singleOrDoubleQuotationEquivalenceForStrings() {
        val pSingleQuoted = """
                S = 'a' 'b"c\''
                
                """.trimIndent()
        val pDoubleQuoted = """
                S = "a" "b\${'"'}c'"
                
                """.trimIndent()

        // Valid parse
        Assertions.assertEquals(
            parser(pSingleQuoted).parse("ab\"c'"),
            parser(pDoubleQuoted).parse("ab\"c'")
        )

        // Invalid parse
        Assertions.assertEquals(
            parser(pSingleQuoted).parse(""),
            parser(pDoubleQuoted).parse("")
        )

        // The parsers are the same on the inside.
        Assertions.assertEquals(
            parser(pSingleQuoted).grammar,
            parser(pDoubleQuoted).grammar
        )
    }

    @Test
    fun singleOrDoubleQuotationEquivalenceForRegexes() {
        val pSingleQuoted = """
                S = #'a' #'b"c\''
                
                """.trimIndent()
        val pDoubleQuoted = """
                S = #"a" #"b\${'"'}c'"
                
                """.trimIndent()

        // Valid parse
        Assertions.assertEquals(
            parser(pSingleQuoted).parse("ab\"c'"),
            parser(pDoubleQuoted).parse("ab\"c'")
        )

        // Invalid parse
        Assertions.assertEquals(
            parser(pSingleQuoted).parse(""),
            parser(pDoubleQuoted).parse("")
        )

        // The parsers are the same on the inside.
        Assertions.assertEquals(
            parser(pSingleQuoted).grammar,
            parser(pDoubleQuoted).grammar
        )
    }

    @Test
    fun testParserCreationNewWithStandardWhitespace() {
        val p = parser(
            "S = ('a' | 'b')*",
            ParserCreationOptions.newWithStandardWhitespace()
        )
        val tree = PT.create("S", "a", "b", "a", "b", "a")
        Assertions.assertEquals(tree, p.parse("a b      a\tb\na"))
    }

    @Test
    fun testUnhideOptionsNone() {
        val p = parser("S = 'a' <B> C <D> 'a'\nB = 'b'+\n<C> = 'c'\n<D> = 'd'")
        val opts = ParsingOptions.getDefault().withUnhide(UnhideOptions.NONE)
        val tree = PT.create("S", "a", "c", "a")
        Assertions.assertEquals(tree, p.parse("abcda", opts))
    }

    @Test
    fun testUnhideOptionsTags() {
        val p = parser("S = 'a' <B> C <D> 'a'\nB = 'b'+\n<C> = 'c'\n<D> = 'd'")
        val opts = ParsingOptions.getDefault().withUnhide(UnhideOptions.TAGS)
        val tree = PT.create("S", "a", PT.create("C", "c"), "a")
        Assertions.assertEquals(tree, p.parse("abcda", opts))
    }

    @Test
    fun testUnhideOptionsContent() {
        val p = parser("S = 'a' <B> C <D> 'a'\nB = 'b'+\n<C> = 'c'\n<D> = 'd'")
        val opts = ParsingOptions.getDefault().withUnhide(UnhideOptions.CONTENT)
        val tree = PT.create("S", "a", PT.create("B", "b"), "c", "d", "a")
        Assertions.assertEquals(tree, p.parse("abcda", opts))
    }

    @Test
    fun testUnhideOptionsAll() {
        val p = parser("S = 'a' <B> C <D> 'a'\nB = 'b'+\n<C> = 'c'\n<D> = 'd'")
        val opts = ParsingOptions.getDefault().withUnhide(UnhideOptions.ALL)
        val tree = PT.create("S", "a", PT.create("B", "b"), PT.create("C", "c"), PT.create("D", "d"), "a")
        Assertions.assertEquals(tree, p.parse("abcda", opts))
    }

    @Test
    fun testUnhideOptionsInOneCase() {
        val p = parser("S = 'a' <B> C <D> 'a'\nB = 'b'+\n<C> = 'c'\n<D> = 'd'")

        Assertions.assertEquals(
            PT.create("S", "a", "c", "a"),
            p.parse("abcda", ParsingOptions.getDefault().withUnhide(UnhideOptions.NONE))
        )

        Assertions.assertEquals(
            PT.create("S", "a", PT.create("C", "c"), "a"),
            p.parse("abcda", ParsingOptions.getDefault().withUnhide(UnhideOptions.TAGS))
        )

        Assertions.assertEquals(
            PT.create("S", "a", PT.create("B", "b"), "c", "d", "a"),
            p.parse("abcda", ParsingOptions.getDefault().withUnhide(UnhideOptions.CONTENT))
        )

        Assertions.assertEquals(
            PT.create(
                "S",
                "a",
                PT.create("B", "b"),
                PT.create("C", "c"),
                PT.create("D", "d"),
                "a"
            ),
            p.parse("abcda", ParsingOptions.getDefault().withUnhide(UnhideOptions.ALL))
        )
    }

    @Test
    fun testPartialParseOptionIgnoredOnSingleParse() {
        run {
            val p = parser("S = 'a'+")
            val opts = ParsingOptions.getDefault().withPartial(true)
            Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa", opts))
        }
        run {
            val p = parser("S = 'a'")
            val opts = ParsingOptions.getDefault().withPartial(true)
            Assertions.assertTrue(p.parse("aa", opts).isFailure())
        }
    }

    @Test
    fun testPartialParseOptionIfNotInGrammar() {
        run {
            val p = parser("S = 'a'")
            val opts = ParsingOptions.getDefault().withPartial(true)
            Assertions.assertEquals(List.of<ParseTree?>(PT.create("S", "a")), p.parses("aa", opts))
        }
    }

    @Test
    fun parserCreationWithExplicitStartProduction() {
        run {
            val opts = ParserCreationOptions.getDefault().withStartProduction(sym("B"))
            val p = parser("A = 'a'\nB = 'b'", opts)

            Assertions.assertEquals(p.startProduction, opts.getStartProduction())

            Assertions.assertTrue(p.parse("a", ParsingOptions.getDefault()).isFailure())
            Assertions.assertEquals(PT.create("B", "b"), p.parse("b", ParsingOptions.getDefault()))
        }
        run {
            // The production is not in the grammar => Fail
            val opts = ParserCreationOptions.getDefault().withStartProduction(sym("B"))
            Assertions.assertThrows<ParserCreationFailure?>(
                ParserCreationFailure::class.java,
                Executable { parser("A = 'a'", opts) })
        }
    }

    @Test
    fun parseWithExplicitStartProduction() {
        run {
            val p = parser("A = 'a'\nB = 'b'")
            val opts = ParsingOptions.getDefault().withStart(sym("B"))

            Assertions.assertTrue(p.parse("b", ParsingOptions.getDefault()).isFailure())
            Assertions.assertEquals(PT.create("B", "b"), p.parse("b", opts))
        }
        run {
            // The production is not in the grammar => Fail
            val opts = ParsingOptions.getDefault().withStart(sym("B"))
            val p = parser("A = 'a'")
            Assertions.assertThrows<ParserCreationFailure?>(
                ParserCreationFailure::class.java,
                Executable { p.parse("a", opts) })
        }
    }

    @Test
    fun parse() {
        val p = parser("S = 'A' | 'B' | S S")
        run {
            val res = p.parse("A", ParsingOptions.getDefault())
            Assertions.assertEquals(PT.create("S", "A"), res)
        }
        run {
            val res = p.parse("B", ParsingOptions.getDefault())
            Assertions.assertEquals(PT.create("S", "B"), res)
        }
        run {
            val res = p.parse("AB", ParsingOptions.getDefault())
            Assertions.assertEquals(
                PT.create("S", PT.create("S", "A"), PT.create("S", "B")),
                res
            )
        }
    }

    @Test
    fun parseCat() {
        run {
            val p = parser("S = 'A' 'B'")
            val res = p.parse("AB", ParsingOptions.getDefault())
            Assertions.assertEquals(PT.create("S", "A", "B"), res)
        }
        run {
            val p = parser("S = 'A' 'B' S | ε")
            Assertions.assertEquals(PT.create("S"), p.parse("", ParsingOptions.getDefault()))
            Assertions.assertEquals(
                PT.create("S", "A", "B", PT.create("S")),
                p.parse("AB", ParsingOptions.getDefault())
            )
        }
        run {
            val p = parser("S = 'a' 'a' 'a'")
            Assertions.assertTrue(p.parse("").isFailure())
            Assertions.assertTrue(p.parse("a").isFailure())
            Assertions.assertTrue(p.parse("aa").isFailure())
            Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa"))
            Assertions.assertTrue(p.parse("aaaa").isFailure())
        }
    }

    @Test
    fun parsePlus() {
        run {
            val p = parser("S = 'a'+")
            Assertions.assertTrue(p.parse("", ParsingOptions.getDefault()).isFailure())
            Assertions.assertEquals(PT.create("S", "a"), p.parse("a", ParsingOptions.getDefault()))
            Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa", ParsingOptions.getDefault()))
            Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa", ParsingOptions.getDefault()))
        }
        run {
            val p = parser("S = ('a' | 'b')+")
            Assertions.assertTrue(p.parse("", ParsingOptions.getDefault()).isFailure())
            Assertions.assertEquals(PT.create("S", "b"), p.parse("b", ParsingOptions.getDefault()))
            Assertions.assertEquals(PT.create("S", "a", "b", "a"), p.parse("aba", ParsingOptions.getDefault()))
        }
    }

    @Test
    fun parseStar() {
        run {
            val p = parser("S = 'a'*")
            Assertions.assertEquals(PT.create("S"), p.parse("", ParsingOptions.getDefault()))
            Assertions.assertEquals(PT.create("S", "a"), p.parse("a", ParsingOptions.getDefault()))
            Assertions.assertEquals(PT.create("S", "a", "a"), p.parse("aa", ParsingOptions.getDefault()))
            Assertions.assertEquals(PT.create("S", "a", "a", "a"), p.parse("aaa", ParsingOptions.getDefault()))
        }
        run {
            val p = parser("S = ('a' | 'b')*")
            Assertions.assertEquals(PT.create("S"), p.parse("", ParsingOptions.getDefault()))
            Assertions.assertEquals(PT.create("S", "b"), p.parse("b", ParsingOptions.getDefault()))
            Assertions.assertEquals(PT.create("S", "a", "b", "a"), p.parse("aba", ParsingOptions.getDefault()))
        }
    }

    @Test
    fun parseSimpleComplex() {
        run {
            val p = parser("S = ε | S")
            val forest = p.parses("").castToParsesSuccess()
            Assertions.assertEquals(
                List.of<ParseTree?>(
                    PT.create("S"), PT.create("S", PT.create("S")),
                    PT.create("S", PT.create("S", PT.create("S"))),
                    PT.create("S", PT.create("S", PT.create("S", PT.create("S"))))
                ),
                forest.stream().limit(4).toList()
            )
        }
    }

    @Test
    fun parseSimpleString() {
        run {
            val p = parser("S = 'AB'")
            val res = p.parse("AB", ParsingOptions.getDefault())
            Assertions.assertEquals(PT.create("S", "AB"), res)
        }
        run {
            val p = parser("S = 'a'")
            val res = p.parse("a", ParsingOptions.getDefault())
            Assertions.assertEquals(PT.create("S", "a"), res)
        }
    }

    @Test
    fun parsesWithChoice() {
        run {
            val p = parser("S = 'A' | 'B' | S S")
            val res = p.parses("ABA", ParsingOptions.getDefault())
            val possibleResults = HashSet<ParseTree?>(sabssPossibleResults())

            // Using Sets because the order of results is implementation-dependent when using alternation rules.
            Assertions.assertEquals(possibleResults, HashSet<ParseTree?>(res))
        }
    }

    @Test
    fun parsesWithChoiceEps() {
        run {
            val p = parser("S = ε | A | B | C\nA = C \nB = C \nC = ε")
            val possibleTrees = Set.of<ParseTree?>(
                PT.create("S"),
                PT.create("S", PT.create("C")),
                PT.create("S", PT.create("A", PT.create("C"))),
                PT.create("S", PT.create("B", PT.create("C")))
            )
            Assertions.assertEquals(possibleTrees, HashSet<ParseTree?>(p.parses("", ParsingOptions.getDefault())))
        }
        run {
            val grammar = """
                    S  = (r1 | r2 | r3)* | ε
                    r1 = 'a'
                    r2 = 'a'
                    r3 = 'a'
                    
                    """.trimIndent()
            val text = "aa"
            val p = parser(grammar)
            val ps = HashSet<ParseTree?>(p.parses(text, ParsingOptions.getDefault()))
            val possibleParses = HashSet<ParseTree?>(r1r2r3Results())
            Assertions.assertEquals(possibleParses, ps)
        }
    }

    @Test
    fun parsesWithOrderedChoice() {
        run {
            val p = parser("S = 'A' / 'B' / S S")
            val res = p.parses("ABA", ParsingOptions.getDefault())
            val possibleResults = sabssPossibleResults()
            Assertions.assertEquals(possibleResults, res)
        }

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
            val expect = List.of<ParseTree?>(
                PT.create("S", PT.create("A")),
                PT.create("S", PT.create("B")),
                PT.create("S", PT.create("C")),
                PT.create("S", PT.create("D")),
                PT.create("S", PT.create("E"))
            )
            Assertions.assertEquals(expect, p.parses("", ParsingOptions.getDefault()))
        }
        run {
            val p = parser("S = 'a' / ε / 'a'")
            val possibleTrees = List.of<ParseTree?>(PT.create("S", "a"))
            Assertions.assertEquals(possibleTrees, p.parses("a", ParsingOptions.getDefault()))
        }
        run {
            val p = parser("S = ε / 'a' / 'a' / ε")
            val possibleTrees = List.of<ParseTree?>(PT.create("S", "a"))
            Assertions.assertEquals(possibleTrees, p.parses("a", ParsingOptions.getDefault()))
        }
        run {
            val grammar = """
                    S = (r1 / r2 / r3)*
                    r1 = 'a'
                    r2 = 'a'
                    r3 = 'a'
                    """.trimIndent()
            val text = "a"
            val p = parser(grammar)
            val ps = p.parses(text, ParsingOptions.getDefault())
            val possibleParses = List.of<ParseTree?>(
                PT.create("S", PT.create("r1", "a")),
                PT.create("S", PT.create("r2", "a")),
                PT.create("S", PT.create("r3", "a"))
            )
            Assertions.assertEquals(possibleParses, ps)
        }
        run {
            val grammar = """
                    S = (r1 / r2)*
                    r1 = 'a'
                    r2 = 'a'
                    
                    """.trimIndent()
            val text = "aa"
            val p = parser(grammar)
            val ps = p.parses(text, ParsingOptions.getDefault())
            val possibleParses = List.of<ParseTree?>(
                PT.create("S", PT.create("r1", "a"), PT.create("r1", "a")),
                PT.create("S", PT.create("r2", "a"), PT.create("r1", "a")),
                PT.create("S", PT.create("r1", "a"), PT.create("r2", "a")),
                PT.create("S", PT.create("r2", "a"), PT.create("r2", "a"))
            )
            Assertions.assertEquals(possibleParses, ps)
        }
    }

    /**
     * There used to be a bug where the following made the parse fail.
     */
    @Test
    fun orderedChoiceAtSameIndex() {
        val p = parser(
            """
                        S = number ("+" | "-") S / number
                        number = [ "+" / "-" ] digits
                        <digits> = #'[0-9]'+
                        
                        """.trimIndent()
        )
        Assertions.assertEquals(
            List.of<ParseTree?>(PT.create("S", PT.create("number", "1", "2", "3"))),
            p.parses("123")
        )
    }

    private fun r1r2r3Results(): MutableList<ParseTree?> {
        return List.of<ParseTree?>(
            PT.create("S", PT.create("r1", "a"), PT.create("r1", "a")),
            PT.create("S", PT.create("r2", "a"), PT.create("r1", "a")),
            PT.create("S", PT.create("r1", "a"), PT.create("r2", "a")),
            PT.create("S", PT.create("r2", "a"), PT.create("r2", "a")),
            PT.create("S", PT.create("r2", "a"), PT.create("r3", "a")),
            PT.create("S", PT.create("r1", "a"), PT.create("r3", "a")),
            PT.create("S", PT.create("r3", "a"), PT.create("r3", "a")),
            PT.create("S", PT.create("r3", "a"), PT.create("r2", "a")),
            PT.create("S", PT.create("r3", "a"), PT.create("r1", "a"))
        )
    }

    private fun sabssPossibleResults(): MutableList<ParseTree?> {
        return List.of<ParseTree>(
            PT.create(
                "S",
                PT.create("S", "A"),
                PT.create("S", PT.create("S", "B"), PT.create("S", "A"))
            ),
            PT.create(
                "S",
                PT.create("S", PT.create("S", "A"), PT.create("S", "B")),
                PT.create("S", "A")
            )
        )
    }

    @Test
    fun parsesPartial() {
        val opts = ParsingOptions(
            ParsingOptions.DEFAULT_START,
            true,
            ParsingOptions.DEFAULT_UNHIDE,
            ParsingOptions.DEFAULT_EMBED_FAILURES,
            ParsingOptions.DEFAULT_ITERATIVE_DEEPENING,
            ParsingOptions.DEFAULT_FAILURE_IF_EMPTY,
            ParsingOptions.DEFAULT_STOP_EARLY_ON_EMPTY_REPETITION,
            ParsingOptions.DEFAULT_RESULT_LIMIT
        )
        run {
            val grammar = """
                    S  = (r1 / r2 / r3)*
                    r1 = 'a'
                    r2 = 'a'
                    r3 = 'a'
                    
                    """.trimIndent()
            val text = "aa"
            val p = parser(grammar)
            val ps = p.parses(text, opts)
            val possibleParses = partialParsesOrderedR123()
            Assertions.assertEquals(possibleParses, ps)
        }
        run {
            val grammar = """
                    S  = (r1 | r2 | r3)*
                    r1 = 'a'
                    r2 = 'a'
                    r3 = 'a'
                    
                    """.trimIndent()
            val text = "aa"
            val p = parser(grammar)
            val ps = HashSet<ParseTree?>(p.parses(text, opts))
            val possibleParses = HashSet<ParseTree?>(partialParsesOrderedR123())
            Assertions.assertEquals(possibleParses, ps)
        }
    }

    private fun partialParsesOrderedR123(): MutableList<ParseTree?> {
        return List.of<ParseTree?>(
            PT.create("S"),
            PT.create("S", PT.create("r1", "a")),
            PT.create("S", PT.create("r1", "a"), PT.create("r1", "a")),
            PT.create("S", PT.create("r2", "a")),
            PT.create("S", PT.create("r2", "a"), PT.create("r1", "a")),
            PT.create("S", PT.create("r1", "a"), PT.create("r2", "a")),
            PT.create("S", PT.create("r3", "a")),
            PT.create("S", PT.create("r2", "a"), PT.create("r2", "a")),
            PT.create("S", PT.create("r2", "a"), PT.create("r3", "a")),
            PT.create("S", PT.create("r1", "a"), PT.create("r3", "a")),
            PT.create("S", PT.create("r3", "a"), PT.create("r3", "a")),
            PT.create("S", PT.create("r3", "a"), PT.create("r2", "a")),
            PT.create("S", PT.create("r3", "a"), PT.create("r1", "a"))
        )
    }

    @Test
    fun parserWithStart() {
        val p = parser("S1 = 'A'\nS2 = 'B'")
        Assertions.assertEquals(PT.create("S1", "A"), p.parse("A"))
        Assertions.assertTrue(p.parse("B").isFailure())

        val parserWithOtherStart = parser(
            "S1 = 'A'\nS2 = 'B'",
            ParserCreationOptions.getDefault().withStartProduction(sym("S2"))
        )
        Assertions.assertTrue(parserWithOtherStart.parse("A").isFailure())
        Assertions.assertEquals(PT.create("S2", "B"), parserWithOtherStart.parse("B"))
    }

    @Test
    fun parseWithStart() {
        val p = parser("S1 = 'A'\nS2 = 'B'")
        Assertions.assertEquals(PT.create("S1", "A"), p.parse("A"))
        Assertions.assertTrue(p.parse("B").isFailure())

        val opts = ParsingOptions.getDefault().withStart(sym("S2"))
        Assertions.assertTrue(p.parse("A", opts).isFailure())
        Assertions.assertEquals(PT.create("S2", "B"), p.parse("B", opts))
    }
}