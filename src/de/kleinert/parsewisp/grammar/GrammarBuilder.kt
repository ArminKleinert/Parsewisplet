package de.kleinert.parsewisp.grammar

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.error.IllegalGrammarException
import de.kleinert.parsewisp.grammar.Grammar.GrammarInfo
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parser_options.RedefinitionOption
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.ReductionType
import java.util.*
import java.util.function.BiFunction
import java.util.function.Function
import java.util.regex.Pattern
import java.util.stream.Stream

/**
 * This class provides an easy way to construct grammars. To use it, it needs to be inherited and the [.make] method overridden.
 * Using a GrammarBuilder is much faster than constructing grammars from strings.
 *
 *
 * In the following example, two equivalent grammars and parsers are constructed from a string and from a builder. The assertions show that the grammars are equal and that the parsers give equivalent outputs.
 * <pre>
 * `var gFromString = Parsewisp.parser("""                     S = NUMBER NUMBER*                     NUMBER = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'                     """)             .grammar();     var gFromGB = new GrammarBuilder(ParserCreationOptions.getDefault()) {         @Override         public void make() {             addProduction("S", cat(nt("NUMBER"), zeroOrMore(nt(Sym.sym("NUMBER")))));             addProduction("NUMBER", alt("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));         }     }.build();     Assertions.assertEquals(gFromString, gFromGB);     var pFromString = Parsewisp.parser(gFromString, ParserCreationOptions.getDefault().withStartProduction(Sym.sym("S")));     var pFromGB = Parsewisp.parser(gFromGB, ParserCreationOptions.getDefault().withStartProduction(Sym.sym("S")));     var text = "0123456789";     Assertions.assertEquals(pFromString.parse(text), pFromGB.parse(text)); `
</pre> *
 *
 *
 * The [.addProduction] method can be used to add new productions.
 *
 * @since 0.9.7
 */
abstract class GrammarBuilder protected constructor(redefinitionOption: RedefinitionOption = RedefinitionOption.Companion.defaultOption) {
    private val productions: LinkedHashMap<Sym, Rule>
    private val buffer: BufferForRules
    private val redefinitionOpt: RedefinitionOption
    private var wasMakeCalled = false

    /**
     * The starting production.
     */
    protected var startProduction: Sym? = null

    /**
     * Creates a new builder with the given option for redefinitions.
     *
     * @param redefinitionOption Option used when a production is added multiple times (see [.addProduction]).
     * @since 0.9.7
     */
    /**
     * Default constructor. Calls [GrammarBuilder] with the default value [RedefinitionOption.defaultOption].
     *
     * @since 0.9.7
     */
    init {
        this.productions = LinkedHashMap<Sym, Rule>()
        this.redefinitionOpt = redefinitionOption
        this.buffer = BufferForRules()
    }

    /**
     * Override this to create a grammar. Used in [.build].
     *
     * @since 0.9.7
     */
    protected open fun make() {
    }

    /**
     * Use this to construct the grammar.
     *
     * @return The grammar.
     * @since 0.9.7
     */
    fun build(): Grammar {
        return buildWithWhitespace(null, null, true)
    }

    /**
     * Use this to construct the grammar. Productions can be added before starting the builder.
     *
     * @param startProduction  Starting production. Set to null to use the first production that was added.
     * @param wsParser         Whitespace parser to include.
     * @param checkCorrectness Whether to check the grammar for validity.
     * @return The grammar.
     * @since 0.9.7
     */
    fun buildWithWhitespace(
        startProduction: Sym?,
        wsParser: Parser?,
        checkCorrectness: Boolean
    ): Grammar {
        val start: Sym

        if (!wasMakeCalled) {
            make()
            wasMakeCalled = true
        }

        val resultProductions = LinkedHashMap(productions)

        if (startProduction != null) {
            start = startProduction
        } else if (this.startProduction != null) {
            start = this.startProduction!!
        } else {
            start = resultProductions.keys.iterator().next()!!
        }

        //compress(resultProductions);
        ReductionType.Companion.applyStandardReductionToProductions(resultProductions)
        if (wsParser != null) {
            autoWhitespace(resultProductions, start, wsParser.grammar, wsParser.startProduction)
        }

        val g = Grammar(start, resultProductions)

        if (checkCorrectness) {
            val analysisResult = g.analyze()
            if (!analysisResult.isValid) throw IllegalGrammarException(
                stringBuildGrammarCreationFailureMessage(analysisResult)
            )
        }

        return g
    }

