package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType
import java.util.*

/**
 * This class represents the empty parse epsilon. It can typically be written in the following ways: "Epsilon", "epsilon", "EPSILON", "eps", "ε" or as the empty string "".
 * <br></br>
 * When parsing, the process depends on whether a full parse is being done (matching to the end of the input):
 * If yes, success if the end of input has been reached, fail otherwise. If no, always success.
 *
 * @since 0.9.7
 */
class EpsilonTerm  //private static final @NotNull EpsilonTerm epsilon = new EpsilonTerm(defaultHidden, defaultReductionType);
private constructor(hide: Boolean, red: ReductionType) : Terminal(hide, red) {
    override fun parse(index: Int, runner: Gll) {
        runner.pushSuccessMessageWithoutValue(Rule.Companion.makeKey(index, this), index)
    }

    override fun fullParse(index: Int, runner: Gll) {
        if (index == runner.tramp()
                .getText1().length
        ) runner.pushSuccessMessageWithoutValue(Rule.Companion.makeKey(index, this), index)
        else runner.fail(Rule.Companion.makeKey(index, this), index, this, true)
    }

    override fun withHideTag(hide: Boolean): EpsilonTerm {
        return if (isHiddenInResult == hide) this else EpsilonTerm(hide, red)
    }

    override fun withReduction(red: ReductionType): EpsilonTerm {
        return if (reduction === red) this else EpsilonTerm(hide, red)
    }

    override fun equals(o: Any?): Boolean {
        if (o !is EpsilonTerm) return false
        if (this === o) return true
        return hide == o.hide && red == o.red
    }

    override fun hashCode(): Int {
        return Objects.hash(hide, red)
    }

    companion object {
        val default: EpsilonTerm
            /**
             * A default instance of Epsilon. May be buffered, but does not have to.
             *
             * @return A default instance of Epsilon.
             * @since 0.9.7
             */
            get() = EpsilonTerm(defaultHidden, defaultReductionType)
    }
}
