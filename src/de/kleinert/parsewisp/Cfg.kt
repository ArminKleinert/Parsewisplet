package de.kleinert.parsewisp

import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.error.ParserCreationFailure
import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.grammar.GrammarBuilder
import de.kleinert.parsewisp.grammar.GrammarPrinter
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.parsing.EOFTerm
import de.kleinert.parsewisp.parsing.EpsilonTerm
import de.kleinert.parsewisp.parsing.Gll
import de.kleinert.parsewisp.parsing.Rule
import de.kleinert.parsewisp.result.Node
import de.kleinert.parsewisp.result.Node.NodeParseTree
import de.kleinert.parsewisp.result.ParseFailure
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.util.StrParser
import java.util.Map

/**
 * This class provides the means of creating a Grammar
 */
internal class Cfg private constructor(private val options: ParserCreationOptions) {
    private class GrammarBuild(
        options: ParserCreationOptions,
        var spec: String,
        var grammarGrammar: Grammar
    ) : GrammarBuilder(options.redefinitionOption) {
        fun parseRepMinMax(partsUncut: String): Long {
            var partsUncut = partsUncut
            partsUncut = partsUncut.substring(1, partsUncut.length - 1) // Cut away "{" and "}"
            var parts = partsUncut.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (partsUncut.isEmpty()) { // The input was only "{}"
                parts = arrayOf<String>("", "")
            } else if (parts.size == 1) {
                // Format at this point is "[0-9]+" or "[0-9]+," or ",[0-9]+" or "[0-9]+,[0-9]+"
                val temp = arrayOf<String>("", "")
                if (partsUncut.get(0) == ',') { // Only maximum provided (e.g. `p{,m}`)
                    temp[1] = parts[0]
                } else if (partsUncut.get(partsUncut.length - 1) == ',') { // Only minimum provided (e.g. `p{n,}`
                    temp[0] = parts[0]
                } else { // Only an exact number is given: Both minimum and maximum. (e.g. `p{n}`).
                    temp[0] = parts[0]
                    temp[1] = temp[0]
                }
                parts = temp
            } else require(parts.size <= 2) { "Invalid format for repetition rule: " + partsUncut }
            // If none of the cases were true, the input had the usual format [0-9]+\\*[0-9]+
            val min = if (parts[0].isBlank()) 0 else parts[0].toInt()
            val max = if (parts[1].isBlank()) Int.MAX_VALUE else parts[1].toInt()
            return (min.toLong() shl 32) or max.toLong()
        }

        fun buildRuleRule(
            tree: ParseTree
        ): MutableMap.MutableEntry<Sym?, Rule?> {
            val allContents = tree.getContent()
            val nt = allContents.get(0).content() as ParseTree
            val altOrOrd = allContents.get(1).content() as ParseTree
            var content = nt.getContent().get(0)

            val key: Sym
            val rule: Rule

            if (sym("hide-nt") == nt.getTag().content) {
                content = (content.content() as ParseTree).getContent().get(0)
                key = sym(content.content().toString())
                rule = (buildRule(altOrOrd) as Rule).mergeToTop()
            } else {
                key = sym(content.content() as String)
                rule = buildRule(altOrOrd) as Rule
            }

            return Map.entry<Sym?, Rule?>(key, rule)
        }

        private val strParser = StrParser()
        private val epsNames = mutableListOf<String?>("Epsilon", "epsilon", "EPSILON", "eps", "ε")

        fun buildRule(tree1: ParseTree): Any {
            var tree = tree1
            while (true) {
                if (tree.getTag() == ParseTree.NULL_TAG) {
                    tree = tree.getContent().get(0).content() as ParseTree
                    continue
                }

                val tag = tree.getTag().content.name()
                when (tag) {
                    "rule" -> {
                        return buildRuleRule(tree)
                    }

                    "ntOrEpsOrEof" -> {
                        val name = tree.getContent().get(0).content() as String
                        if (epsNames.contains(name)) return EpsilonTerm.getDefault()
                        if (name == EOFTerm.text()) return EOFTerm.getDefault()
                        return nt(sym(name))
                    }

                    "nt" -> {
                        val name = tree.getContent().get(0).content() as String
                        return nt(sym(name))
                    }

                    "paren" -> {
                        // The parse tree is wrapped in hidden "(" ")".
                        tree = tree.getContent().get(0).content() as ParseTree
                        continue  // Open up the grouping and take it to the top.
                    }

                    "alt" -> {
                        return alt(
                            tree
                                .getContent()
                                .stream()
                                .map<Rule?> { c: Node? -> buildRule(c!!.content() as ParseTree) as Rule }
                                .toList())
                    }

                    "ord" -> {
                        return ordAlt(
                            tree
                                .getContent()
                                .stream()
                                .map<Rule?> { c: Node? -> buildRule(c!!.content() as ParseTree) as Rule }
                                .toList())
                    }

                    "hide" -> {
                        return (buildRule(
                            (tree.getContent().get(0) as NodeParseTree).content
                        ) as Rule).hideInResult()
                    }

                    "cat" -> {
                        return cat(
                            tree
                                .getContent()
                                .stream()
                                .map<Rule?> { c: Node? -> buildRule(c!!.content() as ParseTree) as Rule }
                                .toList())
                    }

                    "string" -> {
                        val s = tree.getContent().get(0).content() as String
                        if (s.startsWith("%")) {
                            val caseInsensitive = when (s.get(1)) {
                                'i' -> true
                                's' -> false
                                else -> throw IllegalStateException()
                            }
                            return string(
                                strParser.processString(s.substring(2)),
                                caseInsensitive
                            )
                        }
                        return string(strParser.processString(s))
                    }

                    "regexp" -> {
                        return regex(
                            strParser.processRegexp(
                                tree.getContent().get(0).content() as String,
                                2, 1
                            )
                        )
                    }

                    "neg" -> {
                        return neg(
                            buildRule(
                                tree.getContent().get(0).content() as ParseTree
                            ) as Rule
                        )
                    }

                    "opt" -> {
                        return opt(
                            buildRule(
                                tree.getContent().get(0).content() as ParseTree
                            ) as Rule
                        )
                    }

                    "opt_rep" -> {
                        return zeroOrMore(
                            buildRule(
                                tree.getContent().get(0).content() as ParseTree
                            ) as Rule
                        )
                    }

                    "look" -> {
                        return look(
                            buildRule(
                                tree.getContent().get(0).content() as ParseTree
                            ) as Rule
                        )
                    }

                    "abnf-range" -> {
                        val content = tree.getContent()
                        val parts =
                            (content.get(0).content() as String).split("-".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        val prefix = parts[0] // "%b..."/"%d..."/"%x..."
                        val radix = when (prefix.get(1)) {
                            'b' -> 2
                            'd' -> 10
                            'x' -> 16
                            else -> throw ParserCreationFailure("Invalid format for value range.")
                        }
                        val rangeFirst = parts[0].substring(2).toInt(radix)
                        val rangeLast = if (parts.size == 1)
                            rangeFirst
                        else parts[1].toInt(radix)
                        return numVal(rangeFirst, rangeLast)
                    }

                    "epsilon" -> {
                        return EpsilonTerm.getDefault()
                    }

                    "exclude" -> {
                        val rule1 = buildRule(tree.getContent().get(0).content() as ParseTree) as Rule
                        if (tree.getContent().size == 1) return rule1

                        val rule2 = buildRule(tree.getContent().get(1).content() as ParseTree) as Rule
                        return exclude(rule1, rule2)
                    }

                    "factor" -> {
                        val ruleAndPostfixStringIter = tree.getContent().iterator()
                        var rule = buildRule(ruleAndPostfixStringIter.next()!!.content() as ParseTree) as Rule
                        while (ruleAndPostfixStringIter.hasNext()) {
                            val postfix = ruleAndPostfixStringIter.next()!!.string()
                            if (postfix == "?") {
                                rule = opt(rule)
                            } else if (postfix == "*") {
                                rule = zeroOrMore(rule)
                            } else if (postfix == "+") {
                                rule = onceOrMore(rule)
                            } else if (postfix.startsWith("{")) {
                                val minMax = parseRepMinMax(postfix)
                                try {
                                    rule = rep(rule, (minMax shr 32).toInt(), (minMax and 0x7FFFFFFFL).toInt())
                                } catch (e: IllegalArgumentException) {
                                    throw ParserCreationFailure(e)
                                }
                            } else {
                                throw IllegalArgumentException()
                            }
                        }
                        return rule
                    }
                }
                throw UnsupportedOperationException(tag)
            }
        }

        override fun make() {
            val rules = Gll.parse(
                grammarGrammar,
                sym("rules"),
                spec, false, false, GrammarPrinter.getDefault()
            )

            if (rules is ParseFailure) {
                throw ParserCreationFailure(
                    "Error parsing grammar specification:\n" + rules + "\n"
                )
            }

            for (rule in rules.castToParseSuccess().getContent()) {
                val sc = buildRuleRule(rule.content() as ParseTree)
                addProduction(sc.key!!, sc.value!!)
            }
        }
    }

    fun buildGrammar(
        spec: String,
        grammarGrammar: Grammar
    ): Grammar {
        return GrammarBuild(options, spec, grammarGrammar).buildWithWhitespace(
            null, null,
            options.doCheckCorrectness()
        )
    }

    companion object {
        /**
         * Returns a default creator.
         *
         * @param options Input options.
         * @return An instance of [Cfg].
         */
        fun make(options: ParserCreationOptions): Cfg {
            return Cfg(options)
        }
    }
}