    private fun stringBuildGrammarCreationFailureMessage(analysisResult: GrammarInfo): String? {
        throw IllegalGrammarException(
            ("The grammar is invalid. Undefined productions: "
                    + analysisResult.undefinedUsedNTs
                    + "; number of terminals: "
                    + analysisResult.definedTerminals().size
                    + " (a valid grammar has at least one terminal)")
        )
    }

    /**
     * Add multiple entries.
     *
     * @param entries The entries.
     * @see .addProduction
     * @since 0.9.7
     */
    protected fun addAllProductions(
        entries: Collection<Map.Entry<Sym, Rule>>
    ) {
        for (entry in entries) {
            addProduction(entry!!.key!!, entry.value!!)
        }
    }

    /**
     * Adds a production to the output. The specific behavior depends on the [ParserCreationOptions.getRedefinitionOption] used.
     *
     * @param lhs The production's key. (left-hand-side)
     * @param rhs The production's right-hand-side.
     * @since 0.9.7
     */
    protected fun addProduction(
        lhs: Sym, rhs: Rule
    ) {
        val existing = productions.putIfAbsent(lhs, rhs)
        if (existing == null) return

        when (redefinitionOpt) {
            RedefinitionOption.OVERRIDE -> productions.put(lhs, rhs)
            RedefinitionOption.ERROR -> throw IllegalArgumentException(
                "Production already in grammar: " + lhs
            )

            RedefinitionOption.CHOICE -> productions.put(lhs, alt(existing, rhs))
            RedefinitionOption.KEEP -> {
            }
        }
    }

    /**
     * Adds a production to the output. This method is a convenient alternative to [.addProduction] if you do not want to make the string into a symbol yourself.
     *
     * @param lhs The production's key. (left-hand-side)
     * @param rhs The production's right-hand-side.
     * @see .addProduction
     * @since 0.9.7
     */
    protected fun addProduction(
        lhs: String, rhs: Rule
    ) {
        addProduction(sym(lhs), rhs)
    }

    /**
     * Create a [RegexTerm]. The rule might be buffered.
     * If the regex can only ever match a single string, a [StringTerm] might be returned instead.
     *
     * @param p The input regex.
     * @return A [RegexTerm] or something that returns equivalent outputs when parsing.
     * @since 0.9.7
     */
    protected fun regex(p: Pattern): Rule {
        return buffer.getOrAddRegex(p)
    }

    /**
     * See [.regex].
     *
     * @param s The input regex.
     * @return A [RegexTerm] or something that returns equivalent outputs when parsing.
     * @since 0.9.7
     */
    protected fun regex(s: String): Rule {
        return buffer.getOrAddRegex(Pattern.compile(s))
    }

    /**
     * Creates a [LookaheadRule].
     * The output can be a [EpsilonTerm] if the input is epsilon.
     *
     * @param input The rule to look for.
     * @return The new rule.
     * @since 0.9.7
     */
    protected fun look(input: Rule): Rule {
        return LookaheadRule.Companion.create(input)
    }

    /**
     * Creates a [NegativeLookaheadRule].
     * The output can be a [EpsilonTerm] if the input is epsilon.
     *
     * @param input The rule to avoid.
     * @return The new rule.
     * @since 0.9.7
     */
    protected fun neg(input: Rule): Rule {
        return NegativeLookaheadRule.Companion.create(input)
    }

    /**
     * Creates a [Rule] to match the codepoint range.
     * This can be any kind of [Rule] capable of accomplishing that task.
     *
     * @param lo The minimum codepoint.
     * @param hi The maximum codepoint.
     * @return The new rule.
     * @since 0.9.7
     */
    protected fun numVal(lo: Int, hi: Int): Rule {
        return ValueRangeTerm.Companion.create(lo, hi)
    }

    /**
     * Creates a [Rule] to match the codepoint.
     * This can be any kind of [Rule] capable of accomplishing that task.
     *
     * @param loHi The codepoint.
     * @return The new rule.
     * @since 0.9.7
     */
    protected fun numVal(loHi: Int): Rule {
        return ValueRangeTerm.Companion.create(loHi, loHi)
    }

    /**
     * Creates a [OrderedChoiceRule].
     *
     *  * If the argument List is empty, a [EpsilonTerm] is returned instead.
     *  * If there is only one element in the list, it is returned.
     *  * Otherwise, returns a [OrderedChoiceRule], as expected.
     *
     *
     * @param inputRules The parsers for the output.
     * @return A rule.
     * @since 0.9.7
     */
    protected fun ordAlt(inputRules: List<Rule>): Rule {
        return OrderedChoiceRule.Companion.create(inputRules)
    }

