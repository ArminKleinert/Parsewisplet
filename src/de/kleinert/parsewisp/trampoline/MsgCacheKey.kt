package de.kleinert.parsewisp.trampoline

import de.kleinert.parsewisp.functions.Listener

/**
 * A key for the trampoline's message cache.
 *
 * @param index    The index.
 * @param listener The listener.
 * @since 0.9.7
 */
data class MsgCacheKey(val index: Int, val listener: Listener)
