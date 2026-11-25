package de.kleinert.parsewisp

import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * A class for string-alternatives that are slightly slower to create than strings, but provide guaranteed
 * interning and O(1) comparisons via the build-in `==` operator.
 *
 *
 * `Sym k1 = Sym.sym(str);     Sym k2 = Sym.sym(str);     k1 == k2 // Guarantied to be true. `
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
        return this === other
    }

    companion object {
        private val table: MutableMap<String, Reference<Sym?>> = ConcurrentHashMap<String, Reference<Sym?>>()
        private val rq = ReferenceQueue<Sym?>()

        /**
         * Creates a new symbol or returns an existing one.
         *
         * @param sym The string for the symbol.
         * @return The new or already existing symbol.
         * @since 0.9.7
         */
        @JvmStatic
        fun sym(sym: String): Sym {
            var k: Sym? = null
            var existingRef: Reference<Sym?>? = table[sym]
            if (existingRef == null) {
                if (rq.poll() != null) {
                    var o: Any? = rq.poll()
                    while (o != null) {
                        o = rq.poll()
                    }

                    for (e in table.entries) {
                        val `val` = e.value
                        if (`val`.get() == null) {
                            table.remove(e.key, `val`)
                        }
                    }
                }

                k = Sym(sym)
                existingRef = table.putIfAbsent(sym, WeakReference(k, rq))
            }

            if (existingRef == null) {
                return k!!
            } else {
                val existing = existingRef.get()
                if (existing != null) {
                    return existing
                }
                table.remove(sym, existingRef)
                return sym(sym)
            }
        }
    }
}
