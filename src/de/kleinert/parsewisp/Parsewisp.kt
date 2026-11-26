package de.kleinert.parsewisp

import de.kleinert.parsewisp.error.IllegalGrammarException
import de.kleinert.parsewisp.error.ParserCreationFailure
import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.grammar.GrammarBuilder
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions

/**
 * The main interface for interacting with the library.
 *
 * @since 0.9.7
 */
object Parsewisp {
    /**
     * Creates a parser from a grammar specification. For documentation of the options, see [ParserCreationOptions].
     *
     * @param grammar The grammar as a string.
     * @param options The options.
     * @return The parser.
     * @since 0.9.7
     */
    fun parser(
        grammar: String,
        options: ParserCreationOptions = ParserCreationOptions.default
    ): Parser {
        val grammarForParsingGrammars = CfgGrammar.makeCfg()
        val g = Cfg.make(options).buildGrammar(grammar, grammarForParsingGrammars)
        return parser(g, options)
    }

    /**
     * Creates a parser from a grammar. See [.parser] for what the options do.
     *
     * @param grammar The grammar.
     * @param options The options, most importantly the start production.
     * @return The parser.
     * @throws ParserCreationFailure If the start production is invalid.
     * @since 0.9.7
     */
    fun parser(
        grammar: Grammar,
        options: ParserCreationOptions? = null
    ): Parser {
        var options = options
        if (options == null) options = ParserCreationOptions.default

        if (options.startProduction != null && !grammar.containsKey(options.startProduction)) throw ParserCreationFailure(
            "The start production " + options.startProduction + " is not in the grammar."
        )

        try {
            val g: Grammar
            if (options.startProduction == null && !options.doCheckCorrectness() && options.whitespaceParser == null) {
                g = grammar
            } else {
                val builder: GrammarBuilder = object : GrammarBuilder() {
                    override fun make() {
                        addAllProductions(grammar.entries)
                    }
                }

                g = builder.buildWithWhitespace(
                    options.startProduction,
                    options.whitespaceParser,
                    options.doCheckCorrectness()
                )
            }

            return Parser(g, g.startSym, options.printer)
        } catch (exception: IllegalGrammarException) {
            throw ParserCreationFailure(exception)
        }
    }

    /**
     * Get a whitespace parser (for use in [ParserCreationOptions.withWhitespaceParser]). The parser is accessed by a keyword.
     *
     *
     * The defined names are the keywords `:standard` (ignores spaces, tabs and newlines) and `:comma` (which also ignores commas).
     *
     * @param wsParserName The key.
     * @return A parser or null.
     * @since 0.9.7
     */
    fun getPredefinedWhitespaceParser(
        wsParserName: String?
    ): Parser? {
        if (wsParserName == null) {
            return null
        }
        if (predefinedWsParsers == null) {
            predefinedWsParsers = mapOf(
                "standard" to Parsewisp.parser("whitespace = #'\\s+'", ParserCreationOptions.default),
                "comma" to Parsewisp.parser("whitespace = #'[,\\s]+'", ParserCreationOptions.default)
            )
        }
        return predefinedWsParsers!![wsParserName]
    }

    private var predefinedWsParsers: Map<String, Parser>? = null
}
