package de.kleinert.parsewisp.grammar

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.AlternationRule
import de.kleinert.parsewisp.parsing.ConcatRule
import de.kleinert.parsewisp.parsing.EOFTerm
import de.kleinert.parsewisp.parsing.EpsilonTerm
import de.kleinert.parsewisp.parsing.ExclusionRule
import de.kleinert.parsewisp.parsing.LookaheadRule
import de.kleinert.parsewisp.parsing.NegativeLookaheadRule
import de.kleinert.parsewisp.parsing.NonTerminal
import de.kleinert.parsewisp.parsing.OnceOrMoreRule
import de.kleinert.parsewisp.parsing.OptionalRule
import de.kleinert.parsewisp.parsing.OrderedChoiceRule
import de.kleinert.parsewisp.parsing.RegexTerm
import de.kleinert.parsewisp.parsing.Rule
import de.kleinert.parsewisp.parsing.SimpleRule
import de.kleinert.parsewisp.parsing.SpecialSequenceRule
import de.kleinert.parsewisp.parsing.StringTerm
import de.kleinert.parsewisp.parsing.TransformRule
import de.kleinert.parsewisp.parsing.ValueRangeTerm
import de.kleinert.parsewisp.parsing.VariableRepetitionRule
import de.kleinert.parsewisp.parsing.ZeroOrMoreRule
import java.util.*
import java.util.stream.Collectors

/**
 * This class implements methods to provide pretty formatting for grammars. It can be overridden for other grammar formats.
 * By default, the default printer can be used as follows to print a grammar:
 * <pre>
 * `var p = Parsewisp.parser(     """     S = P S | eps     P = "(" S* ")"     """); // A variant of the grammar of balanced parentheses System.out.println(p.show()); // Use default printer. `
</pre> *
 * The code above prints
 * <pre>
 * `S = P S | ε ; P = "(" S* ")" ; `
</pre> *
 * To use a different printer, it can be included in the options when creating a parser, or be invoked later.
 * <pre>
 * `var p = Parsewisp.parser(     "S = P S | eps ; P = \"(\" S* \")\" ;",     ParserCreationOptions.getDefault().withPrinter(new GrammarPrinter())); // << Or pass another printer System.out.println(new GrammarPrinter().toString(p.grammar())); // Use specific printer. `
</pre> *
 *
 *
 * To implement your own printer, you need to override the methods as follows:
 * <table>
 * <caption>A table of rules/elements of a grammar with the associated methods in the printer.</caption>
 * <tbody>
 * <tr><th>Element/Class</th><th>Method</th><th>Default</th></tr>
 * <tr><td>Production</td><td>[.productionToString]</td><td>`name = ... ;`</td></tr>
 * <tr><td>[AlternationRule]</td><td>[.altToString]</td><td>`rule1 | ... | ruleN` (Rules divided by bars)</td></tr>
 * <tr><td>[ConcatRule]</td><td>[.catToString]</td><td>`rule1 ... ruleN` (rules separated by spaces)</td></tr>
 * <tr><td>[EOFTerm]</td><td>[.eofToString]</td><td>`EOF`</td></tr>
 * <tr><td>[EpsilonTerm]</td><td>[.epsToString]</td><td>`ε`</td></tr>
 * <tr><td>[ExclusionRule]</td><td>[.exclusionToString]</td><td>`expectedRule - excludedRule` (rule 1 and 2 divided by a dash)</td></tr>
 * <tr><td>[LookaheadRule]</td><td>[.lookToString]</td><td>`&rule` (AND-symbol followed by the rule)</td></tr>
 * <tr><td>[NegativeLookaheadRule]</td><td>[.negToString]</td><td>`!rule` (BANG-symbol followed by the rule)</td></tr>
 * <tr><td>[NonTerminal]</td><td>[.ntToString]</td><td>The name.</td></tr>
 * <tr><td>[OnceOrMoreRule]</td><td>[.onceOrMoreToString]</td><td>`rule+` (The rule followed by a "+")</td></tr>
 * <tr><td>[OptionalRule]</td><td>[.optToString]</td><td>`rule?` (The rule followed by a "?")</td></tr>
 * <tr><td>[OrderedChoiceRule]</td><td>[.ordToString]</td><td>`rule1 / ... / ruleN` (Rules separated by slashes)</td></tr>
 * <tr><td>[RegexTerm]</td><td>[.regexpToString]</td><td>`#"..."` (Hash-symbol and then the pattern between double-quotes)</td></tr>
 * <tr><td>[SpecialSequenceRule]</td><td>[.specialToString]</td><td>`?...?` (The description between question-marks)</td></tr>
 * <tr><td>[StringTerm]</td><td>[.strTermToString]</td><td>`"..."` (The string in double-quotes)</td></tr>
 * <tr><td>[TransformRule]</td><td>[.transformerToString]</td><td>No special notation.</td></tr>
 * <tr><td>[ValueRangeTerm]</td><td>[.valRangeToString]</td><td>`%x...-...` (ABNF hexadecimal format)</td></tr>
 * <tr><td>[VariableRepetitionRule]</td><td>[.repToString]</td><td>`rule{n,m}` (minimum STAR maximum followed by the rule)</td></tr>
 * <tr><td>[ZeroOrMoreRule]</td><td>[.zeroOrMoreToString]</td><td>`rule*` (The rule followed by a "*")</td></tr>
</tbody> *
</table> *
 *
 * @since 0.9.7
 */
