package de.kleinert.parsewisp.tests

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.parser_options.ParserCreationOptions.Companion.newWithStandardWhitespace
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.result.ParseResult

/**
 * Examples for usage of the library.
 */
object Arithmetic {
    /**
     * We use a simple EBNF grammar for arithmetic expressions on the test input. We do not use any special features yet.
     *
     * @return output for tutorial state
     */
    fun state1(): ParseResult {
        val g = """
                Expression = Term , { ( "+" | "-" ) , Term } ;
                Term       = Factor , { ( "*" | "/" ) , Factor } ;
                Factor     = Number | "(", Expression, ")" ;
                Number     = [ "+" | "-" ] , Digit , { Digit } ;
                Digit      = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
                
                """.trimIndent()
        val p = Parsewisp.parser(g)

        // Since Parsewisp is scannerless, we can not include spaces for now.
        return p.parse("(8-9)*-20/18+1")
    }

    /**
     * We use the alternative string terminals '...' instead of the standard "...". This is still 100% standard EBNF.
     *
     * @return output for tutorial state
     */
    fun state2(): ParseResult {
        val g = """
                Expression = Term , { ( '+' | '-' ) , Term } ;
                Term       = Factor , { ( '*' | '/' ) , Factor } ;
                Factor     = Number | '(', Expression, ')' ;
                Number     = [ '+' | '-' ] , Digit , { Digit } ;
                Digit      = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ;
                
                """.trimIndent()
        val p = Parsewisp.parser(g)

        // Since Parsewisp is scannerless, we can not include spaces for now.
        return p.parse("(8-9)*-20/18+1")
    }

    /**
     * We remove the commas (dividing rules in concatenation) and semicolons (production terminators).
     *
     * @return output for tutorial state
     */
    fun state3(): ParseResult {
        val g = """
                Expression = Term { ( '+' | '-' ) Term }
                Term       = Factor { ( '*' | '/' ) Factor }
                Factor     = Number | '(' Expression ')'
                Number     = [ '+' | '-' ] Digit { Digit }
                Digit      = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
                
                """.trimIndent()
        val p = Parsewisp.parser(g)

        // Since Parsewisp is scannerless, we can not include spaces for now.
        return p.parse("(8-9)*-20/18+1")
    }

    /**
     * Annoyed by needing to write the expression without spaces? I am too. Let's fix that.
     *
     *
     * Add explicit options to the parser's construction like so:
     * <pre>
     * `var options = ParserCreationOptions                 .getDefault()                 .withWhitespaceParser(Parsewisp.parser("whitespace = ' ' | '\\t' | '\\n'")); `
    </pre> *
     * Since this pattern came up so often, a shorter alternative is provided:
     * <pre>
     * `var options = ParserCreationOptions.newWithStandardWhitespace(); `
    </pre> *
     * This modifies the grammar when starting. But the parse tree should remain the same.
     *
     * @return output for tutorial state
     */
    fun state4(): ParseResult {
        val g = """
                Expression = Term { ( '+' | '-' ) Term }
                Term       = Factor { ( '*' | '/' ) Factor }
                Factor     = Number | '(' Expression ')'
                Number     = [ '+' | '-' ] Digit { Digit }
                Digit      = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
                
                """.trimIndent()
        val options = newWithStandardWhitespace()
        val p = Parsewisp.parser(g, options)

        return p.parse("(8 - 9) * -20 / 18 + 1")
    }

    /**
     * You may have noticed that the definition of "Digit" is pretty long. We can replace it with a regular expression (regex).
     * The format for regexes is `#"..."`. The alternative syntax `#'...'` is also available.
     *
     *
     * We need to step out of the standard EBNF territory. We can compress `'0'|...|'9'` into the less annoying regex `#'[0-9]'`.
     *
     * @return output for tutorial state
     */
    fun state5(): ParseResult {
        val g = """
                Expression = Term { ( '+' | '-' ) Term }
                Term       = Factor { ( '*' | '/' ) Factor }
                Factor     = Number | '(' Expression ')'
                Number     = [ '+' | '-' ] Digit { Digit }
                Digit      = #'[0-9]'
                
                """.trimIndent()
        val options = newWithStandardWhitespace()
        val p = Parsewisp.parser(g, options)

        return p.parse("(8 - 9) * -20 / 18 + 1")
    }

