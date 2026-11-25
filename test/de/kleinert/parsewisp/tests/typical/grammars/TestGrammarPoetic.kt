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
import java.util.Map
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.Any
import kotlin.Char
import kotlin.Int
import kotlin.RuntimeException
import kotlin.streams.toList

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
                ParserCreationOptions.getDefault()
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun transform(pt: ParseResult): String {
        val m: MutableMap<Sym?, Function<MutableList<Any?>?, Any?>?> =
            HashMap<Sym?, Function<MutableList<Any?>?, Any?>?>()
        m.put(sym("S"), Function { l: MutableList<Any?>? -> l })
        m.put(sym("i1"), Function { l: MutableList<Any?>? -> '[' }) // 1 corresponds to BF's '[' instruction
        m.put(sym("i2"), Function { l: MutableList<Any?>? -> ']' }) // 2 corresponds to BF's ']' instruction
        m.put(sym("i3"), Function { l: MutableList<Any?>? -> '+' }) // 3 corresponds to BF's '+' instruction
        m.put(sym("i4"), Function { l: MutableList<Any?>? -> '-' }) // 4 corresponds to BF's '-' instruction
        m.put(sym("i5"), Function { l: MutableList<Any?>? -> '>' }) // 5 corresponds to BF's '>' instruction
        m.put(sym("i6"), Function { l: MutableList<Any?>? -> '<' }) // 6 corresponds to BF's '<' instruction
        m.put(sym("i7"), Function { l: MutableList<Any?>? -> '.' }) // 7 corresponds to BF's '.' instruction
        m.put(sym("i8"), Function { l: MutableList<Any?>? -> ',' }) // 8 corresponds to BF's ',' instruction

        // 9 fills the current cell with a random number in 0..255
        m.put(sym("i9"), Function { l: MutableList<Any?>? -> "[-]" + "+".repeat(Random().nextInt(0, 255)) })

        // 10 terminates the program under certain conditions. I will ignore this.
        m.put(sym("i10"), Function { l: MutableList<Any?>? -> '_' })

        // Anything over 10 writes the number into the current cell. This is implemented as setting the cell to 0 and then mindlessly incrementing.
        m.put(sym("imore"), Function { l: MutableList<Any?>? -> "[-]" + "+".repeat(l!!.size) })

        return (Transform.transform(pt, m) as MutableList<*>)
            .stream()
            .map { obj: Any? -> obj.toString() }
            .toList()
            .joinToString()
    }

    fun bfToP() {
        val s = ",+[-.,+]"
        val transformer = Map.of<Char?, Int?>(
            '[', 1,
            ']', 2,
            '+', 3,
            '-', 4,
            '>', 5,
            '<', 6,
            '.', 7,
            ',', 8
        )
        //System.out.println(s.chars().map(c -> transformer.getOrDefault((char) c, 0)).mapToObj((i) -> "a".repeat(i) + " ").collect(Collectors.joining("")));
    }

    fun bfToN() {
        val s = ",+[-.,+]"
        val transformer = Map.of<Char?, Int?>(
            '[', 1,
            ']', 2,
            '+', 3,
            '-', 4,
            '>', 5,
            '<', 6,
            '.', 7,
            ',', 8
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