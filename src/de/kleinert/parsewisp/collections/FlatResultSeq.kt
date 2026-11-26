package de.kleinert.parsewisp.collections

import java.util.Arrays
import java.util.stream.Stream
import kotlin.collections.Collection

/**
 * A list-like type of generic elements. It is used to differentiate from other List types.
 * Elements can be added and iterated upon. Each addition creates a new instance.
 *
 *
 * These lists are outwardly purely functional. They are also intended to be intermediate structures and thus do not need to override [Object.equals], [Object.hashCode], or [Object.toString].
 *
 *
 * It is advised that users of this library do not use this type directly.
 *
 * @since 0.9.7
 */
class FlatResultSeq {
    //private static FlatResultSeq EMPTY = null;
    private val head: FlatResultSeq
    private val nodeContent: Any
    private val size: Int

    private var cached: Array<Any?>? = null

    private constructor(head: FlatResultSeq, nodeContent: Any) {
        this.head = head
        this.nodeContent = nodeContent

        val siz = head.size + if (nodeContent is FlatResultSeq) nodeContent.size else 1
        this.size = siz
    }

    private constructor() {
        this.head = this
        this.nodeContent = 0 // Meaningless value
        this.size = 0
    }

    val isEmpty: Boolean
        /**
         * Checks whether this sequence is empty.
         *
         * @return true if size==0
         * @since 0.9.7
         */
        get() = size == 0

    /**
     * Returns the size of this sequence.
     *
     * @return The size.
     * @since 0.9.7
     */
    fun size(): Int {
        return size
    }

    /**
     * This method flattens the sequence into an array.
     *
     *
     * Implementation note: [FlatResultSeq] is built as a reversed singly-linked list.
     *
     * <pre>
     * `var frs12 = FlatResultSeq.make().appendOrConcat(1).appendOrConcat(2); // (content=2, head=(content=1, head=EMPTY))     var frs1212 = frs12.appendOrConcat(frs12); // (content=frs12, head=frs12)     var frs12123 = frs12.appendOrConcat(frs12).appendOrConcat(3); // (content=3, head=(content=frs12, head=frs12)) `
    </pre> *
     *
     *
     * Flattening the sequence iterates backwards.
     *
     *
     * **Example: `frs12123.stream()` is called.**
     *
     *
     * First, an array is created: `new Object[frs12123.size()]`<br></br>
     * The index is set to `frs12123.size()-1`
     *
     *
     * Now, the list is iterated backwards:<br></br>
     * Iteration 1: `array=[null,null,null,null,null], index=4, seq=(content=3, head=(content=(content=2, head=(content=1, head=EMPTY)), head=(content=2, head=(content=1, head=EMPTY))))`
     *
     *
     * Add `content` at the index to the array, decrement index, continue with `head`.<br></br>
     * Iteration 2: `array=[null,null,null,null,3], index=3, seq=(content=(content=2, head=(content=1, head=EMPTY)), head=(content=2, head=(content=1, head=EMPTY)))`
     *
     *
     * `content` is a [FlatResultSeq], so do a recursive call. This adds `2` at index 3 and `1` at index 2. The index is decremented by 2 because two elements were added.<br></br>
     * Iteration 3: `array=[null,null,1,2,3], index=1, seq=(content=2, head=(content=1, head=EMPTY))`
     *
     *
     * Do another recursive call on the last two elements.<br></br>
     * Iteration 4: `array=[1,2,1,2,3], index=-1, seq=EMPTY`
     *
     *
     * The sequence is now empty, the array is filled. So the index (`-1`) is returned and the array is saved.<br></br>
     *
     * @param list           Destination array.
     * @param backwardsIndex Current index.
     * @return The next index.
     * @since 0.9.7
     */
    private fun flatten(list: Array<Any?>, backwardsIndex: Int): Int {
        var backwardsIndex = backwardsIndex
        if (this.isEmpty) return backwardsIndex

        var h = this
        while (h.size > 0) {
            if (h.nodeContent is FlatResultSeq) {
                val frs = h.nodeContent
                backwardsIndex = frs.flatten(list, backwardsIndex) // recursive call
            } else {
                list[backwardsIndex] = h.nodeContent
                backwardsIndex--
            }
            h = h.head
        }

        return backwardsIndex
    }

    /**
     * Returns a stream for this sequence. The result may be cached.
     *
     * @return A stream for this sequence.
     * @since 0.9.7
     */
    fun stream(): Stream<Any?> {
        if (cached == null) {
            cached = arrayOfNulls(size)
            flatten(cached!!, size - 1)
        }
        return Arrays.stream(cached)
    }

    /**
     * Adds an element or multiple elements to this sequence.
     *
     *  * If the input is `null`, do nothing.
     *  * If the input is a [FlatResultSeq], add all elements flattened, as if by [Collection.addAll]
     *  * Otherwise, add the input as a single element, as if by [Collection.add].
     *
     *
     * @param obj The input.
     * @return A new instance or `this`, if nothing is added.
     * @since 0.9.7
     */
    fun appendOrConcat(obj: Any?): FlatResultSeq {
        if (obj == null) return this
        return FlatResultSeq(this, obj)
    }

    companion object {
        /**
         * Instances of this type always start empty. This method simply returns an empty sequence.
         *
         * @return The empty instance.
         * @since 0.9.7
         */
        fun make(): FlatResultSeq {
//        if (EMPTY == null) EMPTY = new FlatResultSeq();
//        return EMPTY;
            return FlatResultSeq()
        }
    }
}
