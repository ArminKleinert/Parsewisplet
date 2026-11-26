package de.kleinert.parsewisp.collections

/**
 * Takes a function and lazily executes it repeatedly when requested.
 * The results are buffered in a [List] for fast access.
 * A maximum number of results can be specified.
 * The function takes an `int` as input.
 *
 * @param <T> The result type for the function.
</T> */
open class LazySupplierList<T>(nextFn: (Int) -> T?, maxResults: Int) : List<T> {
    private val evaluatedPart: MutableList<T> = mutableListOf()
    private val maxResults: Int
    private var nextFn: ((Int) -> T?)?

    /**
     * True if the list is fully evaluated, false otherwise.
     *
     * @return true or false.
     */
    var isFullyEvaluated: Boolean = false
        private set

    init {
        this.nextFn = nextFn
        this.maxResults = maxResults
    }

    private fun evalutePart(i: Int): T? {
        // Already calculated.
        if (i < evaluatedPart.size) return evaluatedPart.get(i)

        // End reached.
        if (evaluatedPart.size >= maxResults) {
            this.isFullyEvaluated = true
            nextFn = null // Discard the function.
        }

        // The list is fully evaluated. There is no element at index i.
        if (this.isFullyEvaluated) return null

        // Fully evaluated but the function is gone? Impossible!
        checkNotNull(nextFn)

        // Calculate the next element.
        val next = nextFn!!(evaluatedPart.size)

        // End reached?
        if (next == null) {
            this.isFullyEvaluated = true
            nextFn = null // Discard the function.
            return null
        }

        // End not reached yet: Buffer the element and return it.
        evaluatedPart.add(next)
        return next
    }

    /**
     * Fully evaluates the list.
     *
     */
    fun evaluate() {
        var ep: T?
        do {
            ep = evalutePart(evaluatedPart.size)
        } while (ep != null)
    }

    override fun toString(): String {
        evaluate()
        return evaluatedPart.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is List<*>) {
            return false
        }

        if (other === this) return true

        val res = evaluateToList() == other
        return res
    }

    fun evaluateToList(): List<T> {
        evaluate()
        return evaluatedPart
    }

    override fun hashCode(): Int {
        var hc = 1
        for (e in this) hc = hc * 31 + e.hashCode()
        return hc
    }

    override val size: Int
        get() {
            evaluate()
            return evaluatedPart.size
        }

    override fun isEmpty(): Boolean {
        return evaluatedPart.isEmpty() && evalutePart(0) == null
    }

    override fun contains(element: T): Boolean {
        for (t in this) {
            if (t == element) return true
        }
        return false
    }

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            private var cursor = 0

            override fun hasNext(): Boolean {
                return getOrNull(cursor) != null
            }

            override fun next(): T {
                val i = cursor
                ++cursor
                return get(i)
            }
        }
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        for (o in elements) {
            if (!contains(o)) return false
        }
        return true
    }

    override fun get(index: Int): T {
        val at = getOrNull(index)
        if (at == null) throw IndexOutOfBoundsException("Cannot access index " + index + " because size is " + size)
        return at
    }

    fun first(): T {
        if (evaluatedPart.isNotEmpty()) return evaluatedPart.get(0)
        evalutePart(0)
        return evaluatedPart.get(0)
    }

    /**
     * Returns the element at index i. If the list has not been calculated up to that point yet, evaluate until the index is reached.
     * This function runs in amortized O(1) time.
     * If the list is fully evaluated and the index is too big, return null;
     *
     * @param i The index.
     * @return The element at the provided index or null.
     */
    fun getOrNull(i: Int): T? {
        // If already calculated, return.
        if (i < evaluatedPart.size) return evaluatedPart.get(i)

        // Not calculated yet: Do work and return.
        var cursor = evaluatedPart.size
        while (!this.isFullyEvaluated) {
            val next = evalutePart(i)
            if (cursor == i) return next // Yay. Found it.

            cursor++
        }
        return null // List too short. null :(
    }

    override fun indexOf(element: T): Int {
        var i = 0
        for (t in this) {
            if (element == t) return i
            i++
        }
        return -1
    }

    override fun lastIndexOf(element: T): Int {
        var i = 0
        var last = -1
        for (t in this) {
            if (element == t) last = i
            i++
        }
        return last
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        val res = mutableListOf<T>()
        val cursor = 0
        for (t in this) {
            if (cursor >= fromIndex && cursor < toIndex) res.add(t)
            else if (cursor >= toIndex) break
        }
        return res.toList()
    }

    override fun listIterator(): ListIterator<T> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): ListIterator<T> {
        return this.toList().listIterator(index)
    }
}
