package de.kleinert.parsewisp.parser_options

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.parsing.Rule
import de.kleinert.parsewisp.reduction.Reduction

/**
 * Options for unhiding parts of the output from a parse. A thorough description can be found in the description of the [ParsingOptions] class.
 *
 *
 * As an example, take the grammar
 * <pre>
 * `S   = 'a' <B> C <D> 'a'     B   = 'b'+     <C> = 'c'     <D> = 'd' `
</pre> *
 * Now, parsing the text `"abcda"`, the expected tree would be `[:S, "a", "c", "a"]`. Where the `B` subtree is completely hidden, `C` is flattened (merged into `S`) and `D` is also completely hidden.
 * <pre>
 * `var p = Parsewisp.parser("S = 'a' <B> C <D> 'a'\nB = 'b'+\n<C> = 'c'\n<D> = 'd'");     Parsewisp.parse(p, "abcda", ParsingOptions.getDefault().withUnhide(UnhideOptions.NONE));    // [:S, "a", "c", "a"]     Parsewisp.parse(p, "abcda", ParsingOptions.getDefault().withUnhide(UnhideOptions.TAGS));    // [:S, "a", [:C, "c"], "a"]     Parsewisp.parse(p, "abcda", ParsingOptions.getDefault().withUnhide(UnhideOptions.CONTENT)); // [:S, "a", [:B, "b"], "c", "d", "a"]     Parsewisp.parse(p, "abcda", ParsingOptions.getDefault().withUnhide(UnhideOptions.ALL));     // [:S, "a", [:B, "b"], [:C, "c"], [:D, "d"], "a"] `
</pre> *
 *
 * @see ParsingOptions.unhide
 */
object Unhide {
    /**
     * Applies [Rule.includeInResult] to all entries in the grammar.
     *
     * @param grammar The grammar.
     * @return The new grammar.
     * @see ParsingOptions.unhide
     */
    fun unhideContent(grammar: Grammar): Grammar {
        val res = linkedMapOf<Sym, Rule>()
        for (symRuleEntry in grammar.entries) {
            res.put(symRuleEntry.key, symRuleEntry.value.includeInResult())
        }
        return Grammar(grammar.startSym, res)
    }

    /**
     * Applies the reduction-type to all entries in the grammar.
     *
     * @param grammar The grammar.
     * @return The new grammar.
     */
    fun unhideTags(grammar: Grammar): Grammar {
        val res = linkedMapOf<Sym, Rule>()
        for (symRuleEntry in grammar.entries) {
            val key = symRuleEntry.key
            val reduction: Reduction = Reduction.taggedTree(key)
            val pUnhide = symRuleEntry.value.withReduction(reduction)
            res.put(key, pUnhide)
        }
        return Grammar(grammar.startSym, res)
    }

    /**
     * Applies the reduction-type to all entries in the grammar
     * and applies [Rule.includeInResult].
     *
     * @param grammar The grammar.
     * @return The new grammar.
     */
    fun unhideAll(grammar: Grammar): Grammar {
        val res = linkedMapOf<Sym, Rule>()
        for (symRuleEntry in grammar.entries) {
            val key = symRuleEntry.key
            val reduction: Reduction = Reduction.taggedTree(key)
            val p = symRuleEntry.value.includeInResult().withReduction(reduction)
            res.put(key, p)
        }
        return Grammar(grammar.startSym, res)
    }
}
