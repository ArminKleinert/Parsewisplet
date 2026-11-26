package de.kleinert.parsewisp.grammar

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.*

/**
 * A type representing a Grammar.
 *
 */
class Grammar(startSym: Sym, m: LinkedHashMap<out Sym, out Rule>) : LinkedHashMap<Sym, Rule>(m.size, 1.0f) {
    /**
     * Return the starting production. This might be null.
     *
     * @return The starting production key.
     */
    /**
     * Staring symbol of this grammar. Can be overridden when the Grammar is used in a parser.
     *
     */
    val startSym: Sym

    init {
        require(m.isNotEmpty()) { "Empty grammar." }

        super.put(startSym, m.get(startSym)!!)
        super.putAll(m)

        this.startSym = startSym
    }

    override fun equals(o: Any?): Boolean {
        if ((o !is Grammar) || o.size != size || (this.startSym != o.startSym)) return false

        for (symRuleEntry in entries) {
            if (o.getProduction(symRuleEntry.key) != symRuleEntry.value) {
                return false
            }
        }

        return true
    }

    /**
     * Gets the right-hand side of the production associated with the key.
     *
     * @param key The right-hand side of the production.
     * @return The production or null if not found.
     */
    fun getProduction(key: Sym): Rule? {
        return getOrDefault(key, null)
    }

    override fun get(key: Sym): Rule? {
        throw UnsupportedOperationException("Please use getProduction(key) instead of get(key).")
    }

    override fun toString(): String {
        return super.toString()
    }

    /**
     * Returns some information about the Grammar.
     *
     * @return An instance of [GrammarInfo].
     * @see GrammarInfo
     *
     */
    fun analyze(): GrammarInfo {
        return GrammarInfo(this)
    }

    /**
     * Holds some information about a grammar.
     *
     * @param grammar The grammar.
     */
    data class GrammarInfo(val grammar: Grammar) {
        /**
         * All non-terminals of the Grammar.
         *
         * @return A collection of all non-terminals of the Grammar.
         */
        fun definedNTs(): Collection<Sym> {
            return grammar.keys
        }

        /**
         * All non-terminals which appear on the right-hand side of any production.
         *
         * @return A collection of all non-terminals which appear on the right-hand side of any production.
         */
        fun usedNTs(): Set<Sym> {
            return collect({ it: Rule -> it is NonTerminal })
                .map { it: Rule -> (it as NonTerminal).keyword }
                .toSet()
        }

        /**
         * Returns a collection of all rules that match the predicate.
         *
         * <pre>
         * `var parser = Parsewisp.parser("""                     S = A*                     A = "a" | "b"                     """);     var grammarInfo = parser.grammar().analyze();     System.out.println(grammarInfo.collect(rule -> rule instanceof Terminal));       // [a, b]     System.out.println(grammarInfo.collect(rule -> rule instanceof ZeroOrMoreRule)); // [A*] `
        </pre> *
         *
         * @param collector The predicate.
         * @return A collection of rules that match the predicate.
         */
        fun collect(collector: (Rule) -> Boolean): Set<Rule> {
            val result: MutableSet<Rule> = HashSet()
            val analyzedRules = mutableSetOf<Rule>()
            val ruleStack = grammar.values.toMutableList()
            var rule: Rule

            while (!ruleStack.isEmpty()) {
                rule = ruleStack.removeAt(ruleStack.size - 1)
                if (analyzedRules.contains(rule)) {
                    continue
                }
                analyzedRules.add(rule)

                if (rule is RuleWithManyChildren) {
                    ruleStack.addAll(rule.rules)
                } else if (rule is RuleWithChild) {
                    ruleStack.add(rule.rule)
                }

                if (collector(rule)) {
                    result.add(rule)
                }
            }

            return result
        }

        val unusedNTs: Collection<Sym>
            /**
             * Returns all NTs that are defined but unused. Effectively, this is a list of unused productions.
             *
             *
             * Note that the implementation makes no guarantees about the type, except that it's a [List] or [Set].
             * However, each key appears only once in the collection.
             *
             *
             *
             * Example:
             * S : 'a' 'b'
             * A : 'c'
             * The key "A" is defined, but never used.
             *
             *
             * @return A collection of unused non-terminals.
             */
            get() {
                val usedNTs = usedNTs()
                return definedNTs()
                    .filter { !usedNTs.contains(it) }
                    .filter { it != grammar.startSym }
                    .toSet()
            }

        val undefinedUsedNTs: Collection<Sym>
            /**
             * Returns a Collection of keys that are used, but not defined. For any valid grammar, the returned collection is empty.
             *
             *
             * Example:
             * S : A 'b'
             * The key "A" is used, but never defined.
             *
             *
             * @return A collection which is hopefully empty.
             */
            get() {
                val definedNTs = definedNTs()
                return usedNTs().filter { it: Sym? -> !definedNTs.contains(it) }.toSet()
            }

