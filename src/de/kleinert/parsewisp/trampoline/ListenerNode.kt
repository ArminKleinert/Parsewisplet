package de.kleinert.parsewisp.trampoline

import de.kleinert.parsewisp.functions.Listener
import de.kleinert.parsewisp.result.success.ParseMessage

/**
 * These objects save listeners and results for a specific index and rule to ensure that no productions are ever repeated for a specific index.
 *
 * @param listeners     Listener objects for parses.
 * @param fullListeners Listeners that expect to be parsed to the end of the input text.
 * @param results       Successes (or failures) for listeners.
 * @param fullResults   Successes (or failures) for full listeners.
 */
data class ListenerNode(
    val listeners: MutableList<Listener> =mutableListOf(),
    val fullListeners: MutableList<Listener> =mutableListOf(),
    val results: LinkedHashSet<ParseMessage> = linkedSetOf(),
    val fullResults: LinkedHashSet<ParseMessage> = linkedSetOf()
)
