package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.collections.FlatResultSeq
import de.kleinert.parsewisp.collections.FlatResultSeq.Companion.make
import de.kleinert.parsewisp.functions.Listener
import de.kleinert.parsewisp.reduction.Reduction
import de.kleinert.parsewisp.result.success.ParseMessage

/**
 * This class provides limited utility for interacting with the host environment and can modify the parse tree.
 */
class TransformRule private constructor(
    hide: Boolean,
    red: Reduction,
    rule: Rule,
    private val transformer: (List<Any?>, Int)-> List<Any?>
) : RuleWithChild(hide, red, rule) {
    override fun parse(index: Int, runner: Gll) {
        runner.pushListener(makeKey(index, rule), commonListener(runner, index))
    }

    override fun fullParse(index: Int, runner: Gll) {
        runner.pushFullListener(makeKey(index, rule), commonListener(runner, index))
    }

    private fun commonListener(runner: Gll, index: Int): Listener {
        return { result: ParseMessage? ->
            var wrapped = result!!.result
            wrapped = if (wrapped is FlatResultSeq)
                wrapped.toList()
            else
                listOf(wrapped)
            runner.pushSuccessMessage(
                makeKey(index, this),
                frs(
                    transformer(
                        wrapped,
                        result.index()
                    )
                ),
                result.index()
            )
        }
    }

    override fun withRule(rule: Rule): TransformRule {
        return TransformRule(isHiddenInResult, reduction, rule, transformer)
    }

    override fun withHideTag(hide: Boolean): TransformRule {
        return if (isHiddenInResult == hide) this else TransformRule(hide, reduction, rule, transformer)
    }

    override fun withReduction(red: Reduction): TransformRule {
        return if (reduction === red) this else TransformRule(isHiddenInResult, red, rule, transformer)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false

        val that = other as TransformRule
        return transformer === that.transformer
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + transformer.hashCode()
        return result
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param rule        The [Rule] to match repeatedly.
         * @param transformer The transformer function.
         * @return A rule.
         */
        fun create(rule: Rule, transformer: (List<Any?>, Int) -> List<Any?>): Rule {
            return TransformRule(defaultHidden, defaultReduction, rule, transformer)
        }

        private fun frs(init: List<Any?>): FlatResultSeq {
            var frs = make()
            for (o in init) {
                frs = frs.appendOrConcat(o)
            }
            return frs
        }
    }
}
