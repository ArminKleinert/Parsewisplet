package de.kleinert.parsewisp.parser_options

import de.kleinert.parsewisp.parsing.AlternationRule

/**
 * Options for deciding what to do when a production is added that already exists.
 *
 *
 * Consider the following grammar:
 * <pre>
 * `S = A     S = B     S = C `
</pre> *
 * The question this class tries to answer is "what to do?". The parser knows thanks to these options.
 *
 *  * [OVERRIDE]: `S = C`
 *  * [ERROR]: Fails.
 *  * [CHOICE]: `S = A | B | C`
 *  * [KEEP]: `S = A`
 *
 *
 * @since 0.9.7
 */
enum class RedefinitionOption {
    /**
     * Ignore existing. Replace and forget.
     *
     *
     * Example: Adding Grammar productions "S = A" and "S = "B" results in "S = B" and discards the first.
     *
     * @since 0.9.7
     */
    OVERRIDE,

    /**
     * Throw exception if a duplicate is added.
     *
     *
     * Example: Adding Grammar productions "S = A" and "S = "B" results in an error.
     *
     * @since 0.9.7
     */
    ERROR,

    /**
     * Create an [AlternationRule].
     *
     *
     * Example: Adding Grammar productions "S = A" and "S = "B" creates a new production "S = A | B".
     *
     * @since 0.9.7
     */
    CHOICE,

    /**
     * Keep old value.
     *
     *
     * Example: Adding Grammar productions "S = A" and "S = "B" keeps "S = A".
     *
     * @since 0.9.7
     */
    KEEP;

    companion object {
        /**
         * Default setting.
         *
         * @since 0.9.7
         */
        val defaultOption: RedefinitionOption = ERROR
    }
}
