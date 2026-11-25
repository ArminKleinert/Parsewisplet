package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.getPredefinedWhitespaceParser
import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.parser_options.UnhideOptions
import de.kleinert.parsewisp.result.ParseFailureNode
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.List
import java.util.Set

internal class ParsewispCoreTest {
    @Test
    fun testAsAndBs() {
        val as_and_bs = parser(
            """
                        S = AB*
                        AB = A B
                        A = 'a'+
                        B = 'b'+
                        
                        """.trimIndent()
        )
        val text = "aaaaabbbaaaabb"
        val tree = PT.create(
            "S",
            PT.create("AB", PT.create("A", "a", "a", "a", "a", "a"), PT.create("B", "b", "b", "b")),
            PT.create("AB", PT.create("A", "a", "a", "a", "a"), PT.create("B", "b", "b"))
        )

        Assertions.assertEquals(
            tree,
            as_and_bs.parse(text, ParsingOptions.default)
        )

        Assertions.assertEquals(
            tree,
            as_and_bs.parse(text)
        )
    }

    @Test
    fun testAsAndBsRegex() {
        val as_and_bs_regex = parser(
            """
                        S = AB*
                        AB = A B
                        A = #'a'+
                        B = #'b'+
                        
                        """.trimIndent()
        )
        val text = "aaaaabbbaaaabb"
        val tree = PT.create(
            "S",
            PT.create("AB", PT.create("A", "a", "a", "a", "a", "a"), PT.create("B", "b", "b", "b")),
            PT.create("AB", PT.create("A", "a", "a", "a", "a"), PT.create("B", "b", "b"))
        )

        Assertions.assertEquals(
            tree,
            as_and_bs_regex.parse(text, ParsingOptions.default)
        )

        Assertions.assertEquals(
            tree,
            as_and_bs_regex.parse(text)
        )
    }

    @Test
    fun testAsAndBsHiccup() {
        val as_and_bs = parser(
            """
                        S = AB*
                        AB = A B
                        A = 'a'+
                        B = 'b'+
                        
                        """.trimIndent()
        )
        val text = "aaaaabbbaaaabb"
        val tree = PT.create(
            "S",
            PT.create("AB", PT.create("A", "a", "a", "a", "a", "a"), PT.create("B", "b", "b", "b")),
            PT.create("AB", PT.create("A", "a", "a", "a", "a"), PT.create("B", "b", "b"))
        )
        val abTag = sym("AB")
        val aTag = sym("A")
        val bTag = sym("B")
        val treeHiccup = listOf(
            sym("S"),
            List.of<Any?>(abTag, List.of<Any?>(aTag, "a", "a", "a", "a", "a"), List.of<Any?>(bTag, "b", "b", "b")),
            List.of<Any?>(abTag, List.of<Any?>(aTag, "a", "a", "a", "a"), List.of<Any?>(bTag, "b", "b"))
        )

        Assertions.assertEquals(
            treeHiccup,
            tree.toRawList()
        )

        Assertions.assertEquals(
            treeHiccup,
            as_and_bs.parse(text, ParsingOptions.default).castToParseSuccess().toRawList()
        )
    }

    @Test
    fun testAsAndBsVariation1() {
        val as_and_bs_variation1 = parser(
            """
                        S = AB*
                        AB = 'a'+ 'b'+
                        
                        """.trimIndent()
        )
        val text = "aaaaabbbaaaabb"

        val res = PT.create(
            "S",
            PT.create("AB", "a", "a", "a", "a", "a", "b", "b", "b"),
            PT.create("AB", "a", "a", "a", "a", "b", "b")
        )

        Assertions.assertEquals(
            res,
            as_and_bs_variation1.parse(text)
        )
    }

    @Test
    fun testAsAndBsVariation2() {
        val as_and_bs_variation2 = parser(
            """
                        S = ('a'+ 'b'+)*
                        
                        """.trimIndent()
        )
        val text = "aaaaabbbaaaabb"

        val res = PT.create(
            "S",
            "a", "a", "a", "a", "a", "b", "b", "b", "a", "a", "a", "a", "b", "b"
        )
        val resList = listOf<Any?>(
            sym("S"),
            "a", "a", "a", "a", "a", "b", "b", "b", "a", "a", "a", "a", "b", "b"
        )

        Assertions.assertEquals(
            res,
            as_and_bs_variation2.parse(text)
        )
        Assertions.assertEquals(
            resList,
            as_and_bs_variation2.parse(text).castToParseSuccess().toRawList()
        )
    }

