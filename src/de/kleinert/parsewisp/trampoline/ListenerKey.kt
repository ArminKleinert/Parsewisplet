package de.kleinert.parsewisp.trampoline

import de.kleinert.parsewisp.parsing.Rule

/**
 * These objects are used to index [ListenerNode] objects in a [Tramp]. They ensure that no productions are ever repeated at a specified index.
 *
 * @param index The index.
 * @param rule  The production.
 */
data class ListenerKey(val index: Int, val rule: Rule)
