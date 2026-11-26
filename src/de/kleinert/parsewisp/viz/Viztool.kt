package de.kleinert.parsewisp.viz

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.result.Node
import de.kleinert.parsewisp.result.ParseFailureNode
import de.kleinert.parsewisp.result.ParseTree
import java.io.FileNotFoundException
import java.io.IOException
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntConsumer

/**
 * Provides a utility for creating pictures of parse trees. If the trees become too large, the generation might fail.
 * <pre>
 * `var text = "...";     var p = Parsewisp.parser("...");     var rule = Parsewisp.parse(p, text).castToParseSuccess();     println(Viztool.dumpParseTree("vizoutput", rule)); `
</pre> *
 * The tool **dot** must be installed.
 *
 * @since 0.9.7
 */
object Viztool {
    private fun dumpParseTreeHelp(
        printer: PrintStream,
        parseRes: List<Node>,
        count: AtomicInteger
    ): Int {
        val currentId = count.getAndIncrement()
        val label = Viztool.getLabel(parseRes.get(0)!!)

        printer.print(currentId.toString() + "[shape=box, label=\"" + label + "\"];")
        parseRes.stream().skip(1)
            .mapToInt { child: Node ->
                dumpParseTreeHelp(
                    printer,
                    if (child.content() is ParseTree) child.content() as ParseTree else listOf<Node>(child),
                    count
                )
            }
            .forEach(IntConsumer { childId: Int ->
                printer.append(currentId.toString())
                    .append(" -> ")
                    .append(childId.toString())
                    .append(";")
            })
        return currentId
    }

    private fun getLabel(node: Node): String {
        val label: String
        val nodeContent = node.content()
        label = if (nodeContent is String) {
            nodeContent
        } else if (nodeContent is Sym) {
            nodeContent.name();
        } else if (nodeContent is ParseFailureNode) {
            throw  IllegalStateException("Cannot create parse-tree visualization for $node (TODO).");
        } else if (nodeContent is ParseTree) {
            throw  IllegalStateException("This case should be handled in dumpParseTreeHelp.");
        } else {
            throw  IllegalStateException(node.javaClass.name);
        }
        return label.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    /**
     * Creates a picture.
     * Example:
     * <pre>
     * `var text = "abc";     var p = Parsewisp.parser("S : A 'bc'\nA : 'a'");     var rule = Parsewisp.parse(p, text).castToParseSuccess(); // Parse tree [:S, [:A, 'a'], 'bc']     println(Viztool.dumpParseTree("vizoutput", rule)); `
    </pre> *
     *
     * @param dotFileNamePrefix Filename without file format.
     * @param parseRes          The parse tree.
     * @return The return code. That is 0 on success or another number on failure.
     * @throws IOException          If the file can't be created or written to.
     * @throws InterruptedException If the operation is interrupted somehow.
     * @since 0.9.7
     */
    fun dumpParseTree(
        dotFileNamePrefix: String,
        parseRes: ParseTree
    ): Int {
        val dotFileName = dotFileNamePrefix + ".dot"
        val pngFileName = dotFileNamePrefix + ".png"
        val args = arrayOf<String>("dot", "-Tpng", dotFileName, "-o", pngFileName)

        try {
            PrintStream(dotFileName).use { printer ->
                printer.print("digraph G {")
                dumpParseTreeHelp(printer, parseRes, AtomicInteger())
                printer.print("}")
            }
        } catch (e: FileNotFoundException) {
            throw RuntimeException(e)
        }

        return Runtime.getRuntime().exec(args).waitFor()
    }
}