interface GrammarPrinter {
    /**
     * Runs [.productionToString] on each rule in the grammar. The starting rule is always printed first.
     *
     * @param grammar The grammar.
     * @return A string.
     * @since 0.9.7
     */
    fun toString(grammar: Grammar): String {
        val sb = StringBuilder()
        val start = grammar.startSym
        sb.append(productionToString(start, Objects.requireNonNull<Rule?>(grammar.getProduction(start))!!))
        for (symRuleEntry in grammar.entries) {
            if (symRuleEntry.key != start) {
                sb.append('\n')
                sb.append(productionToString(symRuleEntry.key, symRuleEntry.value))
            }
        }
        return sb.toString()
    }

    /**
     * Escapes a string. The terminator is a codepoint which is always escaped.
     * Otherwise, the escaped codepoints are `\b`, `\n`, `\t`, `\f`, `\r`, `\\`.
     * The intention is to escape only the symbols that need to be escaped. For example, a grammar may use single quotes to represent strings (as in `'...'`). In such a case, this method should be invoked with `'\''` as its second argument to escape any single quote in the string.
     * <pre>
     * `printer.escape("'abc'", '\''); // => \'abc\' printer.escape("\"a88b\nc\"\\", '\''); // => "a88b\nc"\\ `
    </pre> *
     *
     * @param s                   The string.
     * @param terminatorCodepoint The terminator.
     * @return A string.
     * @since 0.9.7
     */
    fun escape(s: String, terminatorCodepoint: Int): String {
        val sb = StringBuilder()
        val codeIter = s.codePoints().iterator()
        while (codeIter.hasNext()) {
            val i = codeIter.nextInt()
            if (i == terminatorCodepoint) {
                sb.append('\\').appendCodePoint(terminatorCodepoint)
            } else {
                when (i) {
                    '\b'.code -> sb.append("\\b")
                    '\n'.code -> sb.append("\\n")
                    '\t'.code -> sb.append("\\t")
                    '\u000c'.code -> sb.append("\\f")
                    '\r'.code -> sb.append("\\r")
                    '\\'.code -> sb.append("\\\\")
                    else -> sb.appendCodePoint(i)
                }
            }
        }
        return sb.toString()
    }

