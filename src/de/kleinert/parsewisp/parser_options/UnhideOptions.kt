package de.kleinert.parsewisp.parser_options

/**
 * See [Unhide] for documentation and an example.
 */
enum class UnhideOptions {
    /**
     * Do nothing.
     *
     * @see ParsingOptions.unhide
     */
    NONE,

    /**
     * Unhide tags, do not show hidden contents.
     *
     * @see Unhide.unhideTags
     * @see ParsingOptions.unhide
     */
    TAGS,

    /**
     * Unhide contents, but keep tags hidden.
     *
     * @see Unhide.unhideContent
     * @see ParsingOptions.unhide
     */
    CONTENT,

    /**
     * Show both contents and tags.
     *
     * @see Unhide.unhideAll
     * @see ParsingOptions.unhide
     */
    ALL
}
