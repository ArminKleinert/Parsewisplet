package de.kleinert.parsewisp.grammar

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.NonTerminal
import de.kleinert.parsewisp.parsing.RegexTerm
import de.kleinert.parsewisp.parsing.Rule
import de.kleinert.parsewisp.parsing.StringTerm

/**
 * As long as this buffer exists, the following is true:
 * `if (Objects.equals(x, y)) { buffer.getOrAdd(x) == buffer.getOrAdd(y); }`
 */
internal class BufferForRules {
    private val nonTerminalMap: MutableMap<NonTerminal?, NonTerminal?> = mutableMapOf()

    /**
     * Adds a [NonTerminal]. If the object was already present in the buffer, returns the old one.
     * @param nonTerminal A rule.
     * @return The input or a previously buffered equivalent.
     */
    fun getOrAdd(nonTerminal: NonTerminal): NonTerminal {
        val temp = nonTerminalMap.putIfAbsent(nonTerminal, nonTerminal)
        return if (temp == null) nonTerminal else temp
    }

    private val regexTermMap: MutableMap<RegexTerm?, RegexTerm?> = mutableMapOf()

    /**
     * Adds a [RegexTerm]. If the object was already present in the buffer, returns the old one.
     * @param rule A rule.
     * @return The input or a previously buffered equivalent.
     */
    fun getOrAdd(rule: RegexTerm): RegexTerm {
        val temp = regexTermMap.putIfAbsent(rule, rule)
        return if (temp == null) rule else temp
    }

    private val stringTermMap: MutableMap<StringTerm?, StringTerm?> = mutableMapOf()

    /**
     * Adds a [StringTerm]. If the object was already present in the buffer, returns the old one.
     * @param rule A rule.
     * @return The input or a previously buffered equivalent.
     */
    fun getOrAdd(rule: StringTerm): StringTerm {
        val temp = stringTermMap.putIfAbsent(rule, rule)
        return if (temp == null) rule else temp
    }

    private val symToNtSet: MutableMap<Sym?, NonTerminal> = mutableMapOf()

    /**
     * Adds a [NonTerminal] based on the keyword. If the object was already present in the buffer, returns the old one.
     * @param keyword The key for a [NonTerminal].
     * @return The input or a previously buffered equivalent.
     * @see NonTerminal.create
     */
    fun getOrAddNt(keyword: Sym): NonTerminal {
        return symToNtSet.computeIfAbsent(keyword) { keyword: Sym?-> NonTerminal.create(keyword!!) }
    }

    private val stringCsTerms: MutableMap<String?, Rule> = mutableMapOf()
    private val stringCiTerms: MutableMap<String?, Rule> = mutableMapOf()

    /**
     * Adds a [Rule] based on the string. If the object was already present in the buffer, returns the old one.
     * @param string A string.
     * @param caseInsensitive Whether the terminal will be case-insensitive.
     * @return A [StringTerm] or whatever [StringTerm.create] would return for the same inputs.
     * @see StringTerm.create
     */
    fun getOrAddString(string: String, caseInsensitive: Boolean): Rule {
        if (caseInsensitive) {
            return stringCiTerms.computeIfAbsent(string) { s: String? -> StringTerm.create(s!!, true) }
        } else {
            return stringCsTerms.computeIfAbsent(string) { s: String? -> StringTerm.create(s!!, false) }
        }
    }

    private val regexTermMap1: MutableMap<Regex, Rule> = mutableMapOf()

    /**
     * Adds a [Rule] based on the string. If the object was already present in the buffer, returns the old one.
     * @param pattern A pattern (regex).
     * @return A [RegexTerm] or whatever [RegexTerm.create] would return for the same inputs.
     * @see RegexTerm.create
     */
    fun getOrAddRegex(pattern: Regex): Rule {
        return regexTermMap1.computeIfAbsent(pattern) { regexp: Regex? -> RegexTerm.create(regexp!!) }
    }
}
