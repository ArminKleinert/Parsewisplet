package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.trampoline.ListenerKey
import java.util.*
import java.util.function.IntPredicate
import java.util.regex.Pattern

/**
 * Represents terminal expressions from regular expressions.
 * This class has the property of being both more powerful and uniquely dangerous to use.
 * When parsing, the regex is eagerly, not lazily, resolved.
 * <br></br>
 * This means that the regex `"a+"` on the input "aaaab" will always match all four "a"s,
 * instead of returning different results "a", "aa", "aaa" and "aaaa".
 * <br></br>
 * Syntax: A string literal prefixed with a hash-symbol: `#"..."` and `#'...'` are equivalent.
 *
 * @since 0.9.7
 */
class RegexTerm private constructor(
    hide: Boolean,
    red: ReductionType,
    /**
     * The regex/pattern.
     *
     * @return The regex.
     * @since 0.9.7
     */
    val regexp: Pattern
) : Terminal(hide, red) {
    private fun reMatchAtFront(text: CharSequence): String? {
        val matcher = regexp.matcher(text)
        if (matcher.lookingAt()) return matcher.group()
        return null
    }

    /**
     * Parse the string using the regex. Normally, the longest match is eagerly taken.
     * So by default, the following is true:
     * <pre>
     * `var p = Parsewisp.parser("S : #'A+' 'A'");     Assertions.assertTrue(p.parse("AAA").isFailure()); // Parsing failed because #'A+' eagerly matched the entire input. `
    </pre> *
     * If the [ParsingOptions.iterativeDeepening] option is true, the parse would succeed.
     * <pre>
     * `var p = Parsewisp.parser("S : #'A+' 'A'");     var opts = ParsingOptions.getDefault().withIterativeDeepening(true);     Assertions.assertEquals(ParseTree.create("S", "AA", "A"), p.parse("AAA", opts)); `
    </pre> *
     *
     * @param index  The start index.
     * @param runner Helper structure.
     * @since 0.9.7
     */
    override fun parse(index: Int, runner: Gll) {
        val subString = runner.tramp().getText1().substring(index)
        val nodeKey: ListenerKey = Rule.Companion.makeKey(index, this)
        val firstMatch = reMatchAtFront(subString)

        if (firstMatch == null) {
            runner.fail(nodeKey, index, this, false)
            return
        }

        val endIndexInSubString = firstMatch.length

        if (!runner.iterativeDeepening()) {
            runner.pushSuccessMessage(nodeKey, firstMatch, index + endIndexInSubString)
            return
        }

        runner.pushSuccessMessage(nodeKey, firstMatch, index + endIndexInSubString)
        for (end in endIndexInSubString downTo 0) {
            val match = reMatchAtFront(subString.substring(0, end))
            if (match != null) {
                runner.pushSuccessMessage(nodeKey, match, index + match.length)
            }
        }
    }

    override fun fullParse(index: Int, runner: Gll) {
        val substring = runner.tramp().getText1().substring(index)
        val match = reMatchAtFront(substring)
        val nodeKey: ListenerKey = Rule.Companion.makeKey(index, this)
        if (match != null && match.length == substring.length) {
            runner.pushSuccessMessage(nodeKey, match, runner.tramp().getText1().length)
        } else {
            runner.fail(nodeKey, index, this, true)
        }
    }

    override fun withHideTag(hide: Boolean): RegexTerm {
        return if (isHiddenInResult1() == hide) this else RegexTerm(hide, red, regexp)
    }

    override fun withReduction(red: ReductionType): RegexTerm {
        return if (getReduction1() === red) this else RegexTerm(hide, red, regexp)
    }

    override fun equals(o: Any?): Boolean {
        if (o !is RegexTerm) return false
        if (this === o) return true
        return hide == o.hide && red == o.red
                && regexp.pattern() == o.regexp.pattern()
    }

    override fun hashCode(): Int {
        return Objects.hash(hide, red, regexp.pattern())
    }

    companion object {
        /**
         * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
         *
         * @param regexp The pattern.
         * @return A rule.
         * @since 0.9.7
         */
        fun create(regexp: Pattern): Rule {
            val s = regexp.pattern()
            if (regexp.flags() == 0 && s.chars()
                    .allMatch(IntPredicate { codePoint: Int -> Character.isLetterOrDigit(codePoint) })
            ) return StringTerm.Companion.createDirect(s, false)
            return RegexTerm(Rule.Companion.defaultHidden, Rule.Companion.defaultReductionType, regexp)
        }
    }
}
