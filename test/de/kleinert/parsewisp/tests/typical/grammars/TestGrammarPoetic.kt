package de.kleinert.parsewisp.tests.typical.grammars

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parser.Parser
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.result.ParseResult
import de.kleinert.parsewisp.util.Transform
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random

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
            return Parsewisp.parser(
                Files.readString(Path.of("testres/grammars/poetic.grammar")),
                ParserCreationOptions.default
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun transform(pt: ParseResult): String {
        val m: MutableMap<Sym, (List<Any?>)-> Any?> = mutableMapOf()
        m[Sym.sym("S")] = { l -> l }
        m[Sym.sym("i1")] = { '[' } // 1 corresponds to BF's '[' instruction
        m[Sym.sym("i2")] = { ']' } // 2 corresponds to BF's ']' instruction
        m[Sym.sym("i3")] = { '+' } // 3 corresponds to BF's '+' instruction
        m[Sym.sym("i4")] = { '-' } // 4 corresponds to BF's '-' instruction
        m[Sym.sym("i5")] = { '>' } // 5 corresponds to BF's '>' instruction
        m[Sym.sym("i6")] = { '<' } // 6 corresponds to BF's '<' instruction
        m[Sym.sym("i7")] = { '.' } // 7 corresponds to BF's '.' instruction
        m[Sym.sym("i8")] = { ',' } // 8 corresponds to BF's ',' instruction

        // 9 fills the current cell with a random number in 0..255
        m[Sym.sym("i9")] = { "[-]" + "+".repeat(Random.nextInt(0, 255)) }

        // 10 terminates the program under certain conditions. I will ignore this.
        m[Sym.sym("i10")] = { '_' }

        // Anything over 10 writes the number into the current cell. This is implemented as setting the cell to 0 and then mindlessly incrementing.
        m[Sym.sym("imore")] =  { l -> "[-]" + "+".repeat(l.size) }

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