    /**
     * Precedences for rule types. The output is used to determine whether a rule needs to be wrapped in parentheses when printing.
     * For example, a zero-or-more repetition of the rule `A | B` would be printed as `(A | B)*` because the alternation has a higher precedence.
     *
     *
     * The order of precedence (descending) is:
     *
     *  1. [AlternationRule] and [OrderedChoiceRule]
     *  1. [ConcatRule]
     *  1. [ExclusionRule]
     *  1. [LookaheadRule] and [NegativeLookaheadRule]
     *  1. [OptionalRule], [OnceOrMoreRule] and [ZeroOrMoreRule]
     *  1. [VariableRepetitionRule]
     *  1. Any rule that can not wrap other rules. In other words: [de.kleinert.parsewisp.parsing.Terminal], [NonTerminal] or [SpecialSequenceRule]
     *
     *
     * @param rule the rule of which the precedence is needed.
     * @return An int.
     * @since 0.9.7
     */
    fun precedence(rule: Rule): Int {
        if (rule is AlternationRule || rule is OrderedChoiceRule || rule is TransformRule) return 100
        if (rule is ConcatRule) return 80
        if (rule is ExclusionRule) return 70
        if (rule is LookaheadRule || rule is NegativeLookaheadRule) return 60
        if (rule is OptionalRule || rule is OnceOrMoreRule || rule is ZeroOrMoreRule) return 50
        if (rule is VariableRepetitionRule) return 40
        if (rule is SimpleRule) return 10
        throw IllegalArgumentException()
    }

    /**
     * Prints a production in the format `sym = rhs ;`.
     *
     * @param sym The production's name.
     * @param rhs The rule on the right-hand-side of the separator.
     * @return A string.
     * @since 0.9.7
     */
    fun productionToString(sym: Sym, rhs: Rule): String {
        return sym.name() + " = " + ruleToString(rhs) + " ;"
    }

    fun parens(current: Rule, ruleToPrint: Rule): String {
        if (precedence(current) <= precedence(ruleToPrint)) return "(" + ruleToString(ruleToPrint) + ")"
        return ruleToString(ruleToPrint)
    }

    /**
     * This method just delegates the printing to a specific rule for the type of the input.
     *
     * @param rule The rule.
     * @return A string.
     * @since 0.9.7
     */
    fun ruleToString(rule: Rule): String {
        val sb = StringBuilder()

        if (rule.isHiddenInResult1()) sb.append('<')

        val s: String = when (rule) {
            is AlternationRule -> altToString(rule)
            is OrderedChoiceRule -> ordToString(rule)
            is ConcatRule -> catToString(rule)
            is ExclusionRule -> exclusionToString(rule)
            is LookaheadRule -> lookToString(rule)
            is NegativeLookaheadRule -> negToString(rule)
            is OptionalRule -> optToString(rule)
            is OnceOrMoreRule -> onceOrMoreToString(rule)
            is ZeroOrMoreRule -> zeroOrMoreToString(rule)
            is VariableRepetitionRule -> repToString(rule)
            is NonTerminal -> ntToString(rule)
            is SpecialSequenceRule -> specialToString(rule)
            is EOFTerm -> eofToString(rule)
            is EpsilonTerm -> epsToString(rule)
            is RegexTerm -> regexpToString(rule)
            is StringTerm -> strTermToString(rule)
            is ValueRangeTerm -> valRangeToString(rule)
            is TransformRule -> transformerToString(rule)
            else -> throw IllegalArgumentException("Unsupported rule type: " + rule)
        }

        sb.append(s)

        if (rule.isHiddenInResult1()) sb.append('>')

        return sb.toString()
    }

    /**
     * Prints a [AlternationRule].
     *
     *
     * Default format: `rule1 | rule2 | ... | ruleN`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun altToString(rule: AlternationRule): String {
        return rule.rules.stream()
            .map<String?> { it: Rule? -> parens(rule, it!!) }
            .collect(Collectors.joining(" | "))
    }

    /**
     * Prints a [ConcatRule].
     *
     *
     * Default format: `rule1 rule 2 ... ruleN`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun catToString(rule: ConcatRule): String {
        return rule.rules.stream()
            .map<String?> { it: Rule? -> parens(rule, it!!) }
            .collect(Collectors.joining(" "))
    }

    /**
     * Prints a [EOFTerm].
     *
     *
     * Default format: `EOF`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun eofToString(
        @Suppress("unused") rule: EOFTerm
    ): String {
        return EOFTerm.text()
    }

    /**
     * Prints a [EpsilonTerm].
     *
     *
     * Default format: `ε`
     *
     * @param rule The rule.
     * @return A string-representation of the rule,
     * @since 0.9.7
     */
    fun epsToString(
        @Suppress("unused") rule: EpsilonTerm
    ): String {
        return "ε"
    }

