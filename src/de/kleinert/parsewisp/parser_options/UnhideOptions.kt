package de.kleinert.parsewisp.parser_options

/**
 * See [Unhide] for documentation and an example.
 *
 * @since 0.9.7
 */
enum class UnhideOptions {
    /**
     * Do nothing.
     *
     * @see ParsingOptions.unhide
     * @since 0.9.7
     */
    NONE,

    /**
     * Unhide tags, do not show hidden contents.
     *
     * @see Unhide.unhideTags
     * @see ParsingOptions.unhide
     * @since 0.9.7
     */
    TAGS,

    /**
     * Unhide contents, but keep tags hidden.
     *
     * @see Unhide.unhideContent
     * @see ParsingOptions.unhide
     * @since 0.9.7
     */
    CONTENT,

    /**
     * Show both contents and tags.
     *
     * @see Unhide.unhideAll
     * @see ParsingOptions.unhide
     * @since 0.9.7
     */
    ALL
}
