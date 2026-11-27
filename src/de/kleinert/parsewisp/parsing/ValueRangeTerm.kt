package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.Reduction
import de.kleinert.parsewisp.trampoline.ListenerKey

/**
 * Represents an ABNF value range.
 */
class ValueRangeTerm private constructor(
    hide: Boolean,
    red: Reduction,
    lo: Int, hi: Int
) : Terminal(hide, red) {
    /**
     * The lowest codepoint.
     *
     * @return The lowest codepoint as an int.
     */
    val lo: Int

    /**
     * The highest codepoint.
     *
     * @return The highest codepoint as an int.
     */
    val hi: Int

    init {
        require(!(lo > hi || lo < 0 || hi > 0x10FFFF)) { "low codepoint must be less than or equal to high codepoint. Both must be in range 0x00_0000 to 0x10_FFFF. (lo=" + lo + ", hi=" + hi + ")" }
        this.lo = lo
        this.hi = hi
    }

    override fun parse(index: Int, runner: Gll) {
        parse(index, runner, false)
    }

    override fun fullParse(index: Int, runner: Gll) {
        parse(index, runner, true)
    }

    private fun parse(index: Int, runner: Gll, expectEnd: Boolean) {
        val text = runner.tramp().text
        val nodeKey: ListenerKey = makeKey(index, this)

        if (index >= text.length) {
            runner.fail(nodeKey, index, this, expectEnd)
            return
        }

        val codePoint = Character.codePointAt(text, index)
        val charString = String(Character.toChars(codePoint))

        val check = !expectEnd || (index + charString.length == text.length)
        if (check && lo <= codePoint && codePoint <= hi) {
            runner.pushSuccessMessage(nodeKey, charString, index + charString.length)
        } else {
            runner.fail(nodeKey, index, this, expectEnd)
        }
    }

    override fun withHideTag(hide: Boolean): ValueRangeTerm {
        return if (isHiddenInResult == hide) this else ValueRangeTerm(hide, reduction, lo, hi)
    }

    override fun withReduction(red: Reduction): ValueRangeTerm {
        return if (reduction === red) this else ValueRangeTerm(isHiddenInResult, red, lo, hi)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ValueRangeTerm) return false
        if (this === other) return true
        return isHiddenInResult == other.isHiddenInResult && reduction == other.reduction
                && lo == other.lo && hi == other.hi
    }

    override fun hashCode(): Int {
        return hash(isHiddenInResult, reduction, lo, hi)
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param lo The lowest codepoint.
         * @param hi The highest codepoint.
         * @return A rule.
         * @throws IllegalArgumentException if the minimum codepoint value is greater than the maximum.
         */
        fun create(lo: Int, hi: Int): Rule {
            return ValueRangeTerm(defaultHidden, defaultReduction, lo, hi)
        }
    }
}
