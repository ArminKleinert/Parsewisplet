package de.kleinert.parsewisp.parser_options

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parser_options.ParsingOptions.Companion.DEFAULT_EMBED_FAILURES
import de.kleinert.parsewisp.parser_options.ParsingOptions.Companion.DEFAULT_FAILURE_IF_EMPTY
import de.kleinert.parsewisp.parser_options.ParsingOptions.Companion.DEFAULT_ITERATIVE_DEEPENING
import de.kleinert.parsewisp.parser_options.ParsingOptions.Companion.DEFAULT_PARTIAL
import de.kleinert.parsewisp.parser_options.ParsingOptions.Companion.DEFAULT_START
import de.kleinert.parsewisp.parser_options.ParsingOptions.Companion.DEFAULT_UNHIDE
import de.kleinert.parsewisp.parsing.RegexTerm
import de.kleinert.parsewisp.result.ParseFailureNode
import de.kleinert.parsewisp.result.ParseTree

/**
 * Options for parsing with an already created parser.
 *
 * @param start                      Start production for the parse. Use `null` to use the Parser's start production.
 * @param usePartial                 Whether to return partial (incomplete) parses.
 * @param unhide                     Which parts of a parse result to show when they would normally be hidden by the parser.
 * @param embedFailureInParseTree    If true, a failed parse results in a [ParseTree], as a success would, but a [ParseFailureNode] is embedded in the tree.
 * @param iterativeDeepening         Whether to iteratively deepen parsing when parsing with a regex terminal. See [RegexTerm.parse].
 * @param failureIfEmpty             Important for [de.kleinert.parsewisp.parser.Parser.parses] only. If false, return an empty list if the parsing failed. If true, return the failure.
 * @param stopEarlyOnEmptyRepetition If a repetition (once-or-more, zero-or-more, variable) returns an empty parse, it can be stopped early, provided that the minimum number of parses has been resolved.
 * @param resultLimit                Limit for the number of results. This setting only takes effect if [de.kleinert.parsewisp.parser.Parser.parses] is used.
 * @see DEFAULT_START
 * @see DEFAULT_PARTIAL
 * @see DEFAULT_UNHIDE
 * @see DEFAULT_EMBED_FAILURES
 * @see DEFAULT_FAILURE_IF_EMPTY
 * @see DEFAULT_STOP_EARLY_ON_EMPTY_REPETITION
 * @see DEFAULT_RESULT_LIMIT
 */