    @Test
    fun testParenAb() {
        val paren_ab = parser(
            """
                        paren-wrapped = '(' seq-of-A-or-B ')'
                        seq-of-A-or-B = ('a' | 'b')*
                        
                        """.trimIndent()
        )
        val text = "(aba)"
        val tree = PT.create(
            "paren-wrapped",
            "(",
            PT.create("seq-of-A-or-B", "a", "b", "a"),
            ")"
        )

        Assertions.assertEquals(tree, paren_ab.parse(text, ParsingOptions.default))

        Assertions.assertEquals(paren_ab.parse(text, ParsingOptions.default), paren_ab.parse(text))
    }

    @Test
    fun testParenAbHideParens() {
        val paren_ab_hide_parens = parser(
            """
                        paren-wrapped = <'('> seq-of-A-or-B <')'>
                        seq-of-A-or-B = ('a' | 'b')*
                        
                        """.trimIndent()
        )
        val text = "(aba)"
        val tree = PT.create(
            "paren-wrapped",
            PT.create("seq-of-A-or-B", "a", "b", "a")
        )

        Assertions.assertEquals(tree, paren_ab_hide_parens.parse(text, ParsingOptions.default))
        Assertions.assertEquals(
            paren_ab_hide_parens.parse(text, ParsingOptions.default),
            paren_ab_hide_parens.parse(text)
        )
    }

    @Test
    fun testParenAbManuallyFlattened() {
        val paren_ab_manually_flattened = parser(
            """
                        paren-wrapped = <'('> ('a' | 'b')* <')'>
                        
                        """.trimIndent()
        )
        val text = "(aba)"
        val tree = PT.create("paren-wrapped", "a", "b", "a")

        Assertions.assertEquals(tree, paren_ab_manually_flattened.parse(text, ParsingOptions.default))
        Assertions.assertEquals(
            paren_ab_manually_flattened.parse(text, ParsingOptions.default),
            paren_ab_manually_flattened.parse(text)
        )
    }

    @Test
    fun testParenAbHideTag() {
        val paren_ab_hide_tag = parser(
            """
                        paren-wrapped = <'('> seq-of-A-or-B <')'>
                        <seq-of-A-or-B> = ('a' | 'b')*
                        
                        """.trimIndent()
        )
        val text = "(aba)"
        val tree = PT.create("paren-wrapped", "a", "b", "a")

        Assertions.assertEquals(tree, paren_ab_hide_tag.parse(text, ParsingOptions.default))
        Assertions.assertEquals(
            paren_ab_hide_tag.parse(text, ParsingOptions.default),
            paren_ab_hide_tag.parse(text)
        )
    }

    @Test
    fun testParenAbHideBothTags() {
        val paren_ab_hide_both_tags = parser(
            """
                        <paren-wrapped> = <'('> seq-of-A-or-B <')'>
                        <seq-of-A-or-B> = ('a' | 'b')*
                        
                        """.trimIndent()
        )
        val text = "(aba)"

        // Sadly, Parsewisp can not output untagged trees.
        // The expected result for Instaparse is as follows:
        //    (paren-ab-hide-both-tags "(aba)") ;=> ("a" "b" "a")
        val tree = PT.create(ParseTree.NULL_TAG.content.name(), "a", "b", "a")

        // That raw output can be achieved by manual conversion:
        Assertions.assertEquals(mutableListOf<String?>("a", "b", "a"), tree.toRawList())

        Assertions.assertEquals(tree, paren_ab_hide_both_tags.parse(text, ParsingOptions.default))
        Assertions.assertEquals(
            paren_ab_hide_both_tags.parse(text, ParsingOptions.default),
            paren_ab_hide_both_tags.parse(text)
        )
    }

    @Test
    fun testParserAsFunction() {
        val text = "aaaa"
        val grammar = "S = 'a' S / 'a' "
        val tree =
            PT.create("S", "a", PT.create("S", "a", PT.create("S", "a", PT.create("S", "a"))))

        Assertions.assertEquals(tree, parser(grammar).parse(text))
    }

