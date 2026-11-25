package de.kleinert.parsewisp.parser_options

import de.kleinert.parsewisp.Parsewisp.getPredefinedWhitespaceParser
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.grammar.GrammarPrinter
import de.kleinert.parsewisp.parser.Parser

/**
 * This class provides options for creating [Parser] instances.
 */
class ParserCreationOptions
/**
 * Constructor.
 *
 * @param whitespaceParser   A parser which is used to ignore whitespaces between words or characters. This parser is merged into the new parser when the creation options are used. If null, no such parser is used.
 * @param startProduction    The starting production name of the parser. If null, the first defined production is used.
 * @param redefinitionOption Sets what to do when a production appears twice in the definition.
 * @param checkCorrectness   Whether to check the correctness of the grammar when creating the parser.
 * @param printer            A helper for converting the rules to strings if ever required.
 * @since 0.9.7
 */(
    /**
     * A parser which is used to ignore whitespaces between words or characters. This parser is merged into the new parser when the creation options are used. If null, no such parser is used.
     *
     * @return A [Parser] or null.
     * @since 0.9.7
     */
    val whitespaceParser: Parser?,
    /**
     * The starting production name of the parser. If null, the first defined production is used.
     *
     * @return A symbol which should be the left-hand side of a production in the grammar.
     * @since 0.9.7
     */
    val startProduction: Sym?,
    /**
     * What to do when a production appears twice or more often in the definition.
     *
     * @return A [RedefinitionOption]
     * @see RedefinitionOption
     *
     * @since 0.9.7
     */
    val redefinitionOption: RedefinitionOption,
    private val checkCorrectness: Boolean,
    /**
     * Returns the [GrammarPrinter] associated with the created parser.
     *
     * @return The printer.
     * @since 0.9.7
     */
    val printer: GrammarPrinter
) {
    /**
     * Whether to check the correctness of the grammar when creating the parser.
     *
     * @return true or false
     * @since 0.9.7
     */
    fun doCheckCorrectness(): Boolean {
        return checkCorrectness
    }

    /**
     * Creates a new instance with the whitespace-ignoring parser set.
     *
     * @param whitespaceParser The parser (or null).
     * @return A new instance.
     * @since 0.9.7
     */
    fun withWhitespaceParser(
        whitespaceParser: Parser?
    ): ParserCreationOptions {
        if (this.whitespaceParser == whitespaceParser) return this
        return create(
            whitespaceParser, startProduction,
            redefinitionOption,
            checkCorrectness,
            printer
        )
    }

    /**
     * Creates a new instance with the start production set.
     *
     * @param startProduction The start production's name.
     * @return A new instance.
     * @since 0.9.7
     */
    fun withStartProduction(
        startProduction: Sym?
    ): ParserCreationOptions {
        if (this.startProduction == startProduction) return this
        return create(
            whitespaceParser, startProduction,
            redefinitionOption,
            checkCorrectness,
            printer
        )
    }

    /**
     * Sets what to do when a production appears twice in the definition.
     *
     * @param redefinitionOption Sets what to do when a production appears twice in the definition.
     * @return A new instance.
     * @since 0.9.7
     */
    fun withRedefinitionOption(
        redefinitionOption: RedefinitionOption?
    ): ParserCreationOptions {
        return create(
            whitespaceParser, startProduction,
            redefinitionOption,
            checkCorrectness,
            printer
        )
    }

    /**
     * Creates a new instance with [doCheckCorrectness] set to the parameter.
     *
     * @param checkCorrectness The new setting for [doCheckCorrectness].
     * @return A new instance.
     * @since 0.9.7
     */
    fun withCorrectnessCheck(
        checkCorrectness: Boolean
    ): ParserCreationOptions {
        return create(
            whitespaceParser, startProduction,
            redefinitionOption,
            checkCorrectness,
            printer
        )
    }

    /**
     * Creates a new instance with the specified [GrammarPrinter].
     *
     * @param printer The new grammar-printer.
     * @return A new instance.
     * @since 0.9.7
     */
    fun withPrinter(
        printer: GrammarPrinter?
    ): ParserCreationOptions {
        return create(
            whitespaceParser, startProduction,
            redefinitionOption,
            checkCorrectness,
            printer
        )
    }

    companion object {
        private const val defaultCheckCorrectness = true

        /**
         * Constructor.
         *
         * @param whitespaceParser   A parser which is used to ignore whitespaces between words or characters. This parser is merged into the new parser when the creation options are used. If null, no such parser is used.
         * @param startProduction    The starting production name of the parser. If null, the first defined production is used.
         * @param redefinitionOption Sets what to do when a production appears twice in the definition.
         * @param checkCorrectness   Whether to check the correctness of the grammar when creating the parser.
         * @param printer            A helper for converting the rules to strings if ever required.
         * @return A new instance.
         * @since 0.9.7
         */
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
                if (redefinitionOption == null) RedefinitionOption.defaultOption else redefinitionOption,
                checkCorrectness,
                if (printer == null) GrammarPrinter.default else printer
            )
        }

        /**
         * Creates a new instance using the most common whitespace parser.
         * <pre>
         * `// With whitespace parser:     var p = Parsewisp.parser("S = ('a' | 'b')*");     println(p.parse("a b      a\tb\na")); // Error     // With whitespace parser:     var p = Parsewisp.parser("S = ('a' | 'b')*", Parsewisp.ParserCreationOptions.newWithStandardWhitespace());     println(p.parse("a b      a\tb\na")); // [:S, a, b, a, b, a] `
        </pre> *
         *
         * @return A new instance.
         * @since 0.9.7
         */
        @JvmStatic
        fun newWithStandardWhitespace(): ParserCreationOptions {
            return default
                .withWhitespaceParser(getPredefinedWhitespaceParser("standard"))
        }

        @JvmStatic
        val default: ParserCreationOptions
            /**
             * The default settings. Equivalent to [.ParserCreationOptions] with using `null` or whichever defaults this class provides.
             *
             *
             *
             * Characteristics:
             *
             *  * Redefinition option: [RedefinitionOption.defaultOption]
             *
             *
             * @return default settings.
             * @since 0.9.7
             */
            get() = create(
                null, null, null, defaultCheckCorrectness, null
            )
    }
}
