package de.kleinert.parsewisp

import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.grammar.GrammarBuilder
import de.kleinert.parsewisp.parser_options.RedefinitionOption
import de.kleinert.parsewisp.parsing.EOFTerm
import de.kleinert.parsewisp.parsing.Rule
import java.util.*
import java.util.List
import java.util.function.Function
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
                    cWsp, onceOrMore(catUnsafe(List.of<Rule>(nt("rule"), cWsp)))
                )
            )
        )

        addProduction(
            "rule", catUnsafe(
                List.of<Rule?>(
                    altUnsafe(List.of<Rule>(ntNt, nt("hide-nt"))),
                    cWsp,
                    altUnsafe(
                        List.of<Rule?>(
                            stringCS(":="), stringCS("::="), stringCS("=/"),
                            stringCS("="), stringCS(":")
                        )
                    ).hideInResult(),
                    cWsp,
                    altNt,
                    opt(altUnsafe(List.of<Rule>(string(";"), string("."))))
                )
            )
        )

        addProduction(
            "alt", catUnsafe(
                List.of<Rule?>(
                    nt("ord"),
                    cWsp,
                    zeroOrMore(
                        catUnsafe(
                            List.of<Rule?>(
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
                List.of<Rule?>(
                    nt("cat"),
                    cWsp,
                    zeroOrMore(
                        catUnsafe(
                            List.of<Rule?>(
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
                List.of<Rule>(
                    nt("term"),
                    zeroOrMore(catUnsafe(List.of<Rule>(cWsp, nt("term"))))
                )
            )
        )

        addProduction(
            "term", altUnsafe(
                List.of<Rule?>(
                    nt("exclude"), nt("look"), nt("neg")
                )
            ).mergeToTop()
        )

        addProduction(
            "exclude", catUnsafe(
                List.of<Rule?>(
                    factorNt, cWsp, opt(catUnsafe(List.of<Rule?>(string("-").hideInResult(), cWsp, factorNt)))
                )
            )
        )

        addProduction("look", catUnsafe(List.of<Rule?>(string("&").hideInResult(), cWsp, factorNt)))
        addProduction("neg", catUnsafe(List.of<Rule?>(string("!").hideInResult(), cWsp, factorNt)))

        addProduction(
            "factor", catUnsafe(
                List.of<Rule>(
                    nt("primary"),
                    zeroOrMore(
                        catUnsafe(
                            List.of<Rule>(
                                cWsp,
                                altUnsafe(
                                    List.of<Rule?>(
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
                List.of<Rule?>(
                    nt("string"), nt("regexp"), nt("opt"),
                    nt("opt_rep"), nt("paren"), nt("hide"),  //nt("epsilon"),
                    nt("abnf-range"),  //nt("eof"),
                    nt("ntOrEpsOrEof")
                )
            ).mergeToTop()
        )

        addProduction(
            "string", altUnsafe(
                List.of<Rule>(
                    regex(Pattern.compile("(%[is])?\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"(?x) # Double quoted string")),
                    regex(Pattern.compile("(%[is])?'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'(?x) # Single quoted string"))
                )
            )
        )

        addProduction(
            "regexp", altUnsafe(
                List.of<Rule>(
                    regex(Pattern.compile("#'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'(?x) # Single quoted string")),
                    regex(Pattern.compile("#\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"(?x) # Double quoted regex"))
                )
            )
        )

        addProduction(
            "opt", catUnsafe(
                List.of<Rule?>(
                    string("[").hideInResult(), cWsp, altNt, cWsp, string("]").hideInResult()
                )
            )
        )

        addProduction(
            "opt_rep", catUnsafe(
                List.of<Rule?>(
                    string("{").hideInResult(), cWsp, altNt, cWsp, string("}").hideInResult()
                )
            )
        )

        addProduction(
            "paren", catUnsafe(
                List.of<Rule?>(
                    string("(").hideInResult(), cWsp, altNt, cWsp, string(")").hideInResult()
                )
            )
        )

        addProduction(
            "hide", catUnsafe(
                List.of<Rule?>(
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
                listOf<Rule?>(
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
                        List.of<Rule>(
                            nt("comment"),
                            regex(Pattern.compile("[,\\s]*"))
                        )
                    )
                )
            )
        )

        addProduction(
            "comment", catUnsafe(
                List.of<Rule?>(
                    string("(*"),
                    onceOrMore(
                        altUnsafe(
                            List.of<Rule>(
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
        @JvmStatic
        fun makeCfg(): Grammar {
            return CfgGrammar().build()
        }
    }
}