    /**
     * Prints a [ExclusionRule].
     *
     *
     * Default format: `ruleExpected - ruleExcluded`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun exclusionToString(rule: ExclusionRule): String {
        return parens(rule, rule.expected) + " - " + parens(rule, rule.excluded)
    }

    /**
     * Prints a [LookaheadRule].
     *
     *
     * Default format: `&rule`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun lookToString(rule: LookaheadRule): String {
        return "&" + parens(rule, rule.rule)
    }

    /**
     * Prints a [NegativeLookaheadRule].
     *
     *
     * Default format: `!rule`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun negToString(rule: NegativeLookaheadRule): String {
        return "!" + parens(rule, rule.rule)
    }

    /**
     * Prints a [NonTerminal].
     *
     *
     * Default format: `rule` (just the name)
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun ntToString(rule: NonTerminal): String {
        return rule.keyword.name()
    }

    /**
     * Prints a [OnceOrMoreRule].
     *
     *
     * Default format: `rule+`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun onceOrMoreToString(rule: OnceOrMoreRule): String {
        return parens(rule, rule.rule) + "+"
    }

    /**
     * Prints a [OptionalRule].
     *
     *
     * Default format: `rule?`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun optToString(rule: OptionalRule): String {
        return parens(rule, rule.rule) + "?"
    }

    /**
     * Prints a [OrderedChoiceRule].
     *
     *
     * Default format: `rule1 / rule 2 / ... / ruleN`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun ordToString(rule: OrderedChoiceRule): String {
        return rule.rules.stream()
            .map<String?> { it: Rule? -> parens(rule, it!!) }
            .collect(Collectors.joining(" / "))
    }

    /**
     * Prints a [RegexTerm].
     *
     *
     * Default format: `#"text"`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun regexpToString(rule: RegexTerm): String {
        return "#\"" + escape(rule.regexp.pattern(), '"'.code) + "\""
    }

    /**
     * Prints a [SpecialSequenceRule].
     *
     *
     * Default format: `?description?`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun specialToString(rule: SpecialSequenceRule): String {
        return "?" + rule.description + "?"
    }

    /**
     * Prints a [StringTerm].
     *
     *
     * Default format: `"text"`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun strTermToString(rule: StringTerm): String {
        val sb = StringBuilder()
        if (rule.isCaseInsensitive) sb.append('%').append('i')
        sb.append('"').append(escape(rule.string, '"'.code)).append('"')
        return sb.toString()
    }

    /**
     * Prints a [ValueRangeTerm] (range of Unicode characters).
     *
     *
     * Default format: `%xX-X` or `%xX`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun valRangeToString(rule: ValueRangeTerm): String {
        if (rule.lo == rule.hi) return String.format("%%x%X", rule.lo)
        return String.format("%%x%X-%X", rule.lo, rule.hi)
    }

    /**
     * Prints a [TransformRule].
     *
     *
     * Default format: `rule`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun transformerToString(rule: TransformRule): String {
        return ruleToString(rule)
    }

    /**
     * Prints a [VariableRepetitionRule].
     *
     *
     * Default format: `rule{min,max}`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun repToString(rule: VariableRepetitionRule): String {
        return parens(rule, rule.rule) + "{" + rule.min + "," + rule.max + "}"
    }

    /**
     * Prints a [ZeroOrMoreRule].
     *
     *
     * Default format: `rule*`
     *
     * @param rule The rule.
     * @return A string-representation of the rule.
     * @since 0.9.7
     */
    fun zeroOrMoreToString(rule: ZeroOrMoreRule): String {
        return parens(rule, rule.rule) + "*"
    }

    companion object {
        val default: GrammarPrinter
            /**
             * Returns a default GrammarPrinter.
             *
             * @return A GrammarPrinter.
             */
            get() = object : GrammarPrinter {
                override fun equals(other: Any?): Boolean {
                    return other != null && javaClass == other.javaClass
                }

                override fun hashCode(): Int {
                    return Objects.hashCode(javaClass)
                }

                override fun toString(): String {
                    return javaClass.toString()
                }
            }
    }
}
