package de.kleinert.parsewisp.tests.typical.grammars

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.result.ParseResult
import de.kleinert.parsewisp.util.Transform
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Function

/**
 * Test(s) for the Poetic (esolang) grammar.
 *
 *
 * Example from [esolangs.org/wiki/Poetic_(esolang)](https://esolangs.org/wiki/Poetic_(esolang)#Cat_program).
 * Grammar self-written based on
 */
internal class TestGrammarPoetic {
    private fun parser(): Parser {
        try {
            return parser(
                Files.readString(Path.of("testres/grammars/poetic.grammar")),
                ParserCreationOptions.default
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun transform(pt: ParseResult): String {
        val m: MutableMap<Sym, Function<List<Any?>, Any?>> = HashMap()
        m.put(sym("S"), Function { l -> l })
        m.put(sym("i1"), Function { '[' }) // 1 corresponds to BF's '[' instruction
        m.put(sym("i2"), Function { ']' }) // 2 corresponds to BF's ']' instruction
        m.put(sym("i3"), Function { '+' }) // 3 corresponds to BF's '+' instruction
        m.put(sym("i4"), Function { '-' }) // 4 corresponds to BF's '-' instruction
        m.put(sym("i5"), Function { '>' }) // 5 corresponds to BF's '>' instruction
        m.put(sym("i6"), Function { '<' }) // 6 corresponds to BF's '<' instruction
        m.put(sym("i7"), Function { '.' }) // 7 corresponds to BF's '.' instruction
        m.put(sym("i8"), Function { ',' }) // 8 corresponds to BF's ',' instruction

        // 9 fills the current cell with a random number in 0..255
        m.put(sym("i9"), Function { "[-]" + "+".repeat(Random().nextInt(0, 255)) })

        // 10 terminates the program under certain conditions. I will ignore this.
        m.put(sym("i10"), Function { '_' })

        // Anything over 10 writes the number into the current cell. This is implemented as setting the cell to 0 and then mindlessly incrementing.
        m[sym("imore")] = Function { l -> "[-]" + "+".repeat(l!!.size) }

        return (Transform.transform(pt, m) as List<*>)
            .joinToString { obj: Any? -> obj.toString() }
    }

    fun bfToP() {
        val s = ",+[-.,+]"
        val transformer = mapOf(
            '[' to 1,
            ']' to 2,
            '+' to 3,
            '-' to 4,
            '>' to 5,
            '<' to 6,
            '.' to 7,
            ',' to 8
        )
        //System.out.println(s.chars().map(c -> transformer.getOrDefault((char) c, 0)).mapToObj((i) -> "a".repeat(i) + " ").collect(Collectors.joining("")));
    }

    fun bfToN() {
        val s = ",+[-.,+]"
        val transformer = mapOf(
            '[' to 1,
            ']' to 2,
            '+' to 3,
            '-' to 4,
            '>' to 5,
            '<' to 6,
            '.' to 7,
            ',' to 8
        )
        //System.out.println(s.chars().mapToObj(c -> transformer.getOrDefault((char) c, 0).toString()).collect(Collectors.joining("\n")));
    }

    @Test
    fun cat() {
        val p = parser()
        val code = " ALMIGHTY\n" +  // 8
                " GOD\n" +  // 3
                " I\n" +  // 1
                " HAVE\n" +  // 4
                " ＳⲒＮＮⴹᎠ\n" +  // Filler
                " AGAINST\n" +  // 7
                " ΥΟꓴ,\n" +  // Filler
                " ΑＧΑⲒＮＳꓔ\n" +  // Filler
                " Мꓬ\n" +  // Filler
                " NEIGHBOR\n" +  // 8
                " Ⅰ\n" +  // Filler
                " SINＮⴹᎠ\n" +  // 3 + 3 Filler
                " \uD800\uDE87ꓳꓣＧⅠＶⴹ\n" +  // Filler
                " ME" // 2
        //        System.out.println(p.parse(code));
//        System.out.println(transform(p.parse(code)));
    }

    @Test
    fun asciiLoop() {
        val p = parser()
        val code = " I love is a great mystery\n" +
                "but i couldn't really explain it"
        //        System.out.println(p.parse(code));
//        System.out.println(transform(p.parse(code)));
    }
}