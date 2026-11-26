package de.kleinert.parsewisp.parser_options

import de.kleinert.parsewisp.Parsewisp.getPredefinedWhitespaceParser
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.grammar.GrammarPrinter
import de.kleinert.parsewisp.parser.Parser

/**
 * This class provides options for creating [Parser] instances.
 */
data class ParserCreationOptions(
    val whitespaceParser: Parser? = null,
    val startProduction: Sym? = null,
    val redefinitionOption: RedefinitionOption = RedefinitionOption.ERROR,
    val checkCorrectness: Boolean = true,
    val printer: GrammarPrinter = GrammarPrinter.default
) {
    fun withWhitespaceParser(
        whitespaceParser: Parser?
    ) = this.copy(whitespaceParser = whitespaceParser)

    fun withStartProduction(
        startProduction: Sym?
    ) = this.copy(startProduction = startProduction)

    fun withRedefinitionOption(
        redefinitionOption: RedefinitionOption?
    ) = this.copy(redefinitionOption = redefinitionOption ?: RedefinitionOption.ERROR)

    fun withCorrectnessCheck(
        checkCorrectness: Boolean
    ) = this.copy(checkCorrectness = checkCorrectness)

    fun withPrinter(
        printer: GrammarPrinter?
    ) = this.copy(printer = printer ?: GrammarPrinter.default)

    companion object {
        private const val defaultCheckCorrectness = true

        fun create(
            whitespaceParser: Parser?,
            startProduction: Sym?,
            redefinitionOption: RedefinitionOption?,
            checkCorrectness: Boolean,
            printer: GrammarPrinter?
        ): ParserCreationOptions {
            return ParserCreationOptions(
                whitespaceParser,
                startProduction,
                redefinitionOption ?: RedefinitionOption.defaultOption,
                checkCorrectness,
                printer ?: GrammarPrinter.default
            )
        }

        fun newWithStandardWhitespace(): ParserCreationOptions {
            return default
                .withWhitespaceParser(getPredefinedWhitespaceParser("standard"))
        }

        val default: ParserCreationOptions
            get() = create(null, null, null, defaultCheckCorrectness, null)
    }
}