    @Test
    fun testParserAsFunction2() {
        val text = "aaaa"
        val grammar = "S = S 'a' / ε"
        val tree =
            PT.create(
                "S",
                PT.create(
                    "S",
                    PT.create(
                        "S",
                        PT.create(
                            "S",
                            PT.create("S"), "a"
                        ), "a"
                    ), "a"
                ), "a"
            )

        Assertions.assertEquals(tree, parser(grammar).parse(text))
    }

    @Test
    fun testAmbiguousParses() {
        val ambiguous = parser(
            """
                        S = A A
                        A = 'a'*
                        
                        """.trimIndent()
        )
        val text = "aaaaaa"

        val treesAmbiguous = listOf<ParseTree?>(
            PT.create("S", PT.create("A", "a"), PT.create("A", "a", "a", "a", "a", "a")),
            PT.create("S", PT.create("A", "a", "a", "a", "a", "a", "a"), PT.create("A")),
            PT.create("S", PT.create("A", "a", "a"), PT.create("A", "a", "a", "a", "a")),
            PT.create("S", PT.create("A", "a", "a", "a"), PT.create("A", "a", "a", "a")),
            PT.create("S", PT.create("A", "a", "a", "a", "a"), PT.create("A", "a", "a")),
            PT.create("S", PT.create("A", "a", "a", "a", "a", "a"), PT.create("A", "a")),
            PT.create("S", PT.create("A"), PT.create("A", "a", "a", "a", "a", "a", "a"))
        )

        Assertions.assertEquals(treesAmbiguous, ambiguous.parses(text))
        Assertions.assertEquals(treesAmbiguous, ambiguous.parses(text, ParsingOptions.default))
    }

    @Test
    fun testUnambiguousParses() {
        val not_ambiguous = parser(
            """
                        S = A A
                        A = #'a*'
                        
                        """.trimIndent()
        )
        val text = "aaaaaa"

        val treesUnambiguous = listOf<ParseTree?>(
            PT.create("S", PT.create("A", text), PT.create("A", ""))
        )

        Assertions.assertEquals(treesUnambiguous, not_ambiguous.parses(text))
        Assertions.assertEquals(treesUnambiguous, not_ambiguous.parses(text, ParsingOptions.default))
    }

    @Test
    fun testLookaheadExample() {
        val lookahead_example = parser(
            """
                        S = &'ab' ('a' | 'b')+
                        
                        """.trimIndent()
        )
        val text = "abaaaab"
        val tree = PT.create("S", "a", "b", "a", "a", "a", "a", "b")
        Assertions.assertEquals(tree, lookahead_example.parse(text))
        Assertions.assertEquals(tree, lookahead_example.parse(text, ParsingOptions.default))
    }

    @Test
    fun testLookaheadExampleFailure() {
        val lookahead_example = parser(
            """
                        S = &'ab' ('a' | 'b')+
                        
                        """.trimIndent()
        )
        val text = "bbaaaab"

        Assertions.assertFalse(lookahead_example.parse(text).isSuccess)
        Assertions.assertTrue(lookahead_example.parse(text).isFailure)
    }

    @Test
    fun testNegativeLookaheadExample() {
        val negative_lookahead_example = parser(
            """
                        S = !'ab' ('a' | 'b')+
                        
                        """.trimIndent()
        )
        val text = "bbaaaab"
        val tree = PT.create("S", "b", "b", "a", "a", "a", "a", "b")

        Assertions.assertEquals(tree, negative_lookahead_example.parse(text))
    }

    @Test
    fun testNegativeLookaheadExampleFailure() {
        val negative_lookahead_example = parser(
            """
                        S = !'ab' ('a' | 'b')+
                        
                        """.trimIndent()
        )
        val text = "abaaaab"

        Assertions.assertFalse(negative_lookahead_example.parse(text).isSuccess)
        Assertions.assertTrue(negative_lookahead_example.parse(text).isFailure)
    }

