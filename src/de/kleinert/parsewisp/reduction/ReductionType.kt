package de.kleinert.parsewisp.reduction

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parsing.Rule
import de.kleinert.parsewisp.result.ParseTree
import java.util.*

/**
 * A class for output formats. This entire class might be removed in the future and replaced by an alternative [ParseTree] variant specifically for intermediate operations.
 *
 *
 * Unlike the output formats of Instaparse (hiccup and enlive), this class has no relevance for the way parse trees can be handled.
 *
 * @since 0.9.7
 */
class ReductionType private constructor(
    /**
     * The production key this output format is for.
     *
     * @return The production key this output format is for.
     * @since 0.9.7
     */
    val key: Sym,
    /**
     * The [Type] type of this reduction.
     *
     * @return The [Type] type of this reduction.
     * @since 0.9.7
     */
    val reductionType: Type,
    /**
     * If true, the intermediate result will be hidden in the final output tree.
     *
     * @return If true, the intermediate result will be hidden in the final output tree.
     * @since 0.9.7
     */
    val isHiddenOrRaw: Boolean
) {
    fun getReductionType1() = reductionType
    fun getKey1() = key
    fun isHiddenOrRaw1() = isHiddenOrRaw

    /**
     * The available types. They mark whether the parse operation is an intermediate operation or for the final output.
     *
     * @since 0.9.7
     */
    enum class Type {
        /**
         * The default type. This should not be used directly.
         *
         * @since 0.9.7
         */
        INITIAL,

        /**
         * Has the same properties as [INITIAL], but will not be replaced with [TAGGED_PARSE_TREE] when finalizing the grammar.
         *
         * @since 0.9.7
         */
        MERGE,

        /**
         * Default type. Represent parse trees as lists.
         *
         * @since 0.9.7
         */
        TAGGED_PARSE_TREE
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ReductionType
        return key == that.key
    }

    override fun hashCode(): Int {
        return Objects.hash(key, this.reductionType, this.isHiddenOrRaw)
    }

    override fun toString(): String {
        return "{" +
                "key=" + key +
                ", type=" + this.reductionType +
                '}'
    }

    companion object {
        private var mergeReduction: ReductionType? = null
        private var initialReduction: ReductionType? = null

        /**
         * The default output format for results that are to be merged into a parent tree. It has the following properties:
         *
         *  * The NULL_TAG (see [ParseTree.NULL_TAG]) as its tag.
         *  * The type is [Type.MERGE].
         *  * [isHiddenOrRaw1] returns true.
         *
         * @return Default output format for intermediate operations.
         * @since 0.9.7
         */
        fun merging(): ReductionType {
            if (mergeReduction == null) mergeReduction =
                ReductionType(ParseTree.Companion.NULL_TAG.content, Type.MERGE, true)
            return mergeReduction!!
        }

        /**
         * Default output format for new instances of [Rule]. It has the following properties:
         *
         *  * The NULL_TAG (see [ParseTree.NULL_TAG]) as its tag.
         *  * The type is [Type.INITIAL].
         *  * [isHiddenOrRaw1] returns true.
         *
         * @return Default output format for new instances of [Rule].
         * @since 0.9.7
         */
        fun initial(): ReductionType {
            if (initialReduction == null) initialReduction =
                ReductionType(ParseTree.Companion.NULL_TAG.content, Type.INITIAL, true)
            return initialReduction!!
        }

        /**
         * Default output format for final operations whose result is not hidden in the parse tree.
         *
         * @param key The production's name.
         * @return A new output descriptor.
         * @since 0.9.7
         */
        fun taggedTree(key: Sym): ReductionType {
            return ReductionType(key, Type.TAGGED_PARSE_TREE, false)
        }

        /**
         * Replaces all rules that have the reduction type [Type.INITIAL] (the default when created) with rules with the reduction type [Type.TAGGED_PARSE_TREE].
         *
         * @param productions The productions as a mapping of symbols to rules.
         * @since 0.9.7
         */
        fun applyStandardReductionToProductions(
            productions: LinkedHashMap<Sym, Rule>
        ) {
            for (prod in productions.entries) {
                val key = prod.key
                var value = prod.value
                if (value.getReduction1().reductionType == Type.INITIAL) {
                    value = value.withReduction(taggedTree(key))
                }
                prod.setValue(value)
            }
        }
    }
}
