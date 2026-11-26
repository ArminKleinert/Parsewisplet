package de.kleinert.parsewisp.collections

/**
 * An interface which pretends to be a list. Every method of [List] is defined to throw an [UnsupportedOperationException].
 *
 * @param <T> The generic type.
 * @since 0.9.7
</T> */
interface PretenderList<T> : List<T> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun contains(element: T): Boolean {
        throw UnsupportedOperationException()
    }

    override fun iterator(): Iterator<T> {
        throw UnsupportedOperationException()
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun get(index: Int): T {
        throw UnsupportedOperationException()
    }

    override fun indexOf(element: T): Int {
        throw UnsupportedOperationException()
    }

    override fun lastIndexOf(element: T): Int {
        throw UnsupportedOperationException()
    }

    override fun listIterator(): ListIterator<T> {
        throw UnsupportedOperationException()
    }

    override fun listIterator(index: Int): ListIterator<T> {
        throw UnsupportedOperationException()
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        throw UnsupportedOperationException()
    }
}
