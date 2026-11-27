package de.kleinert.parsewisp.reduction

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.Rule
import de.kleinert.parsewisp.result.ParseTree
import java.util.*

/**
 * A class for output formats. This entire class might be removed in the future and replaced by an alternative [ParseTree] variant specifically for intermediate operations.
 */
class Reduction private constructor(
    /**
     * The production key this output format is for.
     *
     * @return The production key this output format is for.
     */
    val key: Sym,
    /**
     * The [RedType] type of this reduction.
     *
     * @return The [RedType] type of this reduction.
     */
    val type: RedType
) {
    /**
     * The available types. They mark whether the parse operation is an intermediate operation or for the final output.
     */
    enum class RedType {
        /**
         * The default type. This should not be used directly.
         */
        INITIAL,

        /**
         * Has the same properties as [INITIAL], but will not be replaced with [TAGGED_PARSE_TREE] when finalizing the grammar.
         */
        MERGE,

        /**
         * Default type. Represent parse trees as lists.
         */
        TAGGED_PARSE_TREE
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Reduction
        return key == that.key
    }

    override fun hashCode(): Int {
        return Objects.hash(key, this.type)
    }

    override fun toString(): String {
        return "{" +
                "key=" + key +
                ", type=" + this.type +
                '}'
    }

    companion object {
        /**
         * The default output format for results that are to be merged into a parent tree. It has the following properties:
         *
         *  * The NULL_TAG (see [ParseTree.NULL_TAG]) as its tag.
         *  * The type is [RedType.MERGE].
         *
         * @return Default output format for intermediate operations.
         */
        fun merging(): Reduction {
            return Reduction(ParseTree.NULL_TAG.content, RedType.MERGE)
        }

        /**
         * Default output format for new instances of [Rule]. It has the following properties:
         *
         *  * The NULL_TAG (see [ParseTree.NULL_TAG]) as its tag.
         *  * The type is [RedType.INITIAL].
         *
         * @return Default output format for new instances of [Rule].
         */
        fun initial(): Reduction {
            return Reduction(ParseTree.NULL_TAG.content, RedType.INITIAL)
        }

        /**
         * Default output format for final operations whose result is not hidden in the parse tree.
         *
         * @param key The production's name.
         * @return A new output descriptor.
         */
        fun taggedTree(key: Sym): Reduction {
            return Reduction(key, RedType.TAGGED_PARSE_TREE)
        }

        /**
         * Replaces all rules that have the reduction type [RedType.INITIAL] (the default when created) with rules with the reduction type [RedType.TAGGED_PARSE_TREE].
         *
         * @param productions The productions as a mapping of symbols to rules.
         */
        fun applyStandardReductionToProductions(
            productions: LinkedHashMap<Sym, Rule>
        ) {
            for (prod in productions.entries) {
                val key = prod.key
                var value = prod.value
                if (value.reduction.type == RedType.INITIAL) {
                    value = value.withReduction(taggedTree(key))
                }
                prod.setValue(value)
            }
        }
    }
}
