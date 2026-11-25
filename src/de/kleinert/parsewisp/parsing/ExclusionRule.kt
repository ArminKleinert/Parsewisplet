package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.result.success.ParseMessage
import java.lang.ref.WeakReference
import java.util.function.Predicate

/**
 * Describes the "Syntactic exception" or "except" operator. That is the `(p1 - p2)` operator in EBNF (where p1 and p2 are instances of [Rule]).
 *
 *
 * Notation: `rule1 - rule2`  (match rule1 except if it also matches rule2)
 *
 *
 * Example
 * <pre>
 * `var p = Parsewisp.parser("S := #'[0-9]+' - '11'"); // Any positive number except 11.     println(p.parse("12"));  // [:S, 12]     println(p.parse("11"));  // Failure `
</pre> *
 * See also: [ISO/IEC 14977:1996](https://www.iso.org/standard/26153.html) and [an explanation on StackOverflow](https://stackoverflow.com/a/35138946).
 *
 * @since 0.9.7
 */
class ExclusionRule private constructor(
    hide: Boolean, red: ReductionType,
    /**
     * The rule that is expected. E.g. in the exclusion rule `(A - B)`, it would be `A`.
     *
     * @return The expected rule.
     */
    val expected: Rule,
    /**
     * The rule that is to be excluded. E.g. in the exclusion rule `(A - B)`, it would be `B`.
     *
     * @return The excluded rule.
     */
    val excluded: Rule
) : RuleWithManyChildren(hide, red, listOf(expected, excluded)) {
    // 0: Default (Complex grammar)
    // 1: Simple string terminal
    // 2: Regex terminal
    // 3: Alternation of string or regex terminals
    // 4: Concatenation of string or regex terminals
    private val mode: Int

    override fun parse(index: Int, runner: Gll) {
        val nodeKeyForThis = makeKey(index, this)

        runner.pushListener(
            makeKey(index, expected),
             { expectedSuccess: ParseMessage? ->
                val substring = runner.tramp().text.substring(index, expectedSuccess!!.index())
                if (reparsePart(substring, runner)) {
                    runner.fail(nodeKeyForThis, index, this, false)
                    return@pushListener
                }
                runner.pushSuccessAgainWithNewKey(nodeKeyForThis, expectedSuccess)
            })
    }

    override fun fullParse(index: Int, runner: Gll) {
        val nodeKeyForThis = makeKey(index, this)

        runner.pushListener(
            makeKey(index, expected),
             { expectedSuccess: ParseMessage? ->
                val substring = runner.tramp().text.substring(index, expectedSuccess!!.index())
                if (reparsePart(substring, runner)) {
                    runner.fail(nodeKeyForThis, index, this, true)
                    return@pushListener
                }
                runner.pushSuccessAgainWithNewKey(nodeKeyForThis, expectedSuccess)
            })
    }

    private var referenceForBufferedElements: WeakReference<Gll?>? = null
    private var modifiedExclusionGrammar: Grammar? = null
    private var modifiedExclusionStart: Sym? = null

    init {
        val stringOrRegexPredicate =
            Predicate { it: Rule? ->
                it is StringTerm
                        || it is RegexTerm
                        || it is EpsilonTerm
            }

        when (excluded) {
            is StringTerm -> {
                mode = 1
            }

            is RegexTerm -> {
                mode = 2
            }

            is AlternationRule if excluded.rules.stream().allMatch(stringOrRegexPredicate) -> {
                mode = 3
            }

            is ConcatRule if excluded.rules.stream().allMatch(stringOrRegexPredicate) -> {
                mode = 4
            }

            is EpsilonTerm -> {
                mode = 5
            }

            else -> {
                mode = 0
            }
        }
    }

    private fun reparsePart(subs: String, runner: Gll): Boolean {
        return when (mode) {
            1, 2, 5 ->  // String mode, Regex mode, Epsilon mode.
                reparseStrOrRegex(subs, excluded)

            3 -> {
                // If any of the rules in the exclusion match, the parse failed.
                for (r in (excluded as AlternationRule).rules) {
                    if (reparseStrOrRegex(subs, r)) return true
                }
                false
            }

            4 -> {
                // If all rules in the exclusion succeeded, the parse failed.
                for (r in (excluded as ConcatRule).rules) {
                    if (!reparseStrOrRegex(subs, r)) return false
                }
                true
            }

            else -> reparseFromMoreComplexGrammar(subs, runner)
        }
    }

    private fun reparseStrOrRegex(subs: String, r1: Rule): Boolean {
        if (r1 is StringTerm) {
            return if (r1.isCaseInsensitive)
                r1.string.equals(subs, ignoreCase = true)
            else
                (r1.string == subs)
        }
        if (r1 is RegexTerm) {
            return r1.regexp.matcher(subs).matches()
        }
        if (r1 is EpsilonTerm) {
            return subs.isEmpty()
        }
        throw IllegalStateException()
    }

    private fun reparseFromMoreComplexGrammar(subs: String, runner: Gll): Boolean {
        val oldGrammar = runner.tramp().grammar
        val startSymbol: Sym
        val grammar: Grammar

        if (referenceForBufferedElements != null && referenceForBufferedElements!!.get() == runner) {
            checkNotNull(modifiedExclusionGrammar)
            checkNotNull(modifiedExclusionStart)
            startSymbol = modifiedExclusionStart!!
            grammar = modifiedExclusionGrammar!!
        } else if (excluded is NonTerminal) {
            // The rule was already a non-terminal. Simply change starting production.
            startSymbol = excluded.keyword
            grammar = oldGrammar
        } else {
            // Find a new start production name which is not in the grammar yet.
            var tempSym: Sym
            if (!oldGrammar.containsKey(sym("RerunForExclusion0"))) {
                tempSym = sym("RerunForExclusion0")
            } else {
                var n = 0
                val nameBuilder = StringBuilder("RerunForExclusion")
                do {
                    nameBuilder.setLength(17)
                    n++
                    tempSym = sym(nameBuilder.append(n).toString())
                } while (oldGrammar.containsKey(tempSym))
            }
            startSymbol = tempSym

            // Make new Grammar
            val tempG = LinkedHashMap<Sym, Rule>(oldGrammar)
            tempG[startSymbol] = ConcatRule.create(listOf(excluded, EOFTerm.default))
            ReductionType.applyStandardReductionToProductions(tempG)

            grammar = Grammar(startSymbol, tempG)

            referenceForBufferedElements = WeakReference(runner)
            modifiedExclusionStart = startSymbol
            modifiedExclusionGrammar = grammar
        }
        return Gll.parse(grammar, startSymbol, subs,
            partial = false,
            iterativeDeepening = false,
            printer = runner.grammarPrinter
        ).isSuccess
    }

    override fun withHideTag(hide: Boolean): ExclusionRule {
        return if (isHiddenInResult == hide) this else ExclusionRule(hide, this.reduction, expected, excluded)
    }

    override fun withReduction(red: ReductionType): ExclusionRule {
        return if (reduction === red) this else ExclusionRule(isHiddenInResult, red, expected, excluded)
    }

    override fun withRules(rules: List<Rule>): ExclusionRule {
        require(rules.size == 2) { "Must pass exactly 2 arguments." }
        return ExclusionRule(isHiddenInResult, reduction, rules.get(0), rules.get(rules.size - 1))
    }

    companion object {
        /**
         * Create a new instance. Represents `(ruleExpected - ruleExcluded)`.
         * Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param ruleExpected The rule that must be matched.
         * @param ruleExcluded The rule that must not be matched.
         * @return A rule.
         * @since 0.9.7
         */
        fun create(
            ruleExpected: Rule,
            ruleExcluded: Rule
        ): Rule {
            if (ruleExpected == ruleExcluded) return EpsilonTerm.default
            return ExclusionRule(defaultHidden, defaultReductionType, ruleExpected, ruleExcluded)
        }
    }
}