    @Test
    fun testCharRangeExampleFailure() {
        val p = parser(
            """
                Regex = (CharNonRange | Range) +
                Range = Char <'-'> Char
                CharNonRange = Char ! ('-' Char)
                Char = #'[-x]' | 'c' (! 'd') 'x'
                
                """.trimIndent()
        )

        val tree = PT.create(
            "Regex",
            PT.create(
                "Range",
                PT.create("Char", "x"),
                PT.create("Char", "c", "x")
            )
        )

        Assertions.assertEquals(
            tree,
            p.parse("x-cx")
        )
        Assertions.assertEquals(
            List.of<ParseTree?>(tree),
            p.parses("x-cx")
        )
    }

    @Test
    fun testAmbiguousTokenizer() {
        val ambiguous_tokenizer = parser(
            """
                        sentence = token (<whitespace> token)*
                        <token> = keyword | identifier
                        whitespace = #'\s+'
                        identifier = #'[a-zA-Z]+'
                        keyword = 'cond' | 'defn'
                        
                        """.trimIndent()
        )
        val text = "defn my cond"
        val trees = setOf<ParseTree>(
            PT.create(
                "sentence",
                PT.create("identifier", "defn"),
                PT.create("identifier", "my"),
                PT.create("identifier", "cond")
            ),
            PT.create(
                "sentence",
                PT.create("keyword", "defn"),
                PT.create("identifier", "my"),
                PT.create("identifier", "cond")
            ),
            PT.create(
                "sentence",
                PT.create("identifier", "defn"),
                PT.create("identifier", "my"),
                PT.create("keyword", "cond")
            ),
            PT.create(
                "sentence",
                PT.create("keyword", "defn"),
                PT.create("identifier", "my"),
                PT.create("keyword", "cond")
            )
        )
        val parses =
            setOf(*ambiguous_tokenizer.parses(text, ParsingOptions.default).toTypedArray())
        Assertions.assertEquals(trees, parses)
    }

    @Test
    fun testUnambiguousTokenizer() {
        val unambiguous_tokenizer = parser(
            """
                        sentence = token (<whitespace> token)*
                        <token> = keyword | !keyword identifier
                        whitespace = #'\s+'
                        identifier = #'[a-zA-Z]+'
                        keyword = 'cond' | 'defn'
                        
                        """.trimIndent()
        )
        val text = "defn my cond"
        val trees = listOf(
            PT.create(
                "sentence",
                PT.create("keyword", "defn"),
                PT.create("identifier", "my"),
                PT.create("keyword", "cond")
            )
        )

        Assertions.assertEquals(trees, unambiguous_tokenizer.parses(text, ParsingOptions.default))
    }

    @Test
    fun testPreferentialTokenizer() {
        val preferential_tokenizer = parser(
            """
                        sentence = token (<whitespace> token)*
                        <token> = keyword / identifier
                        whitespace = #'\s+'
                        identifier = #'[a-zA-Z]+'
                        keyword = 'cond' | 'defn'
                        
                        """.trimIndent()
        )
        val text = "defn my cond"
        val trees = mutableSetOf(
            PT.create(
                "sentence",
                PT.create("keyword", "defn"),
                PT.create("identifier", "my"),
                PT.create("keyword", "cond")
            ),

            PT.create(
                "sentence",
                PT.create("identifier", "defn"),
                PT.create("identifier", "my"),
                PT.create("keyword", "cond")
            ),

            PT.create(
                "sentence",
                PT.create("keyword", "defn"),
                PT.create("identifier", "my"),
                PT.create("identifier", "cond")
            ),

            PT.create(
                "sentence",
                PT.create("identifier", "defn"),
                PT.create("identifier", "my"),
                PT.create("identifier", "cond")
            )
        )
        val parses =
            setOf(*preferential_tokenizer.parses(text, ParsingOptions.default).toTypedArray())
        Assertions.assertEquals(trees, parses)
    }

    @Test
    fun testRepeatedA() {
        val repeated_a = parser(
            """
                S = 'a'+
                
                """.trimIndent()
        )

        val text = "aaaaaa"
        val trees = listOf<ParseTree?>(
            PT.create("S", "a", "a", "a", "a", "a", "a")
        )

        Assertions.assertEquals(trees, repeated_a.parses(text, ParsingOptions.default))

        val partialOpts = ParsingOptions.default.withPartial(true)
        val treesPartial = listOf<ParseTree?>(
            PT.create("S", "a"),
            PT.create("S", "a", "a"),
            PT.create("S", "a", "a", "a"),
            PT.create("S", "a", "a", "a", "a"),
            PT.create("S", "a", "a", "a", "a", "a"),
            PT.create("S", "a", "a", "a", "a", "a", "a")
        )

        Assertions.assertEquals(treesPartial, repeated_a.parses(text, partialOpts))
    }

