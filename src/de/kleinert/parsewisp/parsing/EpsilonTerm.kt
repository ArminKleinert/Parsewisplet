package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType

/**
 * This class represents the empty parse epsilon. It can typically be written in the following ways: "Epsilon", "epsilon", "EPSILON", "eps", "ε" or as the empty string "".
 * <br></br>
 * When parsing, the process depends on whether a full parse is being done (matching to the end of the input):
 * If yes, success if the end of input has been reached, fail otherwise. If no, always success.
 */
class EpsilonTerm  //private static final @NotNull EpsilonTerm epsilon = new EpsilonTerm(defaultHidden, defaultReductionType);
private constructor(hide: Boolean, red: ReductionType) : Terminal(hide, red) {
    override fun parse(index: Int, runner: Gll) {
        runner.pushSuccessMessageWithoutValue(makeKey(index, this), index)
    }

    override fun fullParse(index: Int, runner: Gll) {
        if (index == runner.tramp().text.length)
            runner.pushSuccessMessageWithoutValue(makeKey(index, this), index)
        else runner.fail(makeKey(index, this), index, this, true)
    }

    override fun withHideTag(hide: Boolean): EpsilonTerm {
        return if (isHiddenInResult == hide) this else EpsilonTerm(hide, reduction)
    }

    override fun withReduction(red: ReductionType): EpsilonTerm {
        return if (reduction === red) this else EpsilonTerm(isHiddenInResult, red)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EpsilonTerm) return false
        if (this === other) return true
        return isHiddenInResult == other.isHiddenInResult && reduction == other.reduction
    }

    override fun hashCode(): Int {
        return hash(isHiddenInResult, reduction)
    }

    companion object {
        val default: EpsilonTerm
            /**
             * A default instance of Epsilon. May be buffered, but does not have to.
             *
             * @return A default instance of Epsilon.
             */
            get() = EpsilonTerm(defaultHidden, defaultReductionType)
    }
}
