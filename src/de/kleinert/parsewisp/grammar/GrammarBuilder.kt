package de.kleinert.parsewisp.grammar

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.error.IllegalGrammarException
import de.kleinert.parsewisp.grammar.Grammar.GrammarInfo
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parser_options.RedefinitionOption
import de.kleinert.parsewisp.parsing.*
import de.kleinert.parsewisp.reduction.Reduction
import de.kleinert.parsewisp.reduction.Reduction.RedType

/**
 * This class provides an easy way to construct grammars. To use it, it needs to be inherited and the [.make] method overridden.
 * Using a GrammarBuilder is much faster than constructing grammars from strings.
 *
 *
 * In the following example, two equivalent grammars and parsers are constructed from a string and from a builder. The assertions show that the grammars are equal and that the parsers give equivalent outputs.
 * <pre>
 * `var gFromString = Parsewisp.parser("""                     S = NUMBER NUMBER*                     NUMBER = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'                     """)             .grammar();     var gFromGB = new GrammarBuilder(ParserCreationOptions.getDefault()) {         @Override         public void make() {             addProduction("S", cat(nt("NUMBER"), zeroOrMore(nt(Sym.sym("NUMBER")))));             addProduction("NUMBER", alt("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));         }     }.build();     Assertions.assertEquals(gFromString, gFromGB);     var pFromString = Parsewisp.parser(gFromString, ParserCreationOptions.getDefault().withStartProduction(Sym.sym("S")));     var pFromGB = Parsewisp.parser(gFromGB, ParserCreationOptions.getDefault().withStartProduction(Sym.sym("S")));     var text = "0123456789";     Assertions.assertEquals(pFromString.parse(text), pFromGB.parse(text)); `
 * </pre>
 *
 * The [GrammarBuilder.addProduction] method can be used to add new productions.
 */
abstract class GrammarBuilder protected constructor(redefinitionOption: RedefinitionOption = RedefinitionOption.defaultOption) {
    private val productions: LinkedHashMap<Sym, Rule> = LinkedHashMap()
    private val buffer: BufferForRules = BufferForRules()
    private val redefinitionOpt: RedefinitionOption = redefinitionOption
    private var wasMakeCalled = false

    /**
     * The starting production.
     */
    protected var startProduction: Sym? = null

    /**
     * Override this to create a grammar. Used in [.build].
     *
     */
    protected open fun make() {
    }

    /**
     * Use this to construct the grammar.
     *
     * @return The grammar.
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

        start = if (startProduction != null) {
            startProduction
        } else if (this.startProduction != null) {
            this.startProduction!!
        } else {
            resultProductions.keys.iterator().next()!!
        }

        //compress(resultProductions);
        Reduction.applyStandardReductionToProductions(resultProductions)
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
     */
    protected fun addAllProductions(
        entries: Collection<Map.Entry<Sym, Rule>>
    ) {
        for ((key, value) in entries) {
            addProduction(key, value)
        }
    }

