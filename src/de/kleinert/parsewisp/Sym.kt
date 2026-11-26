package de.kleinert.parsewisp

/**
 * A class that simply wraps a String to differentiate instances from actual Strings. Unlike Parsewisp, instances are (currently) not interned.
 *
 * @since 0.9.7
 */
class Sym private constructor(private val name: String) {
    override fun hashCode(): Int {
        return this.name.hashCode() - 1640531527
    }

    override fun toString(): String {
        return ":" + this.name
    }

    /**
     * Returns the backing string.
     *
     * @return The backing string.
     * @since 0.9.7
     */
    fun name(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is Sym && other.name == name)
    }

    companion object {
        /**
         * Creates a new symbol or returns an existing one.
         *
         * @param sym The string for the symbol.
         * @return The new or already existing symbol.
         * @since 0.9.7
         */
        fun sym(sym: String): Sym {
            return Sym(sym)
        }
    }
}
