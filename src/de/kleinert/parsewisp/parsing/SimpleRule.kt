package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.Reduction

/**
 * A general type of [Rule] for classes which do not have child-rules. This includes Terminals and NonTerminals, but not repetitions.
 */
abstract class SimpleRule
protected constructor(hide: Boolean, red: Reduction) : Rule(hide, red)

