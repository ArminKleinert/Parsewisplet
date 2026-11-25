package de.kleinert.parsewisp.util

import java.util.regex.Pattern

/**
 * Helper functions for creating parsers from strings.
 *
 * @since 0.9.7
 */
class StrParser {
    val sb: StringBuilder

    /**
     * The constructor initializes a StringBuilder or similar object for internal use.
     *
     * @since 0.9.7
     */
    init {
        sb = StringBuilder()
    }

    private fun unescape(offset: Int, backOffset: Int, starter: Char, s: String): String {
        sb.setLength(0)
        var i = offset
        while (i < s.length - backOffset) {
            val c = s.get(i)

            if (c == starter) {
                break
            }

            if (c == '\\') {
                require(i + 1 < s.length) { "Encountered backslash character at end of string: " + s }

                i++
                val c2 = s.get(i)

                if (c2 == starter) {
                    sb.append(c2)
                } else {
                    when (c2) {
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000c')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            i += 4
                            sb.appendCodePoint(Integer.parseInt(s, i - 3, i + 1, 16))
                        }

                        '\\' -> sb.append('\\')
                        else -> throw IllegalArgumentException(s + " at " + i + " ; sb=" + sb)
                    }
                }
            } else {
                sb.append(c)
            }
            i++
        }

        return sb.toString()
    }

    private fun unescapeRegex(offset: Int, backOffset: Int, starter: Char, s: String): String {
        sb.setLength(0)
        var i = offset
        while (i < s.length - backOffset) {
            val c = s.get(i)

            if (c == starter) {
                break
            }

            if (c == '\\') {
                require(i + 1 < s.length) { "Encountered backslash character at end of string: " + s }

                i++
                val c2 = s.get(i)
                if (c2 == starter) {
                    sb.append(c2)
                } else {
                    sb.append('\\').append(c2)
                }
            } else {
                sb.append(c)
            }
            i++
        }

        return sb.toString()
    }

    /**
     * Unescapes a string. Handles most escape sequences, including Unicode chars (starting with "\\u"), but not octal escape sequences. Both offsets are assumed to be 0 or greater.
     *
     * @param s          The (non-empty) input string. Format: '...' or "...", but could be anything.
     * @param offset     The offset from index 0. For example, a string of the form "..." would use offset 1, but a string of the form ''...'' would use offset 2.
     * @param backOffset The offset from the end. This separates the final character of the string. For example, a string of the form "..." would use backOffset 1, but a string of the form ''...'' would use backOffset 2.
     * @return A string.
     * @since 0.9.7
     */
    /**
     * Unescapes a (non-empty) string. Handles most escape sequences, including Unicode chars (starting with "\\u"), but not octal escape sequences.
     *
     * @param s The (non-empty) input string. Format: '...' or "..."
     * @return A string.
     * @since 0.9.7
     */
    @JvmOverloads
    fun processString(s: String, offset: Int = 1, backOffset: Int = 1): String {
        return unescape(offset, backOffset, s.get(0), s)
    }

    /**
     * Unescapes a (non-empty) string and turns it into a [Pattern]. Handles most escape sequences, including Unicode chars (starting with "\\u"), but not octal escape sequences. Both offsets are assumed to be 0 or greater.
     *
     * @param s          The (non-empty) input string. Format: #'...' or #"...", but can be anything.
     * @param offset     The offset from index 0. For example, a regex of the form #"..." would use offset 2, a regex of the form `...` would use offset 1.
     * @param backOffset The offset from the end. This separates the final character of the string. For example, a regex of the form #"..." would use backOffset 1, but a regex of the form #''...'' would use backOffset 2.
     * @return A regex.
     * @since 0.9.7
     */
    fun processRegexp(s: String, offset: Int, backOffset: Int): Pattern {
        return Pattern.compile(unescapeRegex(offset, backOffset, s.get(offset - 1), s))
    }
}