        val isValid: Boolean
            /**
             * Checks whether the Grammar is valid.
             *
             * @return true or false
             * @see GrammarInfo.undefinedUsedNTs
             */
            get() = this.undefinedUsedNTs.isEmpty()
                    && definedTerminals().isNotEmpty()

        /**
         * Collects all terminals and equivalent rules. If a rule occurs multiple times in the grammar, it still appears only once in the resulting collection.
         *
         * @return A collection of all terminals and special sequences (functions).
         */
        fun definedTerminals(): Set<Rule> {
            return collect({ it: Rule -> it is Terminal || it is SpecialSequenceRule })
        }

        /**
         * Returns a Set of rules that are reachable from the input production name.
         *
         * @param start The production from which to analyze.
         * @return A Set of reachable rules.
         */
        fun subGrammar(start: Sym): Grammar {
            val startRule = grammar.getProduction(start)

            requireNotNull(startRule) { "Symbol not in grammar: " + start }

            val visited = mutableSetOf<Rule>()
            val stack = mutableListOf<Rule>()
            stack.add(startRule)

            val reachable = LinkedHashSet<Sym>()
            reachable.add(start)

            while (!stack.isEmpty()) {
                val rule = stack.removeAt(stack.size - 1)

                // If already in set -> Ignore
                if (!visited.add(rule)) continue

                if (rule is RuleWithManyChildren) {
                    stack.addAll(rule.rules)
                } else if (rule is RuleWithChild) {
                    stack.add(rule.rule)
                } else if (rule is NonTerminal) {
                    val key = rule.keyword
                    val prod = grammar.getProduction(key)
                    requireNotNull(prod) { "Symbol not in grammar: $start" }
                    stack.add(prod)
                    reachable.add(key)
                }
            }

            val subGrammar = linkedMapOf<Sym, Rule>()
            for (sym in reachable) {
                subGrammar[sym] = grammar.getProduction(sym)!!
            }

            return Grammar(start, subGrammar)
        }

        /**
         * Return true if the grammar can terminate for the given starting symbol.
         *
         * Sometimes, a grammar can be non-productive, meaning that it would technically not produce an output for any input. For example, the following:
         * <pre>
         * `S = A S     A = epsilon `
         * </pre>
         *
         * With a naive algorithm, the parser could never terminate. You can check for this case by using the following:
         * <pre>
         * `var parser1 = Parsewisp.parser("S = A S ; A = epsilon ;");     var analysis1 = parser1.grammar().analyze();     System.out.println(analysis1.isProductive(Sym.sym("S"))); // false `
         * </pre>
         *
         * @param start The starting symbol.
         * @return true or false
         */
        fun isProductive(start: Sym): Boolean {
            return isProductive(start, mutableMapOf())
        }

        private fun isProductive(start: Sym, productive: MutableMap<Sym?, Boolean?>): Boolean {
            var changed: Boolean

            if (productive.containsKey(start)) return productive.get(start)!!

            do {
                changed = false
                for (entry in grammar.entries) {
                    val nt = entry.key
                    val rule = entry.value

                    productive.putIfAbsent(nt, false)

                    if (productiveRule(rule, productive) && !productive.get(nt)!!) {
                        productive.put(nt, true)
                        changed = true
                    }
                }
            } while (changed)
            return productive.getOrDefault(start, false)!!
        }

        private fun productiveRule(rule: Rule, productive: MutableMap<Sym?, Boolean?>): Boolean {
            if (rule is Terminal || rule is OptionalRule || rule is ZeroOrMoreRule) {
                return true
            } else if (rule is SpecialSequenceRule) {
                return true
            } else if (rule is NonTerminal) {
                return productive.getOrDefault(rule.keyword, false)!!
            } else if (rule is ConcatRule) {
                return rule.rules.all { it: Rule? -> productiveRule(it!!, productive) }
            } else if (rule is AlternationRule || rule is OrderedChoiceRule) {
                return rule.rules.any { it: Rule? -> productiveRule(it!!, productive) }
            } else if (rule is OnceOrMoreRule || rule is LookaheadRule || rule is NegativeLookaheadRule) {
                return productiveRule(rule.rule, productive)
            } else if (rule is ExclusionRule) {
                return productiveRule(rule.expected, productive)
                        && productiveRule(rule.excluded, productive)
            } else if (rule is VariableRepetitionRule) {
                return rule.min == 0 || productiveRule(rule, productive)
            } else {
                throw IllegalArgumentException("Can not handle value " + rule + " of type " + rule.javaClass + ".")
            }
        }

        override fun toString(): String {
            return "GrammarInfo[" +
                    "definedNTs=" + definedNTs() +
                    ", usedNTs=" + usedNTs() +
                    ", unusedNTs=" + this.unusedNTs +
                    ", undefinedUsedNTs=" + this.undefinedUsedNTs +
                    ", isValid=" + this.isValid +
                    ']'
        }
    }
}