    /**
     * Adds a production to the output. The specific behavior depends on the [ParserCreationOptions.redefinitionOption] used.
     *
     * @param lhs The production's key. (left-hand-side)
     * @param rhs The production's right-hand-side.
     */
    protected fun addProduction(
        lhs: Sym, rhs: Rule
    ) {
        val existing = productions.putIfAbsent(lhs, rhs) ?: return

        when (redefinitionOpt) {
            RedefinitionOption.OVERRIDE -> productions[lhs] = rhs
            RedefinitionOption.ERROR -> throw IllegalArgumentException(
                "Production already in grammar: $lhs"
            )

            RedefinitionOption.CHOICE -> productions[lhs] = alt(existing, rhs)
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
     */
    protected fun addProduction(
        lhs: String, rhs: Rule
    ) {
        addProduction(Sym.sym(lhs), rhs)
    }

    /**
     * Create a [RegexTerm]. The rule might be buffered.
     * If the regex can only ever match a single string, a [StringTerm] might be returned instead.
     *
     * @param p The input regex.
     * @return A [RegexTerm] or something that returns equivalent outputs when parsing.
     */
    protected fun regex(p: Regex): Rule {
        return buffer.getOrAddRegex(p)
    }

    /**
     * See [.regex].
     *
     * @param s The input regex.
     * @return A [RegexTerm] or something that returns equivalent outputs when parsing.
     */
    protected fun regex(s: String): Rule {
        return buffer.getOrAddRegex(Regex(s))
    }

    /**
     * Creates a [LookaheadRule].
     * The output can be a [EpsilonTerm] if the input is epsilon.
     *
     * @param input The rule to look for.
     * @return The new rule.
     */
    protected fun look(input: Rule): Rule {
        return LookaheadRule.create(input)
    }

    /**
     * Creates a [NegativeLookaheadRule].
     * The output can be a [EpsilonTerm] if the input is epsilon.
     *
     * @param input The rule to avoid.
     * @return The new rule.
     */
    protected fun neg(input: Rule): Rule {
        return NegativeLookaheadRule.create(input)
    }

    /**
     * Creates a [Rule] to match the codepoint range.
     * This can be any kind of [Rule] capable of accomplishing that task.
     *
     * @param lo The minimum codepoint.
     * @param hi The maximum codepoint.
     * @return The new rule.
     */
    protected fun numVal(lo: Int, hi: Int): Rule {
        return ValueRangeTerm.create(lo, hi)
    }

    /**
     * Creates a [Rule] to match the codepoint.
     * This can be any kind of [Rule] capable of accomplishing that task.
     *
     * @param loHi The codepoint.
     * @return The new rule.
     */
    protected fun numVal(loHi: Int): Rule {
        return ValueRangeTerm.create(loHi, loHi)
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
     */
    protected fun ordAlt(inputRules: List<Rule>): Rule {
        return OrderedChoiceRule.create(inputRules)
    }

    /**
     * Create a [NonTerminal] from the input symbol. The output might be buffered.
     *
     * @param name The name symbol of the output rule.
     * @return A [NonTerminal].
     */
    protected fun nt(name: Sym): NonTerminal {
        return buffer.getOrAddNt(name)
    }

    /**
     * Equivalent to [.nt] with the symbol creates from the input string.
     *
     * @param name The name symbol of the output rule.
     * @return A [NonTerminal].
     */
    protected fun nt(name: String): NonTerminal {
        return buffer.getOrAddNt(Sym.sym(name))
    }

    /**
     * Creates a [StringTerm]
     * or [EpsilonTerm] if the string is empty.
     *
     * @param string          The string to match.
     * @param caseInsensitive Whether the Terminal will match case-insensitive.
     * @return The new rule.
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
     */
    protected fun transform(rule: Rule, transform: (List<Any?>, Int) -> List<Any?>): Rule {
        return TransformRule.create(rule, transform)
    }

    /**
     * Create a copy of the input for which [Rule.isHiddenInResult] returns true.
     *
     * @param o The input.
     * @return A copy of the input for which [Rule.isHiddenInResult] returns true.
     */
    protected fun hide(o: Rule): Rule {
        return o.hideInResult()
    }

    /**
     * Get an epsilon rule.
     *
     * @return An [EpsilonTerm].
     */
    protected fun eps(): Rule {
        return EpsilonTerm.default
    }

    /**
     * Create a rule which matches end of input.
     *
     * @return A rule.
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
     */
    protected fun cat(
        rule: Rule, vararg rules: Rule
    ): Rule {
        val l = mutableListOf(rule)
        l.addAll(rules)
        return cat(l)
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
     */
    protected fun catUnsafe(
        rules: List<Rule>
    ): ConcatRule {
        return ConcatRule.createDirect(rules)
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
     */
    protected fun alt(
        rule: Rule, vararg rules: Rule
    ): Rule {
        val l = mutableListOf(rule)
        l.addAll(rules)
        return alt(l)
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
     */
    protected fun alt(rules: List<Rule>): Rule {
        return AlternationRule.create(rules)
    }

    /**
     * Takes a String and returns a rule which matches any char in it. If the input is empty, returns a rule which matches the empty string.
     *
     * @param chars A string of matchable chars.
     * @return A rule.
     */
    protected fun oneOf(chars: String): Rule {
        if (chars.isEmpty()) return eps()
        val split = mutableListOf<Rule>()
        for (i in chars.indices) {
            split.add(string(chars[i].toString()))
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
     */
    protected fun altUnsafe(
        rules: List<Rule>
    ): AlternationRule {
        return AlternationRule.altUnsafe(rules)
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
     */
    protected fun specialSequence(
        description: String,
        function: (String) -> String?
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
     */
    protected fun rep(
        rule: Rule, min: Int, max: Int
    ): Rule {
        return VariableRepetitionRule.create((rule), min, max)
    }

    /**
     * Equivalent to [.zeroOrMore].
     *
     * @param rule The rule to repeat.
     * @return A rule.
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
     */
    protected fun exclude(
        ruleExpected: Rule, ruleExcluded: Rule
    ): Rule {
        return ExclusionRule.create(ruleExpected, ruleExcluded)
    }

    /**
     * Equivalent to `rep(rule, min, Integer.MAX_VALUE)`.
     *
     * @param rule The rule.
     * @param min  Minimum number of repetitions.
     * @return A repetition rule.
     * @see .rep
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
     */
    protected fun zeroOrMore(rule: Rule): Rule {
        return ZeroOrMoreRule.create(rule)
    }

    /**
     * Creates a [OnceOrMoreRule] or an [EpsilonTerm] if the input is epsilon.
     *
     * @param rule The rule to match repeatedly.
     * @return A rule.
     */
    protected fun onceOrMore(rule: Rule): Rule {
        return OnceOrMoreRule.create(rule)
    }

    /**
     * Creates a [OptionalRule] or an [EpsilonTerm] if the input is epsilon.
     *
     * @param rule The rule to maybe match.
     * @return A rule.
     */
    protected fun opt(rule: Rule): Rule {
        return OptionalRule.create(rule)
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
            return originalRule.withRule(
                autoWhitespaceHelper(originalRule.rule, whitespaceRule)
            )
        }
        if (originalRule is RuleWithManyChildren) {
            val rules = originalRule
                .rules
                .map { p: Rule -> autoWhitespaceHelper(p, whitespaceRule) }
                .toList()
            return originalRule.withRules(rules)
        }
        if (originalRule is Terminal) {
            val rules = mutableListOf<Rule>()
            rules.add(whitespaceRule)
            val result: Rule
            if (originalRule.reduction.type != RedType.INITIAL
                || originalRule.reduction.type != RedType.MERGE) {
                // Hide the terminal in the output.
                // It still appears in the tree, but is flattened into the concatenation.
                rules.add(
                    originalRule.withReduction(
                        Reduction.merging()
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
        throw IllegalArgumentException(originalRule.javaClass.name)
    }

    private fun autoWhitespace(
        productions: LinkedHashMap<Sym?, Rule?>,
        start: Sym,
        grammarWS: Grammar,
        startWS: Sym
    ) {
        val wsRule =
            opt(nt(startWS)).hideInResult()

        for (symRuleEntry in productions.entries) {
            symRuleEntry.setValue(
                autoWhitespaceHelper(
                    symRuleEntry.value!!, wsRule
                )
            )
        }

        val startWithoutReduction = (productions[start]!!
            .withReduction(Reduction.initial()))
        val newStartComb =
            cat(listOf(startWithoutReduction, wsRule))
                .withReduction(productions[start]!!.reduction)

        productions[start] = newStartComb
        productions.putAll(grammarWS)
        productions[startWS] = grammarWS.getProduction(startWS)!!.mergeToTop()
    }

    /**
     * Buffers a rule for the lifetime of this builder.
     *
     * @param rule The rule.
     * @param <T>  The type of the rule.
     * @return The same rule or a previously buffered equivalent.
    </T> */
    protected fun <T : Rule?> buffer(rule: T): T {
        @Suppress("UNCHECKED_CAST")
        return when (rule) {
            is StringTerm -> buffer.getOrAdd(rule as StringTerm) as T
            is RegexTerm -> buffer.getOrAdd(rule as RegexTerm) as T
            is NonTerminal -> buffer.getOrAdd(rule as NonTerminal) as T
            else -> rule
        }
    }
}
