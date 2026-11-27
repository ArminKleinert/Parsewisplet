package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.Reduction

/**
 * A general type of [Rule] for terminals (epsilon, regex, string, char).
 */
abstract class Terminal
protected constructor(hide: Boolean, red: Reduction) : SimpleRule(hide, red)