    @Test
    fun testWordsAndNumbersOneCharacterAtATime() {
        val words_and_numbers_one_character_at_a_time = parser(
            """
                        sentence = token (<whitespace> token)*
                        <token> = word | number
                        whitespace = #'\s+'
                        word = letter+
                        number = digit+
                        <letter> = #'[a-zA-Z]'
                        <digit> = #'[0-9]'
                        
                        """.trimIndent()
        )
        val text = "abc 123 def"
        val tree = PT.create(
            "sentence",
            PT.create("word", "a", "b", "c"),
            PT.create("number", "1", "2", "3"),
            PT.create("word", "d", "e", "f")
        )

        Assertions.assertEquals(
            tree,
            words_and_numbers_one_character_at_a_time.parse(text, ParsingOptions.default)
        )
    }

    @Test
    fun testArithmeticGrammar() {
        val arithmetic = parser(
            """
                        expr = add-sub
                        <add-sub> = mul-div | add | sub
                        add = add-sub <'+'> mul-div
                        sub = add-sub <'-'> mul-div
                        <mul-div> = term | mul | div
                        mul = mul-div <'*'> term
                        div = mul-div <'/'> term
                        <term> = number | <'('> add-sub <')'>
                        number = #'[0-9]+'
                        
                        """.trimIndent()
        )
        val text = "1-2/(3-4)+5*6"
        val tree = PT.create(
            "expr",
            PT.create(
                "add",
                PT.create(
                    "sub",
                    PT.create("number", "1"),
                    PT.create(
                        "div",
                        PT.create("number", "2"),
                        PT.create(
                            "sub",
                            PT.create("number", "3"),
                            PT.create("number", "4")
                        )
                    )
                ),
                PT.create(
                    "mul",
                    PT.create("number", "5"),
                    PT.create("number", "6")
                )
            )
        )

        Assertions.assertEquals(tree, arithmetic.parse(text, ParsingOptions.default))
    }

    @Test
    fun testTrickyEbnfBuild() {
        val tricky_ebnf_build = parser(
            """
                        S = A | B
                        <A> = '='*
                        B = 'b' '='
                        
                        """.trimIndent(),
            ParserCreationOptions.default.withStartProduction(sym("S"))
        )
        val text1 = "==="
        val text2 = "b="
        val tree1 = PT.create("S", "=", "=", "=")
        val tree2 = PT.create("S", PT.create("B", "b", "="))

        Assertions.assertEquals(tree1, tricky_ebnf_build.parse(text1, ParsingOptions.default))
        Assertions.assertEquals(tree2, tricky_ebnf_build.parse(text2, ParsingOptions.default))
    }

    @Test
    fun testFail() {
        Assertions.assertEquals(
            PT.create("S", "a"),
            parser("S = A\n<A> = 'a'").parse("a")
        )
        Assertions.assertEquals(
            PT.create("S", PT.create("A")),
            parser("S = A\nA = <'a'>").parse("a")
        )
    }

    @Test
    fun testOptionalRepeat() {
        val parser = parser("S = ('a'?)+")
        val tree = PT.create("S")

        Assertions.assertEquals(tree, parser.parse(""))
    }

    @Test
    fun testDot() {
        val parser = parser(
            """
                a = b c .
                b = 'b' .
                c = 'c' .
                
                """.trimIndent()
        )
        val tree = PT.create(
            "a",
            PT.create("b", "b"),
            PT.create("c", "c")
        )

        Assertions.assertEquals(tree, parser.parse("bc"))
    }

