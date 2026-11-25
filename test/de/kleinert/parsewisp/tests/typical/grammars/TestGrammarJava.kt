package de.kleinert.parsewisp.tests.typical.grammars

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.parser.Parser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Test(s) for the Java grammar.
 *
 *
 * Grammar from [the Java 21 language specification, section 19](https://docs.oracle.com/javase/specs/jls/se21/html/jls-19.html).
 */
internal class TestGrammarJava {
    private fun parser(): Parser {
        try {
            return parser(
                Files.readString(Path.of("testres/grammars/java21.grammar"))
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun stats() {
        val p = parser()
        val g = p.grammar
        val stats = g.analyze()
        println(stats.unusedNTs)
    }

    @Test
    fun helloWorld() {
        val text =
            "public class Main { public static void main(String[] args) { System.out.println(\"Hello World!\"); } }"
        Assertions.assertTrue(parser().parse(text).isSuccess)
    }

    @Test
    fun basicTestClass() {
        val text = """
                package de.kleinert.parsewisp.parsing;
                
                import de.kleinert.parsewisp.reduction.ReductionType;
                import org.jetbrains.annotations.NotNull;
                
                // Comment
                public abstract sealed class Terminal
                        extends SimpleRule
                        permits EOFTerm, EpsilonTerm, RegexTerm, StringTerm, ValueRangeTerm {
                    /**
                     * Constructor.
                     *
                     * @param hide The hide option.
                     * @param red  The reduction type for this rule.
                     * @since 0.9.7
                     */
                    protected Terminal create(final boolean hide, final @NotNull ReductionType red) {
                    }
                }
                
                """.trimIndent()
        val res = parser().parse(text)
        Assertions.assertTrue(res.isSuccess)
    }

    @Test
    fun biggerTestClass() {
        val text = """
                package de.kleinert.parsewisp.parsing;
                
                import static de.kleinert.parsewisp.trampoline.TrampolineListenerNode.TrampolineListenerKey;
                
                import de.kleinert.parsewisp.collections.FlatResultSeq;
                import de.kleinert.parsewisp.reduction.ReductionType;
                import org.jetbrains.annotations.NotNull;
                
                /**
                 * "Zero or more" repetition. Represents a production which repeatedly ties to match an input. E.g. {@code P*} matches zero or more.
                 * <p>
                 * Notation: {@code {rule}} or {@code rule*}
                 *
                 * @since 0.9.7
                 */
                public final class ZeroOrMoreRule extends RuleWithChild {
                    private ZeroOrMoreRule(final boolean hide,
                                           final @NotNull ReductionType red,
                                           final @NotNull Rule rule) {
                        super(hide, red, rule);
                    }
                
                    /**
                     * Create a new instance. Depending on the implementation, allows for buffering or create a different type of rule.
                     *
                     * @param rule The {@link Rule} to match repeatedly.
                     * @return A rule.
                     * @since 0.9.7
                     */
                    public static @NotNull Rule create(final @NotNull Rule rule) {
                        if (rule instanceof EpsilonTerm)
                            return rule;
                        return new ZeroOrMoreRule(defaultHidden, defaultReductionType, rule);
                    }
                
                    @Override
                    public void parse(final int index, final @NotNull Gll runner) {
                        final @NotNull Rule rule = getRule();
                        final @NotNull TrampolineListenerKey nodeKeyForStar =
                                new TrampolineListenerKey(index, this);
                        runner.pushListener(
                                new TrampolineListenerKey(index, rule),
                                OnceOrMoreRule.plusListener(FlatResultSeq.make(), rule, index, nodeKeyForStar, runner)
                        );
                        runner.pushSuccessMessageWithoutValue(nodeKeyForStar, index);
                    }
                
                    @Override
                    public void fullParse(final int index, final @NotNull Gll runner) {
                        final @NotNull Rule rule = getRule();
                        final @NotNull TrampolineListenerKey nodeKeyForStar =
                                new TrampolineListenerKey(index, this);
                        if (index == runner.tramp().getText().length()) {
                            runner.pushSuccessMessageWithoutValue(nodeKeyForStar, index);
                            return;
                        }
                        runner.pushListener(
                                new TrampolineListenerKey(index, rule),
                                OnceOrMoreRule.plusFullListener(FlatResultSeq.make(), rule, index, nodeKeyForStar, runner));
                    }
                
                    @Override
                    public @NotNull ZeroOrMoreRule withRule(final @NotNull Rule rule) {
                        return new ZeroOrMoreRule(hide, red, rule);
                    }
                
                    @Override
                    public @NotNull ZeroOrMoreRule withHideTag(boolean hide) {
                        return isHiddenInResult() == hide ? this : new ZeroOrMoreRule(hide, red, rule);
                    }
                
                    @Override
                    public @NotNull ZeroOrMoreRule withReduction(@NotNull ReductionType red) {
                        return getReduction() == red ? this : new ZeroOrMoreRule(hide, red, rule);
                    }
                }
                
                """.trimIndent()
        val res = parser().parse(text)
        Assertions.assertTrue(res.isSuccess)
    }

//    @Test
//    fun testWhichIsAcceptablyBig() {
//        var text = """
//                package de.kleinert.parsewisp.tests.typical.rules_available;
//                class RulesAvailableGeneralizedTests {
//
//                """.trimIndent()
//        text += "private static void doNothing(List opts, boolean run) {}".repeat(500)
//        text += "}"
//        val res = parser().parse(text)
//        Assertions.assertTrue(res.isSuccess)
//    }

//    @Test
//    fun testWhichIsWayTooBig() {
//        var text = """
//                package de.kleinert.parsewisp.tests.typical.rules_available;
//                class RulesAvailableGeneralizedTests {
//
//                """.trimIndent()
//        text += "private static void doNothing(List opts, boolean run) {}".repeat(1000) // Note: Maximum repeat as of 2026.11.21: 3537
//        text += "}"
//        val res = parser().parse(text)
//        Assertions.assertTrue(res.isSuccess)
//    }

//    @Test
//    void testParseParsewispSources() throws IOException {
//        BiPredicate<Path, BasicFileAttributes> matcher = (p, bfa) -> bfa.isRegularFile();
//        final var files = Stream.concat(
//                        Files.find(Paths.get("src"), 999, matcher),
//                        Files.find(Paths.get("test"), 999, matcher))
//                .filter(p -> p.toString().endsWith(".java"))
//                .sorted()
//                .toList();
//        var p = parser();
//
//        long totalTime = 0;
//        long byteCount = 0;
//        long lineCount = 0;
//        long slocCount = 0;
//        long start;
//        long end;
//
//        var counts = new LinkedHashMap<String, Long>();
//
//        for (Path file : files) {
//            var s = Files.readString(file);
//            start = System.nanoTime();
//            var res = p.parse(s);
//            end = System.nanoTime();
//            Assertions.assertTrue(
//                    res.isSuccess,
//                    () -> "Failed testcase.\n" + res.castToParseFailure());
//            long diffMillis = (end - start) / 1_000_000;
//            totalTime += diffMillis;
//
//            var lines = s.lines().count();
//            byteCount += s.length();
//            lineCount += lines;
//
//            var sloc = s.lines()
//                    .map(String::trim)
//                    .filter(Predicate.not(l ->
//                            l.isBlank() || l.startsWith("*") || l.startsWith("//") || l.startsWith("/*")
//                                    || l.startsWith("package") || l.startsWith("import")))
//                    .count();
//            slocCount += sloc;
//
//            System.out.println("Parsed " + file + " in " + diffMillis + " ms (" + s.length() + " chars; " + lines + " lines; " + sloc + " sloc)");
//
//            var parent = file.toFile().getParent();
//            counts.putIfAbsent(parent, 0L);
//            counts.computeIfPresent(parent, (ignore, l) -> l + 1L);
//        }
//        System.out.println("Done. Parsed " + files.size() + " files with a total of " + byteCount + " chars and " + lineCount + " lines (" + slocCount + " sloc). Total time: " + totalTime + " ms");
//        System.out.println("Counts: " + counts);
//    }
}