data class ParsingOptions(
    val start: Sym?,
    val usePartial: Boolean,
    val unhide: UnhideOptions,
    val embedFailureInParseTree: Boolean,
    val iterativeDeepening: Boolean,
    val failureIfEmpty: Boolean,
    val stopEarlyOnEmptyRepetition: Boolean,
    val resultLimit: Int
) {
    /**
     * When a parser is created, it carries its own start production. With this option, the start production can be forcefully changed for a parse. If the parser's start should be used, use `null` as the parameter.
     *
     *
     * Example:
     * <pre>
     * `// The grammar has these two, unrelated productions. The first production is A. So that is the production the parser uses.     //    A := 'a'     //    B := 'b'     var p = Parsewisp.parser("A := 'a'\nB := 'b'");     println(p.parse("a")); // Success: [:A, a]     println(p.parse("b")); // Failure: 'b' could not be parsed from production A.     // With the start production explicitly changed, it works:     println(p.parse("b", Parsewisp.ParsingOptions.getDefault().withStart(Keyword.intern("B")))); // [:B, b] `
    </pre> *
     * This can be useful if the grammar has multiple unrelated parts.
     *
     * @param start The new start production.
     * @return A new instance.
     * @see ParsingOptions.start
     * @see DEFAULT_START
     *
     * @see ParsingOptions.default
     */
    fun withStart(start: Sym?): ParsingOptions {
        if (this.start == start) return this
        return ParsingOptions(
            start,
            usePartial,
            unhide,
            embedFailureInParseTree,
            iterativeDeepening,
            failureIfEmpty,
            stopEarlyOnEmptyRepetition,
            resultLimit
        )
    }

    /**
     * When generating a parse tree, the parser tries to parse the full string first. With this option, partial parses can be made available.
     * <br></br>
     * The option only applies when requesting the full parse forest (e.g. [de.kleinert.parsewisp.parser.Parser.parses]), but not when only a single parse is requested (e.g. [de.kleinert.parsewisp.parser.Parser.parse]).
     * <pre>
     * `var p = Parsewisp.parser("S := 'a'+");     println(p.parses("aa")); // [[:S, a, a]]     var opts = Parsewisp.ParsingOptions.getDefault().withPartialSetTo(true);     println(p.parses("aa", opts)); // [[:S, a], [:S, a, a]]     // The option has no effect on single parse:     println(p.parse("aa", opts)); // [:S, a, a] `
    </pre> *
     *
     * @param usePartial The argument as a boolean.
     * @return A new instance.
     * @see .usePartial
     */
    fun withPartial(usePartial: Boolean): ParsingOptions {
        if (this.usePartial == usePartial) return this
        return ParsingOptions(
            start,
            usePartial,
            unhide,
            embedFailureInParseTree,
            iterativeDeepening,
            failureIfEmpty,
            stopEarlyOnEmptyRepetition,
            resultLimit
        )
    }

    /**
     * Create a new instance with [.unhide] set to the parameter.
     *
     *
     * Consider the following grammar:
     * <pre>
     * `S   = 'a' <B> C <D> 'e' (* Expects the string "abcde", but will hide the substrings parsed by productions B and D *)     B   = 'b'     <C> = 'c' (* Expects the string "c", but will "flatten" itself into the output. *)     <D> = 'd' (* Same as C *) `
    </pre> *
     * Explanation: Production S expects the string `"abcde"`, but the outputs of productions B and D will be hidden. The result of production C will be shown in the output without the associated tag "C".
     * <br></br>
     * Now the code:
     * <pre>
     * `var p = Parsewisp.parser("S = 'a' <B> C <D> 'a'\nB = 'b'+\n<C> = 'c'\n<D> = 'd'");     // No options.     println("Default => " + p.parse("abcda"));       // [:S, a, c, a]     var opts = Parsewisp.ParsingOptions.getDefault();     println("Default => " + p.parse("abcda"));       // [:S, a, c, a]     opts = opts.withUnhide(Parsewisp.UnhideOptions.none);     println("none    => " + p.parse("abcda", opts)); // [:S, a, c, a]     opts = opts.withUnhide(Parsewisp.UnhideOptions.tags);     println("tags    => " + p.parse("abcda", opts)); // [:S, a, [:C, c], a]     opts = opts.withUnhide(Parsewisp.UnhideOptions.content);     println("content => " + p.parse("abcda", opts)); // [:S, a, [:B, b], c, d, a]     opts = opts.withUnhide(Parsewisp.UnhideOptions.all);     println("all     => " + p.parse("abcda", opts)); // [:S, a, [:B, b], [:C, c], [:D, d], a] `
    </pre> *
     *
     * @param unhide The new option.
     * @return A new instance.
     * @see .unhide
     */
    fun withUnhide(unhide: UnhideOptions): ParsingOptions {
        if (this.unhide == unhide) return this
        return ParsingOptions(
            start,
            usePartial,
            unhide,
            embedFailureInParseTree,
            iterativeDeepening,
            failureIfEmpty,
            stopEarlyOnEmptyRepetition,
            resultLimit
        )
    }

    /**
     * Creates a new instance with the [.embedFailureInParseTree] option set to the parameter.
     *
     * <pre>
     * `var p = Parsewisp.parser("S := #'a'+");     var text = "ab";     // A normal parse results in a failure.     println(p.parse("ab").castToParseFailure().contentsToString());     // => [1, [ParseFailureReason[rule=#"a", reasonString=null, untilEndOfInput=false, tag=regex]], 1, 2, ab]     // With the total option, a parsetree is returned, potentially providing more information about the failure.     var opts = Parsewisp.ParsingOptions.getDefault().withTotal(true);     println(p.parse("ab", opts));     // => [:S, "a", [:failure, could not parse "b" at 1..2]] `
    </pre> *
     *
     * @param embedFailureInParseTree The new (or old) setting.
     * @return A new instance.
     * @see .embedFailureInParseTree
     */
    fun withEmbedFailureInParseTree(embedFailureInParseTree: Boolean): ParsingOptions {
        if (this.embedFailureInParseTree == embedFailureInParseTree) return this
        return ParsingOptions(
            start,
            usePartial,
            unhide,
            embedFailureInParseTree,
            iterativeDeepening,
            failureIfEmpty,
            stopEarlyOnEmptyRepetition,
            resultLimit
        )
    }

    /**
     * Creates a new instance with the [.iterativeDeepening] option set to the parameter.
     *
     * @param iterativeDeepening The new (or old) setting.
     * @return A new instance.
     * @see .iterativeDeepening
     */
    fun withIterativeDeepening(iterativeDeepening: Boolean): ParsingOptions {
        if (this.iterativeDeepening == iterativeDeepening) return this
        return ParsingOptions(
            start,
            usePartial,
            unhide,
            embedFailureInParseTree,
            iterativeDeepening,
            failureIfEmpty,
            stopEarlyOnEmptyRepetition,
            resultLimit
        )
    }

    /**
     * Creates a new instance with the [.failureIfEmpty] option set to the parameter.
     *
     * @param failureIfEmpty The new (or old) setting.
     * @return A new instance.
     * @see .failureIfEmpty
     */
    fun withFailureIfEmpty(failureIfEmpty: Boolean): ParsingOptions {
        if (this.failureIfEmpty == failureIfEmpty) return this
        return ParsingOptions(
            start,
            usePartial,
            unhide,
            embedFailureInParseTree,
            iterativeDeepening,
            failureIfEmpty,
            stopEarlyOnEmptyRepetition,
            resultLimit
        )
    }

    /**
     * Creates a new instance with the [.stopEarlyOnEmptyRepetition] option set to the parameter.
     *
     * @param stopEarlyOnEmptyRepetition The new (or old) setting.
     * @return A new instance.
     * @see .stopEarlyOnEmptyRepetition
     */
    fun withStopEarlyOnEmptyRepetition(stopEarlyOnEmptyRepetition: Boolean): ParsingOptions {
        if (this.stopEarlyOnEmptyRepetition == stopEarlyOnEmptyRepetition) return this
        return ParsingOptions(
            start,
            usePartial,
            unhide,
            embedFailureInParseTree,
            iterativeDeepening,
            failureIfEmpty,
            stopEarlyOnEmptyRepetition,
            resultLimit
        )
    }

    /**
     * Creates a new instance with the [.resultLimit] option set to the parameter.
     *
     * @param maxResults The new (or old) setting.
     * @return A new instance.
     * @see .resultLimit
     */
    fun withResultLimit(maxResults: Int): ParsingOptions {
        if (this.resultLimit == maxResults) return this
        return ParsingOptions(
            start,
            usePartial,
            unhide,
            embedFailureInParseTree,
            iterativeDeepening,
            failureIfEmpty,
            stopEarlyOnEmptyRepetition,
            maxResults
        )
    }

    companion object {
        /**
         * Default for the start production name of a parse operation. (`null`)
         */
        val DEFAULT_START: Sym? = null

        /**
         * Default for the start production name of a parse operation. (`false`)
         */
        const val DEFAULT_PARTIAL: Boolean = false

        /**
         * By default, leave the parse trees as intended (hidden parts stay hidden). The value is [UnhideOptions.NONE].
         */
        val DEFAULT_UNHIDE: UnhideOptions = UnhideOptions.NONE

        /**
         * By default, do not include failure nodes in parse trees. (`false`)
         */
        const val DEFAULT_EMBED_FAILURES: Boolean = false

        /**
         * By default, do not iteratively deepen search when parsing with a regex. (`false`)<br></br>
         * The reason is that it is much slower to do.
         */
        const val DEFAULT_ITERATIVE_DEEPENING: Boolean = false

        /**
         * `false` by default.
         */
        const val DEFAULT_FAILURE_IF_EMPTY: Boolean = false

        /**
         * `true` by default.
         */
        const val DEFAULT_STOP_EARLY_ON_EMPTY_REPETITION: Boolean = true

        /**
         * [Integer.MAX_VALUE] by default. Lists in the JVM can typically not hold that many elements.
         */
        const val DEFAULT_RESULT_LIMIT: Int = Int.MAX_VALUE

        val default: ParsingOptions
            /**
             * Calls [ParsingOptions] with the static defaults.
             *
             * @return An instance of this class, using all the static DEFAULT_* values.
             * @see DEFAULT_START
             * @see DEFAULT_PARTIAL
             * @see DEFAULT_UNHIDE
             * @see DEFAULT_EMBED_FAILURES
             * @see DEFAULT_ITERATIVE_DEEPENING
             * @see DEFAULT_FAILURE_IF_EMPTY
             */
            get() = ParsingOptions(
                DEFAULT_START, DEFAULT_PARTIAL, DEFAULT_UNHIDE,
                DEFAULT_EMBED_FAILURES, DEFAULT_ITERATIVE_DEEPENING,
                DEFAULT_FAILURE_IF_EMPTY,
                DEFAULT_STOP_EARLY_ON_EMPTY_REPETITION, DEFAULT_RESULT_LIMIT
            )
    }
}