    @Test
    fun testUnhide1() {
        val text = "(ababa)"
        val treeNormal = PT.create(
            "paren-wrapped",
            PT.create(
                "seq-of-A-or-B",
                "a", "b", "a", "b", "a"
            )
        )
        val treeWParen = PT.create(
            "paren-wrapped",
            "(",
            PT.create(
                "seq-of-A-or-B",
                "a", "b", "a", "b", "a"
            ),
            ")"
        )
        val p = parser(
            """
                        paren-wrapped = <'('> seq-of-A-or-B <')'>
                        seq-of-A-or-B = ('a' | 'b')*
                        
                        """.trimIndent()
        )

        Assertions.assertEquals(treeNormal, p.parse(text))
        Assertions.assertEquals(
            treeWParen,
            p.parse(text, ParsingOptions.default.withUnhide(UnhideOptions.CONTENT))
        )
        Assertions.assertEquals(
            treeWParen,
            p.parse(text, ParsingOptions.default.withUnhide(UnhideOptions.ALL))
        )
        Assertions.assertEquals(
            treeNormal,
            p.parse(text, ParsingOptions.default.withUnhide(UnhideOptions.TAGS))
        )
    }

    @Test
    fun testUnhide2() {
        val p = parser(
            """
                        paren-wrapped = <'('> seq-of-A-or-B <')'>
                        <seq-of-A-or-B> = ('a' | 'b')*
                        
                        """.trimIndent()
        )
        val text = "(ababa)"
        val treeWTag = PT.create(
            "paren-wrapped",
            PT.create(
                "seq-of-A-or-B",
                "a", "b", "a", "b", "a"
            )
        )
        val treeWAll = PT.create(
            "paren-wrapped",
            "(",
            PT.create(
                "seq-of-A-or-B",
                "a", "b", "a", "b", "a"
            ),
            ")"
        )

        Assertions.assertEquals(
            treeWTag,
            p.parse(text, ParsingOptions.default.withUnhide(UnhideOptions.TAGS))
        )

        Assertions.assertEquals(
            treeWAll,
            p.parse(text, ParsingOptions.default.withUnhide(UnhideOptions.ALL))
        )
    }

    @Test
    fun testEpsEquivalence() {
        val opts = ParserCreationOptions.default
        Assertions.assertEquals(
            PT.create("S"),
            parser("S = eps", opts).parse("")
        )
        Assertions.assertEquals(
            PT.create("S"),
            parser("S = epsilon", opts).parse("")
        )
        Assertions.assertEquals(
            PT.create("S"),
            parser("S = Epsilon", opts).parse("")
        )
        Assertions.assertEquals(
            PT.create("S"),
            parser("S = EPSILON", opts).parse("")
        )
        Assertions.assertEquals(
            PT.create("S"),
            parser("S = ε", opts).parse("")
        )
    }

    @Test
    fun testEpsFail() {
        val opts = ParserCreationOptions.default
        Assertions.assertTrue(
            parser("S = eps", opts)
                .parse("a")
                .isFailure
        )
        Assertions.assertTrue(
            parser("S = epsilon", opts)
                .parse("a")
                .isFailure
        )
        Assertions.assertTrue(
            parser("S = Epsilon", opts)
                .parse("a")
                .isFailure
        )
        Assertions.assertTrue(
            parser("S = EPSILON", opts)
                .parse("a")
                .isFailure
        )
        Assertions.assertTrue(
            parser("S = ε", opts)
                .parse("a")
                .isFailure
        )
    }

    @Test
    fun testWordsAndNumbers() {
        val words_and_numbers = parser(
            """
                        sentence = token (<whitespace> token)*
                        <token> = word | number
                        whitespace = #'\s+'
                        word = #'[a-zA-Z]+'
                        number = #'[0-9]+'
                        
                        """.trimIndent()
        )
        val text = "ab 123 cd"
        val treeWithoutTokenTag = PT.create(
            "sentence",
            PT.create("word", "ab"),
            PT.create("number", "123"),
            PT.create("word", "cd")
        )
        val treeFull = PT.create(
            "sentence",
            PT.create("token", PT.create("word", "ab")),
            PT.create("whitespace", " "),
            PT.create("token", PT.create("number", "123")),
            PT.create("whitespace", " "),
            PT.create("token", PT.create("word", "cd"))
        )
        Assertions.assertEquals(treeWithoutTokenTag, words_and_numbers.parse(text))

        Assertions.assertEquals(
            treeFull,
            words_and_numbers.parse(text, ParsingOptions.default.withUnhide(UnhideOptions.ALL))
        )
    }

