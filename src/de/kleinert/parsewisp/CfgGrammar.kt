package de.kleinert.parsewisp

import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.grammar.GrammarBuilder
import de.kleinert.parsewisp.parser_options.RedefinitionOption
import de.kleinert.parsewisp.parsing.EOFTerm
import java.util.Optional
import java.util.regex.Pattern

/**
 * A builder for grammars with Parsewisp's default format.
 *
 * @since 0.9.7
 */
class CfgGrammar internal constructor() : GrammarBuilder(RedefinitionOption.ERROR) {
    override fun make() {
        val cWsp = hide(nt("cWsp"))
        val ntNt = nt("nt")
        val factorNt = nt("factor")
        val altNt = nt("alt")

        addProduction(
            "rules", catUnsafe(
                listOf(
                    cWsp, onceOrMore(catUnsafe(listOf(nt("rule"), cWsp)))
                )
            )
        )

        addProduction(
            "rule", catUnsafe(
                listOf(
                    altUnsafe(listOf(ntNt, nt("hide-nt"))),
                    cWsp,
                    altUnsafe(
                        listOf(
                            stringCS(":="), stringCS("::="), stringCS("=/"),
                            stringCS("="), stringCS(":")
                        )
                    ).hideInResult(),
                    cWsp,
                    altNt,
                    opt(altUnsafe(listOf(string(";"), string("."))))
                )
            )
        )

        addProduction(
            "alt", catUnsafe(
                listOf(
                    nt("ord"),
                    cWsp,
                    zeroOrMore(
                        catUnsafe(
                            listOf(
                                hide(string("|")),
                                cWsp,
                                nt("ord")
                            )
                        )
                    )
                )
            )
        )

        addProduction(
            "ord", catUnsafe(
                listOf(
                    nt("cat"),
                    cWsp,
                    zeroOrMore(
                        catUnsafe(
                            listOf(
                                hide(string("/")),
                                cWsp,
                                nt("cat")
                            )
                        )
                    )
                )
            )
        )

        addProduction(
            "cat", catUnsafe(
                listOf(
                    nt("term"),
                    zeroOrMore(catUnsafe(listOf(cWsp, nt("term"))))
                )
            )
        )

        addProduction(
            "term", altUnsafe(
                listOf(
                    nt("exclude"), nt("look"), nt("neg")
                )
            ).mergeToTop()
        )

        addProduction(
            "exclude", catUnsafe(
                listOf(
                    factorNt, cWsp, opt(catUnsafe(listOf(string("-").hideInResult(), cWsp, factorNt)))
                )
            )
        )

        addProduction("look", catUnsafe(listOf(string("&").hideInResult(), cWsp, factorNt)))
        addProduction("neg", catUnsafe(listOf(string("!").hideInResult(), cWsp, factorNt)))

        addProduction(
            "factor", catUnsafe(
                listOf(
                    nt("primary"),
                    zeroOrMore(
                        catUnsafe(
                            listOf(
                                cWsp,
                                altUnsafe(
                                    listOf(
                                        string("?"), string("*"), string("+"),
                                        regex(Pattern.compile("\\{\\d*(,\\d*)?}(?x) # Numbered repetition"))
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        addProduction(
            "primary", altUnsafe(
                listOf(
                    nt("string"), nt("regexp"), nt("opt"),
                    nt("opt_rep"), nt("paren"), nt("hide"),  //nt("epsilon"),
                    nt("abnf-range"),  //nt("eof"),
                    nt("ntOrEpsOrEof")
                )
            ).mergeToTop()
        )

        addProduction(
            "string", altUnsafe(
                listOf(
                    regex(Pattern.compile("(%[is])?\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"(?x) # Double quoted string")),
                    regex(Pattern.compile("(%[is])?'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'(?x) # Single quoted string"))
                )
            )
        )

        addProduction(
            "regexp", altUnsafe(
                listOf(
                    regex(Pattern.compile("#'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'(?x) # Single quoted string")),
                    regex(Pattern.compile("#\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"(?x) # Double quoted regex"))
                )
            )
        )

        addProduction(
            "opt", catUnsafe(
                listOf(
                    string("[").hideInResult(), cWsp, altNt, cWsp, string("]").hideInResult()
                )
            )
        )

        addProduction(
            "opt_rep", catUnsafe(
                listOf(
                    string("{").hideInResult(), cWsp, altNt, cWsp, string("}").hideInResult()
                )
            )
        )

        addProduction(
            "paren", catUnsafe(
                listOf(
                    string("(").hideInResult(), cWsp, altNt, cWsp, string(")").hideInResult()
                )
            )
        )

        addProduction(
            "hide", catUnsafe(
                listOf(
                    string("<").hideInResult(), cWsp, altNt, cWsp, string(">").hideInResult()
                )
            )
        )

        addProduction(
            "abnf-range", regex(
                Pattern.compile(
                    "%b[01]+(-[01]+)?|%d[0-9]+(-[0-9]+)?|%x[0-9a-fA-F]+(-[0-9a-fA-F]+)?(?x) # ABNF range"
                )
            )
        )

        val ntRegex = Pattern.compile(
            "[^, \\r\\t\\n<>(){}\\[\\]+*?:=|'\"#&!;./%\\-0-9][^, \\r\\t\\n<>(){}\\[\\]+*?:=|'\"#&!;./%]*"
        )
        addProduction("ntOrEpsOrEof", regex(ntRegex))

        val epsTerms = setOf<String?>("Epsilon", "epsilon", "EPSILON", "eps", "ε")
        addProduction(
            "nt", specialSequence(
                "Valid nonterminal name, excluding any variant of epsilon or EOF"
            ) { text: String ->
                val matcher = ntRegex.matcher(text)
                if (!matcher.lookingAt()) {
                    return@specialSequence Optional.empty<String?>()
                }
                val matched = matcher.group()
                if (epsTerms.contains(matched) || EOFTerm.text() == matched) {
                    return@specialSequence Optional.empty<String?>()
                }
                Optional.of<String?>(matched)
            }
        )

        addProduction(
            "hide-nt", catUnsafe(
                listOf(
                    string("<").hideInResult(),
                    cWsp,
                    ntNt,
                    cWsp,
                    string(">").hideInResult()
                )
            )
        )

        addProduction(
            "cWsp", hide(
                onceOrMore(
                    altUnsafe(
                        listOf(
                            nt("comment"),
                            regex(Pattern.compile("[,\\s]*"))
                        )
                    )
                )
            )
        )

        addProduction(
            "comment", catUnsafe(
                listOf(
                    string("(*"),
                    onceOrMore(
                        altUnsafe(
                            listOf(
                                regex("(?s)(?:(?!\\(\\*|\\*\\)).)*(?x) # Comment text"),
                                nt("comment")
                            )
                        )
                    ),
                    string("*)")
                )
            )
        )
    }

    companion object {
        /**
         * Returns the base grammar for Parsewisp.
         *
         * @return The base grammar for Parsewisp.
         * @since 0.9.7
         */
        fun makeCfg(): Grammar {
            return CfgGrammar().build()
        }
    }
}