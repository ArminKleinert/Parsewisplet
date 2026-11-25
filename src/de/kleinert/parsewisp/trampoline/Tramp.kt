package de.kleinert.parsewisp.trampoline

import de.kleinert.parsewisp.functions.NegativeListener
import de.kleinert.parsewisp.functions.Procedure
import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.parsing.StringTerm
import de.kleinert.parsewisp.result.ParseFailure
import de.kleinert.parsewisp.result.ParseTree
import de.kleinert.parsewisp.result.success.ParseMessage
import java.util.*

/**
 * This class carries the inner state during the parsing process. It should not be used directly.
 *
 * @since 0.9.7
 */
class Tramp @JvmOverloads constructor(
    /**
     * Get the grammar.
     *
     * @return The grammar.
     * @since 0.9.7
     */
    val grammar: Grammar,
    /**
     * Get the text.
     *
     * @return The text.
     * @since 0.9.7
     */
    val text: String,
    /**
     * Get the index at which parsing started. This is only important if parsing had to be restarted after a failure.
     *
     * @return Get the index at which parsing started.
     * @since 0.9.7
     */
    val failIndex: Int = -1
) {
    /**
     * A stack of derivative parse procedures of the current generation.
     *
     * @return A stack of derivative parse procedures of the current generation.
     * @since 0.9.7
     */
    val stack: ArrayList<Procedure> = ArrayList<Procedure>()
    private val nextStack: ArrayList<Procedure>

    /**
     * The current generation of derivatives.
     *
     * @return The current generation of derivatives.
     * @since 0.9.7
     */
    var generation: Int = 0
        private set

    /**
     * Sequential map of negative lookaheads.
     *
     * @return Sequential map of negative lookaheads.
     * @since 0.9.7
     */
    val negativeListeners: TreeMap<Int, ArrayList<NegativeListener>>

    //private final @NotNull IntMap<@NotNull NegativeListener> negativeListeners;
    private val msgCache: LinkedHashMap<MsgCacheKey?, Int?>
    private val nodes: LinkedHashMap<ListenerKey?, ListenerNode?>
    /**
     * Sets the last detected parsing success. This is usually a String for a parsed [StringTerm] or a finished [ParseTree].
     *
     * @param success The new success or null.
     * @since 0.9.7
     */
    /**
     * Last registered success.
     *
     * @return Last registered success.
     * @since 0.9.7
     */
    var success: ParseMessage?
    private var failure: ParseFailure?

    /**
     * Used to continue parsing after a failure.
     *
     * @param grammar   The grammar.
     * @param text      The text.
     * @param failIndex The index at which to continue.
     * @since 0.9.7
     */
    /**
     * Normal constructor.
     *
     * @param grammar The grammar.
     * @param text    The text.
     * @since 0.9.7
     */
    init {
        this.nextStack = ArrayList<Procedure>()
        //this.negativeListeners = new IntMap<>(1024);
        this.negativeListeners = TreeMap<Int, ArrayList<NegativeListener>>()
        this.msgCache = LinkedHashMap<MsgCacheKey?, Int?>()
        this.nodes = LinkedHashMap<ListenerKey?, ListenerNode?>()
        this.success = null
        this.failure = null
    }

    fun getText1() = text

    fun getGeneration1() = generation

    fun getStack1() = stack
    fun getNegativeListeners1() = negativeListeners
    fun getFailIndex1() = failIndex

    /**
     * Adds a new listener at the key's index.
     *
     * @param creator          The key.
     * @param negativeListener The listener procedure.
     * @since 0.9.7
     */
    fun pushNegativeListener(
        creator: ListenerKey,
        negativeListener: NegativeListener
    ) {
        val present = negativeListeners.get(creator.index)
        if (present == null) {
            val listeners = ArrayList<NegativeListener>()
            listeners.add(negativeListener)
            negativeListeners.put(creator.index, listeners)
        } else {
            present.add(negativeListener)
        }
    }

    /**
     * If parsing failed, a failure is set.
     *
     * @return If parsing failed, a failure is set.
     * @since 0.9.7
     */
    fun getFailure(): ParseFailure? {
        return failure
    }

    /**
     * Increments the generation.
     *
     * @since 0.9.7
     */
    fun nextGeneration() {
        generation = generation + 1
        stack.clear()
        stack.addAll(nextStack)
        nextStack.clear()
    }

    /**
     * Adds a procedure to the stack.
     *
     * @param frame The frame.
     * @since 0.9.7
     */
    fun addToStack(frame: Procedure) {
        this.stack.add(frame)
    }

    /**
     * Removes the top of the stack.
     *
     * @since 0.9.7
     */
    fun popStack() {
        stack.removeAt(stack.size - 1)
    }

    /**
     * Adds an element to the stack for the next generation.
     *
     * @param frame The next frame.
     * @since 0.9.7
     */
    fun addToNextStack(frame: Procedure) {
        nextStack.add(frame)
    }

    /**
     * Returns the "generation" a listener will belong to.
     *
     * @param key        The key for the message.
     * @param defaultVal The generation to use if the key was not registered.
     * @return The generation.
     * @since 0.9.7
     */
    fun getFromMsgCache(key: MsgCacheKey, defaultVal: Int): Int {
        return msgCache.getOrDefault(key, defaultVal)!!
    }

    /**
     * Adds a listener to the message cache.
     *
     * @param key        The key for the message.
     * @param defaultVal The generation the listener is for.
     * @since 0.9.7
     */
    fun addToMsgCache(key: MsgCacheKey, defaultVal: Int) {
        msgCache.put(key, defaultVal)
    }

    /**
     * This function is used when the parser detects a failure. Parsing will be terminated when a failure is set.
     *
     * @param failure The failure.
     * @since 0.9.7
     */
    fun setFailure(failure: ParseFailure) {
        this.failure = failure
    }

    /**
     * Gets a listener node (if registered) from the trampoline.
     *
     * @param nodeKey The key (position and production).
     * @return The node or null.
     * @since 0.9.7
     */
    fun getNode(nodeKey: ListenerKey): ListenerNode? {
        return nodes.get(nodeKey)
    }

    /**
     * Adds a new listener-node.
     *
     * @param key  The key (position and production)
     * @param node The node.
     * @since 0.9.7
     */
    fun addToNodes(
        key: ListenerKey,
        node: ListenerNode
    ) {
        nodes.put(key, node)
    }

    override fun toString(): String {
        return "Tramp{" +
                "grammar=" + grammar +
                ", text='" + text + '\'' +
                ", failIndex=" + failIndex +
                ", stack=" + stack +
                ", nextStack=" + nextStack +
                ", generation=" + generation +
                ", negativeListeners=" + negativeListeners +
                ", msgCache=" + msgCache +
                ", nodes=" + nodes +
                ", success=" + success +
                ", failure=" + failure +
                '}'
    }
}