    @Test
    fun testWordsAndNumbersAutoWhitespace() {
        val p = parser(
            """
                        sentence = token+
                        <token> = word | number
                        word = #'[a-zA-Z]+'
                        number = #'[0-9]+'
                        
                        """.trimIndent(),
            ParserCreationOptions.default.withWhitespaceParser(parser("whitespace = #'\\s+'"))
        )
        val p2 = parser(
            """
                        sentence = token+
                        <token> = word | number
                        word = #'[a-zA-Z]+'
                        number = #'[0-9]+'
                        
                        """.trimIndent(),
            ParserCreationOptions.default.withWhitespaceParser(
                getPredefinedWhitespaceParser("standard")
            )
        )
        val tree = PT.create(
            "sentence",
            PT.create("word", "ab"),
            PT.create("number", "123"),
            PT.create("word", "cd")
        )

        Assertions.assertEquals(tree, p.parse("ab 123 cd"))
        Assertions.assertEquals(tree, p.parse(" ab 123 cd "))

        Assertions.assertEquals(tree, p2.parse("ab 123 cd"))
        Assertions.assertEquals(tree, p2.parse(" ab 123 cd "))
    }

    @Test
    fun testWordsAndNumbersAutoWhitespaceAndComments() {
        val ws = parser(
            """
                        ws-or-comments = #'\s+' | comments
                        comments = comment+
                        comment = '(*' inside-comment* '*)'
                        inside-comment =  !'*)' !'(*' #'.' | comment
                        
                        """.trimIndent(),
            ParserCreationOptions.default.withWhitespaceParser(parser("whitespace = #'\\s+'"))
        )
        val words_and_numbers_auto_whitespace_and_comments = parser(
            """
                        sentence = token+
                        <token> = word | number
                        word = #'[a-zA-Z]+'
                        number = #'[0-9]+'
                        
                        """.trimIndent(),
            ParserCreationOptions.default.withWhitespaceParser(ws)
        )
        val tree = PT.create(
            "sentence",
            PT.create("word", "abc"),
            PT.create("number", "123"),
            PT.create("word", "def")
        )

        Assertions.assertEquals(
            tree,
            words_and_numbers_auto_whitespace_and_comments
                .parse(" abc 123  def ")
        )
        Assertions.assertEquals(
            tree,
            words_and_numbers_auto_whitespace_and_comments
                .parse(" abc 123 (* (*de*)f *) def")
        )
    }

    @Test
    fun testEatA() {
        val p = parser(
            "Aeater = #'[a]'+",
            ParserCreationOptions.default
        )
        val tree = PT.create(
            "Aeater",
            "a", "a", "a", "a", "a", "a", "a", "a",
            ParseFailureNode("bbbbbb", sym("failure"), 8, 14)
        )

        val text = "aaaaaaaabbbbbb"

        Assertions.assertEquals(tree, p.parse(text, ParsingOptions.default.withEmbedFailureInParseTree(true)))
    }

    @Test
    fun testIntOrDouble() {
        val int_or_double = parser(
            """
                        ws = #'\s+';
                        Int = #'[0-9]+';
                        Double = #'[0-9]+\.[0-9]*|\.[0-9]+';
                        <ConstExpr> = Int | Double;
                        Input = ConstExpr <ws> ConstExpr;
                        
                        """.trimIndent(),
            ParserCreationOptions.default.withStartProduction(sym("Input"))
        )
        val tree = PT.create(
            "Input",
            PT.create("Int", "31"),
            PT.create("Double", "0.2")
        )

        val text = "31 0.2"

        Assertions.assertEquals(tree, int_or_double.parse(text))
    }

    @Test
    fun testGreedyRegex() {
        run {
            val p = parser("S = #'\\s*'")
            val text = "     "
            val tree = PT.create("S", text)
            Assertions.assertEquals(tree, p.parse(text))
        }
        run {
            val p = parser("S = #'a+'")
            val text = "aaaaaa"
            val tree = PT.create("S", text)
            Assertions.assertEquals(tree, p.parse(text))
        }
    }

    @Test
    fun testSimpleOrderedChoice() {
        val p = parser("S = 'a' / ε")

        Assertions.assertEquals(PT.create("S"), p.parse(""))
        Assertions.assertEquals(PT.create("S", "a"), p.parse("a"))
    }