    /**
     * Create a [NonTerminal] from the input symbol. The output might be buffered.
     *
     * @param name The name symbol of the output rule.
     * @return A [NonTerminal].
     * @since 0.9.7
     */
    protected fun nt(name: Sym): NonTerminal {
        return buffer.getOrAddNt(name)
    }

    /**
     * Equivalent to [.nt] with the symbol creates from the input string.
     *
     * @param name The name symbol of the output rule.
     * @return A [NonTerminal].
     * @since 0.9.7
     */
    protected fun nt(name: String): NonTerminal {
        return buffer.getOrAddNt(sym(name))
    }

    /**
     * Creates a [StringTerm]
     * or [EpsilonTerm] if the string is empty.
     *
     * @param string          The string to match.
     * @param caseInsensitive Whether the Terminal will match case-insensitive.
     * @return The new rule.
     * @since 0.9.7
     */
    /**
     * Creates a [StringTerm]
     * or [EpsilonTerm] if the string is empty
     * . The case-sensitivity depends on the options used for this [GrammarBuilder].
     *
     * @param string The string to match.
     * @return The new rule.
     * @since 0.9.7
     */
    protected fun string(string: String, caseInsensitive: Boolean = false): Rule {
        return buffer.getOrAddString(string, caseInsensitive)
    }

    /**
     * Creates a case-sensitive [StringTerm]
     * or [EpsilonTerm] if the string is empty.
     *
     * @param string The string to match.
     * @return The new rule.
     * @since 0.9.7
     */
    protected fun stringCS(string: String): Rule {
        return string(string, false)
    }

    /**
     * Creates a case-insensitive [StringTerm]
     * or [EpsilonTerm] if the string is empty.
     *
     * @param string The string to match.
     * @return The new rule.
     * @since 0.9.7
     */
    protected fun stringCI(string: String): Rule {
        return string(string, true)
    }

    /**
     * Creates a [TransformRule].
     *
     * @param rule      The rule.
     * @param transform The transformer function for the result.
     * @return The new rule.
     * @since 0.9.7
     */
    protected fun transform(rule: Rule, transform: BiFunction<List<Any?>, Int, List<Any?>>): Rule {
        return TransformRule.create(rule, transform)
    }

    /**
     * Create a copy of the input for which [Rule.isHiddenInResult1] returns true.
     *
     * @param o The input.
     * @return A copy of the input for which [Rule.isHiddenInResult1] returns true.
     * @since 0.9.7
     */
    protected fun hide(o: Rule): Rule {
        return o.hideInResult()
    }

    /**
     * Get an epsilon rule.
     *
     * @return An [EpsilonTerm].
     * @since 0.9.7
     */
    protected fun eps(): Rule {
        return EpsilonTerm.default
    }

    /**
     * Create a rule which matches end of input.
     *
     * @return A rule.
     * @since 0.9.7
     */
    protected fun eof(): Rule {
        return EOFTerm.default
    }

    /**
     * Creates a [ConcatRule].
     *
     *  * If the argument List is empty, a [EpsilonTerm] is returned instead.
     *  * If there is only one element in the list, it is returned.
     *  * Otherwise, returns a [ConcatRule], as expected.
     *
     *
     * @param rule  First rule for the output.
     * @param rules More rules for the output.
     * @return A rule.
     * @since 0.9.7
     */
    protected fun cat(
        rule: Rule, vararg rules: Rule
    ): Rule {
        return cat(Stream.concat(Stream.of<Rule>(rule), Arrays.stream<Rule>(rules)).toList())
    }

    /**
     * Creates a [ConcatRule].
     *
     *  * If the argument List is empty, a [EpsilonTerm] is returned instead.
     *  * If there is only one element in the list, it is returned.
     *  * Otherwise, returns a [ConcatRule], as expected.
     *
     *
     * @param rules The rules for the output.
     * @return A rule.
     * @since 0.9.7
     */
    protected fun cat(
        rules: List<Rule>
    ): Rule {
        return ConcatRule.create(rules)
    }

    /**
     * Like [.cat], except it assumes that (1) the input is not empty and (2) the input does not include epsilons.
     *
     * @param rules The rules.
     * @return A [ConcatRule].
     * @since 0.9.7
     */
    protected fun catUnsafe(
        rules: List<Rule>
    ): ConcatRule {
        return ConcatRule.Companion.createDirect(rules)
    }

