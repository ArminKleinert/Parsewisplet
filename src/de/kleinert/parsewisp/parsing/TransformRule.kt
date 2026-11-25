package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.collections.FlatResultSeq
import de.kleinert.parsewisp.collections.FlatResultSeq.Companion.make
import de.kleinert.parsewisp.functions.Listener
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.result.success.ParseMessage
import java.util.function.BiFunction
import java.util.stream.Stream

/**
 * This class provides limited utility for interacting with the host environment and can modify the parse tree
 * <pre>
 * `var g = new Grammar(Sym.sym("S"), new LinkedHashMap<>(Map.of(             Sym.sym("S"), ConcatRule.create(List.of(                     TransformRule.create(                             StringTerm.create("abc", false),                             (r, i) -> List.of("a", "b", "c")                     ))))));     var p = Parsewisp.parser(g);     System.out.println(p.parse("abc")); // [:S, "a", "b", "c"] `
</pre> *
 *
 * @since 0.9.7
 */
class TransformRule private constructor(
    hide: Boolean,
    red: ReductionType,
    rule: Rule,
    private val transformer: BiFunction<List<Any?>, Int, List<Any?>>
) : RuleWithChild(hide, red, rule) {
    override fun parse(index: Int, runner: Gll) {
        runner.pushListener(Rule.Companion.makeKey(index, rule), commonListener(runner, index))
    }

    override fun fullParse(index: Int, runner: Gll) {
        runner.pushFullListener(Rule.Companion.makeKey(index, rule), commonListener(runner, index))
    }

    private fun commonListener(runner: Gll, index: Int): Listener {
        return Listener { result: ParseMessage? ->
            var wrapped = result!!.result
            wrapped = if (wrapped is FlatResultSeq)
                wrapped.stream()
            else
                Stream.ofNullable(wrapped)
            runner.pushSuccessMessage(
                makeKey(index, this),
                frs(
                    transformer.apply(
                        (wrapped as Stream<*>).toList() as List<Any?>,
                        result.index()
                    )!!
                ),
                result.index()
            )
        }
    }

    override fun withRule(rule: Rule): TransformRule {
        return TransformRule(hide, red, rule, transformer)
    }

    override fun withHideTag(hide: Boolean): TransformRule {
        return if (isHiddenInResult1() == hide) this else TransformRule(hide, red, rule, transformer)
    }

    override fun withReduction(red: ReductionType): TransformRule {
        return if (getReduction1() === red) this else TransformRule(hide, red, rule, transformer)
    }

    override fun equals(o: Any?): Boolean {
        if (o == null || javaClass != o.javaClass) return false
        if (!super.equals(o)) return false

        val that = o as TransformRule
        return transformer == that.transformer
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
         * @since 0.9.7
         */
        fun create(rule: Rule, transformer: BiFunction<List<Any?>, Int, List<Any?>>): Rule {
            return TransformRule(Rule.Companion.defaultHidden, Rule.Companion.defaultReductionType, rule, transformer)
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