    @Test
    fun testStarRepetitionFailures() {
        val p = parser("S = 'a'*")

        Assertions.assertTrue(p.parse("AaaAaa").isFailure)
        Assertions.assertTrue(p.parse("").isSuccess)
    }

    @Test
    fun testStarRepetitionFailuresTotal() {
        val p = parser("S = 'a'*")
        val opts = ParsingOptions.default.withEmbedFailureInParseTree(true)

        Assertions.assertEquals(
            PT.create(
                "S",
                ParseFailureNode(
                    "AaaAaa",
                    sym("failure"),
                    0, 6
                )
            ),
            p.parse("AaaAaa", opts)
        )

        Assertions.assertTrue(p.parse("", opts).isSuccess)
    }

    @Test
    fun testPlusRepetitionFailures() {
        val p = parser("S = 'a'+")

        Assertions.assertTrue(p.parse("AaaAaa").isFailure)
        Assertions.assertTrue(p.parse("").isFailure)
    }

    @Test
    fun testPlusRepetitionFailuresTotal() {
        val p = parser("S = 'a'+")
        val opts = ParsingOptions.default.withEmbedFailureInParseTree(true)

        Assertions.assertEquals(
            PT.create(
                "S",
                ParseFailureNode(
                    "AaaAaa",
                    sym("failure"),
                    0, 6
                )
            ),
            p.parse("AaaAaa", opts)
        )

        Assertions.assertEquals(
            PT.create(
                "S",
                ParseFailureNode(
                    "",
                    sym("failure"),
                    0, 0
                )
            ),
            p.parse("", opts)
        )
    }

    @Test
    fun testStringVsStringCi() {
        val text = "AaaAaa"

        val pStandard1 = parser("S = %s'a'+")
        Assertions.assertTrue(pStandard1.parse(text).isFailure)

        val tree = PT.create("S", "a", "a", "a", "a", "a", "a")
        val pCaseInsensitive = parser("S = %i'a'+")
        Assertions.assertEquals(tree, pCaseInsensitive.parse(text))
    }

    @Test
    fun testAutoWhitespaceExamples() {
        val auto_whitespace_example = parser(
            """
                        S = A B
                        <A> = 'foo'
                        <B> = #'\d+'
                        
                        """.trimIndent(),
            ParserCreationOptions.default.withWhitespaceParser(parser("whitespace = #'\\s+'"))
        )
        val tree = PT.create("S", "foo", "123")
        val text = "foo 123"

        Assertions.assertEquals(tree, auto_whitespace_example.parse(text))
    }

    @Test
    fun testAutoWhitespaceExample2() {
        val auto_whitespace_example2 = parser(
            """
                        S = A B
                        <A> = 'foo'
                        <B> = #'\d+'
                        
                        """.trimIndent(),
            ParserCreationOptions.default.withWhitespaceParser(
                getPredefinedWhitespaceParser("standard")
            )
        )
        val tree = PT.create("S", "foo", "123")
        val text = "foo 123"

        Assertions.assertEquals(tree, auto_whitespace_example2.parse(text))
    }

    @Test
    fun testRegexFail() {
        Assertions.assertTrue(
            parser("f = #'asdf'").parse("asdf").isSuccess
        )
        Assertions.assertTrue(
            parser("f = #'asdf'").parse("").isFailure
        )
        Assertions.assertFalse(
            parser("f = #'asdf'").parse("").isSuccess
        )
    }

    @Test
    fun testCaseInsensitiveRegex() {
        val case_insensitive_regexp = parser(
            """
                        S = #'(?i)a+'
                        
                        """.trimIndent()
        )
        Assertions.assertEquals(
            PT.create("S", "aaa"),
            case_insensitive_regexp.parse("aaa")
        )
        Assertions.assertEquals(
            PT.create("S", "AAA"),
            case_insensitive_regexp.parse("AAA")
        )
        Assertions.assertEquals(
            PT.create("S", "aAa"),
            case_insensitive_regexp.parse("aAa")
        )
    }

    @Test
    fun testOptionFailureIfEmpty() {
        val opts = ParsingOptions.default.withFailureIfEmpty(true)
        val p = parser("S = 'a'")
        Assertions.assertTrue(p.parses("b").isEmpty())
        Assertions.assertFalse(p.parses("b", opts).isSuccess)
    }
}