    /**
     * Creates a [AlternationRule].
     *
     *  * If the argument List is empty, a [EpsilonTerm] is returned instead.
     *  * If there is only one element in the list, it is returned.
     *  * Otherwise, returns a [AlternationRule], as expected.
     *
     *
     * @param rule  The first element for the output.
     * @param rules More elements.
     * @return A rule.
     * @since 0.9.7
     */
    protected fun alt(
        rule: Rule, vararg rules: Rule
    ): Rule {
        val result = Stream
            .concat(Stream.of<Rule>(rule), Arrays.stream<Rule>(rules))
            .toList()
        return alt(result)
    }

    /**
     * Creates a [AlternationRule].
     *
     *  * If the argument List is empty, a [EpsilonTerm] is returned instead.
     *  * If there is only one element in the list, it is returned.
     *  * Otherwise, returns a [AlternationRule], as expected.
     *
     *
     * @param rules The rules for the output.
     * @return A rule.
     * @since 0.9.7
     */
    protected fun alt(rules: List<Rule>): Rule {
        return AlternationRule.Companion.create(rules)
    }

    /**
     * Takes a String and returns a rule which matches any char in it. If the input is empty, returns a rule which matches the empty string.
     *
     * @param chars A string of matchable chars.
     * @return A rule.
     * @since 0.9.7
     */
    protected fun oneOf(chars: String): Rule {
        if (chars.isEmpty()) return eps()
        val split = ArrayList<Rule>()
        for (i in 0..<chars.length) {
            split.add(string(chars.get(i).toString()))
        }
        return alt(split)
    }

    /**
     * Like [.alt] except the input list is assumed to be distinct
     * (each rule in the list occurs exactly once) and not empty.
     * Use this method only if you are sure that the rules are distinct.
     *
     * @param rules The rules.
     * @return A rule.
     * @since 0.9.7
     */
    protected fun altUnsafe(
        rules: List<Rule>
    ): AlternationRule {
        return AlternationRule.Companion.altUnsafe(rules)
    }

    /**
     * Represents a special sequence. The typical EBNF-notation is `?...?`
     * where `...` stands for some free-form text.
     * For example, `S = ?any whitespace except newline?` means what it says.
     * This allows some interaction with the program around the parser.
     * However, special sequences have to be heavily abstracted here.
     *
     * @param description The description of the special sequence.
     * @param function    The function which does what the description says.
     * @return A [SpecialSequenceRule].
     * @since 0.9.7
     */
    protected fun specialSequence(
        description: String,
        function: Function<String, Optional<String>>
    ): Rule {
        return SpecialSequenceRule.create(description, function)
    }

    /**
     * Creates a [VariableRepetitionRule] or an [EpsilonTerm] if the input is epsilon.
     *
     *  * If minimum and maximum amount of repetitions is set to 0, return an [EpsilonTerm].
     *  * If minimum and maximum amount of repetitions is set to 1, may return the input rule.
     *  * If minimum and maximum amount of repetitions are equal, a [ConcatRule] might be returned instead.
     *  * Otherwise, does as normally expected.
     *
     *
     * @param min  The minimum amount of repetitions.
     * @param max  The maximum amount of repetitions.
     * @param rule The rule.
     * @return A rule, as described.
     * @since 0.9.7
     */
    protected fun rep(
        rule: Rule, min: Int, max: Int
    ): Rule {
        return VariableRepetitionRule.Companion.create((rule), min, max)
    }

    /**
     * Equivalent to [.zeroOrMore].
     *
     * @param rule The rule to repeat.
     * @return A rule.
     * @since 0.9.7
     */
    protected fun rep(
        rule: Rule
    ): Rule {
        return zeroOrMore(rule)
    }

    /**
     * Equivalent to `rep(rule, exact, exact)`.
     *
     * @param rule  The rule.
     * @param exact Minimum and maximum number of repetitions.
     * @return A repetition rule.
     * @see .rep
     * @since 0.9.7
     */
    protected fun rep(
        rule: Rule, exact: Int
    ): Rule {
        return rep(rule, exact, exact)
    }

    /**
     * Creates a [ExclusionRule].
     *
     * @param ruleExpected The rule that must be matched.
     * @param ruleExcluded The rule that must not be matched.
     * @return A [ExclusionRule].
     * @since 0.9.7
     */
    protected fun exclude(
        ruleExpected: Rule, ruleExcluded: Rule
    ): Rule {
        val r1 = ruleExpected
        val r2 = ruleExcluded
        return ExclusionRule.Companion.create(r1, r2)
    }

    /**
     * Equivalent to `rep(rule, min, Integer.MAX_VALUE)`.
     *
     * @param rule The rule.
     * @param min  Minimum number of repetitions.
     * @return A repetition rule.
     * @see .rep
     * @since 0.9.7
     */
    protected fun repMin(
        rule: Rule, min: Int
    ): Rule {
        return rep(rule, min, Int.MAX_VALUE)
    }

