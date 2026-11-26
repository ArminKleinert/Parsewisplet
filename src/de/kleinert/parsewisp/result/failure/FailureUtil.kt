package de.kleinert.parsewisp.result.failure

import de.kleinert.parsewisp.result.ParseFailure
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * Utilities for printing and modifying failure objects. Calling these methods from outside the parsing algorithm is heavily discouraged and might result in immediate termination of the user.
 *
 * @since 0.9.7
 */
object FailureUtil {
    /**
     * Creates string with caret at nth position, 1-based
     * and accounts for horizontal tabs which might change
     * the alignment of the '^' to the error location.
     *
     * @param failure The failure.
     * @param text    The text.
     * @return A new string.
     * @since 0.9.7
     */
    fun augmentFailure(
        failure: ParseFailure,
        text: String
    ): ParseFailure {
        var line = 1
        var col = 1

        val index = failure.index

        val charCodes = text.chars().iterator()
        for (counter in 0..<index) {
            val code = charCodes.nextInt()
            if (code == '\n'.code) {
                line++
                col = 1
                //            } else if (code == (int) '\t') {
//                col += 4;
            } else {
                col++
            }
        }

        val lineText = text.lineSequence().drop(line - 1).firstOrNull()
        return ParseFailure(index, failure.reasonList, line, col, lineText)
    }

    /**
     * Creates string with caret at nth position, 1-based
     * and accounts for horizontal tabs which might change
     * the alignment of the '^' to the error location.
     *
     * @param text The text.
     * @param n    The index.
     * @return Indentation and marker.
     * @since 0.9.7
     */
    fun marker(text: String?, n: Int): String {
        if (text == null) return "<No text>"

        val reg = Pattern.compile("\\S")
        val markerText = reg.matcher(text).replaceAll(" ")
        if (n <= 1) return "^"
        return markerText.substring(0, n - 1) + '^'
    }

    /**
     * Adds a failure reasonList to a failure object if the next index (parameter8 us greater than the failure's index.
     *
     * @param failure   The failure object.
     * @param newReason The new reasonList.
     * @param nextIndex The next index for the reasonList.
     * @return A modified failure object.
     * @since 0.9.7
     */
    fun modifyFailureByIndex(
        failure: ParseFailure?,
        newReason: ParseFailureReason,
        nextIndex: Int
    ): ParseFailure {
        val currentIndex = if (failure == null) 0 else failure.index
        if (nextIndex > currentIndex)
            return ParseFailure(nextIndex, ArrayList(listOf(newReason)));
        if (nextIndex < currentIndex) return failure!!

        val newReasonList = ArrayList(if (failure == null) listOf() else failure.reasonList);
        newReasonList.add(newReason);
        return ParseFailure(nextIndex, newReasonList);
    }

    /**
     * Creates a nicely formatted string for a failure object.
     *
     * @param failure The failure.
     * @return The string.
     * @since 0.9.7
     */
    fun pprintFailure(failure: ParseFailure): String {
        val line = (failure.line as Number).toInt()
        val column = (failure.column as Number).toInt()
        val text = failure.text
        val reason = failure.reasonList

        val sb = StringBuilder()
        sb.append("Parse error at line ").append(line).append(", column ").append(column).append(";\n")
        if (text != null) {
            sb.append(text).append('\n')
            sb.append(marker(text, column)).append('\n')
        } else {
            sb.append("<No text provided.>\n")
        }

        val fullReasons: List<String> = reason
            .filter { obj: ParseFailureReason -> obj.untilEndOfInput }
            .map { obj: ParseFailureReason -> obj.failureReasonString() }
            .distinct()
        val partialReasons = reason
            .filterNot({ obj: ParseFailureReason -> obj.untilEndOfInput() })
            .map { obj: ParseFailureReason? -> obj!!.failureReasonString() }
            .distinct()

        val total = fullReasons.size + partialReasons.size

        if (total == 1) {
            sb.append("Expected:").append('\n')
        } else if (total > 1) {
            sb.append("Expected one of:").append('\n')
        }

        for (fullReasonExpect in fullReasons) {
            sb.append(fullReasonExpect)
            if (fullReasonExpect != "end-of-string") sb.append(" (follow by end-of-string)")
            sb.append('\n')
        }

        for (partialReasonExpect in partialReasons) {
            sb.append(partialReasonExpect).append('\n')
        }

        return sb.toString()
    }
}
