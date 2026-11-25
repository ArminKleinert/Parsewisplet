package de.kleinert.parsewisp.parser

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.collections.FlatResultSeq.Companion.make
import de.kleinert.parsewisp.error.ParserCreationFailure
import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.grammar.GrammarPrinter
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.parser_options.Unhide
import de.kleinert.parsewisp.parser_options.UnhideOptions
import de.kleinert.parsewisp.parsing.Gll
import de.kleinert.parsewisp.result.ParseFailure
import de.kleinert.parsewisp.result.ParseResult
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.result.ParsesResult
import de.kleinert.parsewisp.result.ParsesResult.LazyResultList

/**
 * A parser contains a grammar and the name of the first production to try.
 *
 * @param grammar         The grammar.
 * @param startProduction The first production to try.
 * @param printer         The printer for the grammar. Used in [.show].
 * @since 0.9.7
 */
data class Parser(@JvmField val grammar: Grammar, val startProduction: Sym, @JvmField val printer: GrammarPrinter) {
    private fun getStartProductionFromParserOrOptionsAndCheck(
        options: ParsingOptions
    ): Sym {
        var startProduction = options.start
        if (startProduction == null) startProduction = this.startProduction
        if (!this.grammar.containsKey(startProduction)) throw ParserCreationFailure("Start production not in grammar: " + startProduction)
        return startProduction
    }

    private fun unhideParser(unhide: UnhideOptions): Parser {
        return when (unhide) {
            UnhideOptions.NONE -> this
            UnhideOptions.CONTENT -> withGrammar(Unhide.unhideContent(this.grammar))
            UnhideOptions.TAGS -> withGrammar(Unhide.unhideTags(this.grammar))
            UnhideOptions.ALL -> withGrammar(Unhide.unhideAll(this.grammar))
        }
    }

    /**
     * Runs a parser on a text. If the parse is successful, returns a [ParseTree]. If the parse fails, returns a [ParseFailure]
     *
     *
     * The options apply as follows:
     *
     *  * [ParsingOptions.embedFailureInParseTree]: Return a [ParseTree] on failure, with the information included in the tree.
     *  * [ParsingOptions.unhide]: Unhide some parts of the parser in the output.
     *  * [ParsingOptions.start]: Explicitly changes the start production.
     *
     *
     * @param text    The text.
     * @param options Options.
     * @return [ParseTree] if successful, [ParseFailure] if not.
     * @since 0.9.7
     */
    /**
     * Same as [parse] using [ParsingOptions.default] as the options.
     *
     * @param text The text.
     * @return The result of the parse.
     * @see parse
     * @see ParsingOptions.default
     * @since 0.9.7
     */
    @JvmOverloads
    fun parse(
        text: String,
        options: ParsingOptions = ParsingOptions.default
    ): ParseResult {
        val startProduction = getStartProductionFromParserOrOptionsAndCheck(options)
        val unhiddenParser = unhideParser(options.unhide)

        val parsingResult: ParseResult
        if (options.embedFailureInParseTree) {
            parsingResult = Gll.Companion.parseEmbedFailure(
                unhiddenParser.grammar, startProduction, text, false,
                options.iterativeDeepening, printer
            )
        } else {
            parsingResult = Gll.Companion.parse(
                unhiddenParser.grammar, startProduction, text, false,
                options.iterativeDeepening, printer
            )
        }

        return ParseResult.Companion.make(parsingResult)
    }

    /**
     * Runs a parser on a string and returns a parse forest as a [LazyResultList].
     *
     *
     * The following options apply:
     *
     *  * [ParsingOptions.usePartial]: Include partial parses.
     *  * [ParsingOptions.embedFailureInParseTree]: Include failure information in parse trees.
     *  * [ParsingOptions.unhide]: Unhide some parts of the parser in the output.
     *  * [ParsingOptions.start]: Explicitly changes the start production.
     *
     *
     * @param text    The text.
     * @param options The options for the parse operation.
     * @return A (potentially empty) parse forest. ([LazyResultList])
     * @see parses
     * @since 0.9.7
     */
    /**
     * Same as [parses] using [ParsingOptions.default] as the options.
     *
     * @param text The text.
     * @return The parse forest or error, as needed.
     * @see parses
     * @since 0.9.7
     */
    @JvmOverloads
    fun parses(
        text: String,
        options: ParsingOptions = ParsingOptions.default
    ): ParsesResult {
        val startProduction = getStartProductionFromParserOrOptionsAndCheck(options)
        val unhiddenParser = unhideParser(options.unhide)
        val usePartial = options.usePartial

        val embedFailure = options.embedFailureInParseTree
        if (embedFailure) {
            return Gll.Companion.parsesEmbedFailure(
                unhiddenParser.grammar, startProduction, text,
                usePartial, options.iterativeDeepening, printer,
                options.stopEarlyOnEmptyRepetition, options.resultLimit
            )
        } else {
            return Gll.Companion.parses(
                unhiddenParser.grammar, startProduction, text,
                usePartial, options.iterativeDeepening,
                options.failureIfEmpty, printer,
                options.stopEarlyOnEmptyRepetition, options.resultLimit
            )
        }
    }

    /**
     * Creates a new Parser with the grammar changed. This method may fail if the input is invalid.
     *
     * @param grammar the new grammar.
     * @return A new Parser.
     * @throws IllegalArgumentException if the new grammar does not include a production for the current start production.
     * @since 0.9.7
     */
    fun withGrammar(grammar: Grammar): Parser {
        if (this.grammar == grammar) return this
        return Parser(grammar, startProduction, printer)
    }

    /**
     * Creates a string representing this parser.
     *
     * @return The string.
     * @since 0.9.7
     */
    fun show(): String {
        return printer.toString(this.grammar)
    }

    /**
     * Creates a new Parser.
     *
     * @param grammar         The grammar.
     * @param startProduction The first production to try.
     * @throws ParserCreationFailure if the parameters are invalid (for example, if the grammar does not contain the start-production symbol).
     * @since 0.9.7
     */
    init {
        if (!grammar.containsKey(startProduction)) throw ParserCreationFailure("Illegal start-production " + startProduction + ": not in grammar.")
    }
}
