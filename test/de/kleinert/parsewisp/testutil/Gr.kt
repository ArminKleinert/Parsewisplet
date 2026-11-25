package de.kleinert.parsewisp.testutil

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.parsing.Rule

/**
 * Helper class for creating Grammars for test cases.
 */
object Gr {
    /**
     * Helper method for creating Grammars for test cases.
     * @param g Input.
     * @return A grammar.
     */
    fun grammar(vararg g: Any?): Grammar {
        require(!(g.size == 0 || (g.size and 1) == 1))
        val lhm = LinkedHashMap<Sym, Rule>()
        var start: Sym? = null
        val iter = g.iterator()
        do {
            val key = Sym.sym(iter.next() as String)
            if (start == null) start = key
            val `val` = iter.next() as Rule
            lhm.put(key, `val`)
        } while (iter.hasNext())
        return Grammar(start, lhm)
    }

    /**
     * Helper method for creating Grammars for test cases.
     * @param startName Name of the start sym.
     * @param g Input.
     * @return A grammar.
     */
    fun grammarS(startName: String, vararg g: Any?): Grammar {
        require(!(g.size == 0 || (g.size and 1) == 1))
        val lhm = LinkedHashMap<Sym, Rule>()
        val start = sym(startName)
        val iter = g.iterator()
        do {
            val key = Sym.sym(iter.next() as String)
            val `val` = iter.next() as Rule
            lhm.put(key, `val`)
        } while (iter.hasNext())
        return Grammar(start, lhm)
    }
}