    /**
     * Equivalent to `rep(rule, 0, max)`.
     *
     * @param rule The rule.
     * @param max  Maximum number of repetitions.
     * @return A repetition rule.
     * @see .rep
     * @since 0.9.7
     */
    protected fun repMax(
        rule: Rule, max: Int
    ): Rule {
        return rep(rule, 0, max)
    }

    /**
     * Creates a [ZeroOrMoreRule] or an [EpsilonTerm] if the input is epsilon.
     *
     * @param rule The rule to match repeatedly.
     * @return A rule.
     * @since 0.9.7
     */
    protected fun zeroOrMore(rule: Rule): Rule {
        return ZeroOrMoreRule.Companion.create(rule)
    }

    /**
     * Creates a [OnceOrMoreRule] or an [EpsilonTerm] if the input is epsilon.
     *
     * @param rule The rule to match repeatedly.
     * @return A rule.
     * @since 0.9.7
     */
    protected fun onceOrMore(rule: Rule): Rule {
        return OnceOrMoreRule.Companion.create(rule)
    }

    /**
     * Creates a [OptionalRule] or an [EpsilonTerm] if the input is epsilon.
     *
     * @param rule The rule to maybe match.
     * @return A rule.
     * @since 0.9.7
     */
    protected fun opt(rule: Rule): Rule {
        return OptionalRule.Companion.create(rule)
    }

    private fun autoWhitespaceHelper(
        originalRule: Rule,
        whitespaceRule: Rule
    ): Rule {
        if (originalRule is NonTerminal) {
            return originalRule
        }
        if (originalRule is EpsilonTerm) {
            return originalRule
        }
        if (originalRule is RuleWithChild) {
            val rule = originalRule
            return rule.withRule(
                autoWhitespaceHelper(rule.rule, whitespaceRule)
            )
        }
        if (originalRule is RuleWithManyChildren) {
            val combWithParsers = originalRule
            val rules = combWithParsers
                .rules
                .map { p: Rule -> autoWhitespaceHelper(p!!, whitespaceRule) }
                .toList()
            return combWithParsers.withRules(rules)
        }
        if (originalRule is Terminal) {
            val rules: MutableList<Rule> = ArrayList()
            rules.add(whitespaceRule)
            val result: Rule
            if (!originalRule.reduction.isHiddenOrRaw1()) {
                // Hide the terminal in the output.
                // It still appears in the tree, but is flattened into the concatenation.
                rules.add(
                    originalRule.withReduction(
                        ReductionType.merging()
                    )
                )
                result = cat(rules).withReduction(originalRule.reduction)
            } else {
                rules.add(originalRule)
                result = cat(rules)
            }
            return result
        }
        if (originalRule is SpecialSequenceRule) {
            return originalRule
        }
        throw IllegalArgumentException(originalRule.javaClass.getName())
    }

    private fun autoWhitespace(
        productions: LinkedHashMap<Sym?, Rule?>,
        start: Sym,
        grammarWS: Grammar,
        startWS: Sym
    ) {
        val wsRule =
            opt(nt(startWS)).hideInResult()

        val finalGrammar =
            productions
        for (symRuleEntry in finalGrammar.entries) {
            symRuleEntry.setValue(
                autoWhitespaceHelper(
                    symRuleEntry.value!!, wsRule
                )
            )
        }

        val startWithoutReduction = (finalGrammar.get(start)!!
            .withReduction(ReductionType.initial()))
        val newStartComb =
            cat(listOf(startWithoutReduction, wsRule))
                .withReduction(finalGrammar.get(start)!!.reduction)

        finalGrammar.put(start, newStartComb)
        finalGrammar.putAll(grammarWS)
        finalGrammar.put(
            startWS,
            grammarWS.getProduction(startWS)!!.mergeToTop()
        )
    }

    /**
     * Buffers a rule for the lifetime of this builder.
     *
     * @param rule The rule.
     * @param <T>  The type of the rule.
     * @return The same rule or a previously buffered equivalent.
     * @since 0.9.7
    </T> */
    protected fun <T : Rule?> buffer(rule: T?): T? {
        if (rule is StringTerm) {
            return buffer.getOrAdd(rule as StringTerm) as T
        } else if (rule is RegexTerm) {
            return buffer.getOrAdd(rule as RegexTerm) as T
        } else if (rule is NonTerminal) {
            return buffer.getOrAdd(rule as NonTerminal) as T
        }
        return rule
    }
}
