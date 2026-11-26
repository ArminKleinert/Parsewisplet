package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType

/**
 * A general type of [Rule] for terminals (epsilon, regex, string, char).
 */
abstract class Terminal
protected constructor(hide: Boolean, red: ReductionType) : SimpleRule(hide, red)

