package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.reduction.ReductionType

/**
 * A general type of [Rule] for classes which do not have child-rules. This includes Terminals and NonTerminals, but not repetitions.
 *
 * @since 0.9.7
 */
abstract class SimpleRule
protected constructor(hide: Boolean, red: ReductionType) : Rule(hide, red)