    /**
     * We can make the grammar a bit nicer to look at by using '?' for optionals and '*' for repetitions.
     *
     * @return output for tutorial state
     */
    fun state6(): ParseResult {
        val g = """
                Expression = Term (( '+' | '-' ) Term)*
                Term       = Factor (( '*' | '/' ) Factor)*
                Factor     = Number | '(' Expression ')'
                Number     = ('+' | '-')? Digit Digit*
                Digit      = #'[0-9]'
                
                """.trimIndent()
        val options = newWithStandardWhitespace()
        val p = Parsewisp.parser(g, options)

        return p.parse("(8 - 9) * -20 / 18 + 1")
    }

    /**
     * In the "Factor" production, do the '(' and ')' around "Expression" provide any value in the parse tree? Not really? Let's hide them.
     * To hide a rule in the output, wrap it in '&lt;' and '&gt;'.
     *
     * @return output for tutorial state
     */
    fun state7(): ParseResult {
        val g = """
                Expression = Term (( '+' | '-' ) Term)*
                Term       = Factor (( '*' | '/' ) Factor)*
                Factor     = Number | <'('> Expression <')'>
                Number     = ('+' | '-')? Digit Digit*
                Digit      = #'[0-9]'
                
                """.trimIndent()
        val options = newWithStandardWhitespace()
        val p = Parsewisp.parser(g, options)

        return p.parse("(8 - 9) * -20 / 18 + 1")
    }

    /**
     * Parsewisp also supports '+' to shorten our rule "Digit Digit*".
     *
     * @return output for tutorial state
     */
    fun state8(): ParseResult {
        val g = """
                Expression = Term (('+' | '-') Term)*
                Term       = Factor ( ( '*' | '/' ) Factor )*
                Factor     = Number | <'('> Expression <')'>
                Number     = ('+' | '-')? Digit+
                Digit      = #'[0-9]'
                
                """.trimIndent()
        val options = newWithStandardWhitespace()
        val p = Parsewisp.parser(g, options)

        return p.parse("(8 - 9) * -20 / 18 + 1")
    }

    /**
     * We can compress the output further by flattening "Digit" into "Number". To do this, wrap Digit between "&lt;" and "&gt;", but this time on the left.
     *
     * @return output for tutorial state
     */
    fun state9(): ParseResult {
        val g = """
                Expression = Term (('+' | '-') Term)*
                Term       = Factor ( ( '*' | '/' ) Factor )*
                Factor     = Number | <'('> Expression <')'>
                Number     = ('+' | '-')? Digit+
                <Digit>    = #'[0-9]'
                
                """.trimIndent()
        val options = newWithStandardWhitespace()
        val p = Parsewisp.parser(g, options)

        return p.parse("(8 - 9) * -20 / 18 + 1")
    }

    /**
     * If we used ABNF, we could also use value ranges instead of a regular expression for Digit. But we are using EBNF. Still, we can allow that rule explicitly.
     * To do this, we need to modify the creation-options again:
     *
     * @return output for tutorial state
     */
    fun state10(): ParseResult {
        val g = """
                Expression = Term (('+' | '-') Term)*
                Term       = Factor ( ( '*' | '/' ) Factor )*
                Factor     = Number | <'('> Expression <')'>
                Number     = ('+' | '-')? Digit+
                <Digit>    = %d48-57 (* same as %x30-39 *)
                
                """.trimIndent()
        val options = newWithStandardWhitespace()
        val p = Parsewisp.parser(g, options)

        return p.parse("(8 - 9) * -20 / 18 + 1", ParsingOptions.default)
    }

    /**
     * Example main for tutorial.
     *
     * @param args Ignored.
     */
    fun main(args: Array<String>) {
        println("1:  " + state1())
        println("2:  " + state2())
        println("3:  " + state3())
        println("4:  " + state4())
        println("5:  " + state5())
        println("6:  " + state6())
        println("7:  " + state7())
        println("8:  " + state8())
        println("9:  " + state9())
        println("10: " + state10())
    }
}
