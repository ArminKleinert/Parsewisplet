package de.kleinert.parsewisp.parsing

import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.collections.FlatResultSeq
import de.kleinert.parsewisp.functions.Listener
import de.kleinert.parsewisp.functions.NegativeListener
import de.kleinert.parsewisp.functions.Procedure
import de.kleinert.parsewisp.grammar.Grammar
import de.kleinert.parsewisp.grammar.GrammarPrinter
import de.kleinert.parsewisp.parser_options.ParsingOptions
import de.kleinert.parsewisp.reduction.ReductionType
import de.kleinert.parsewisp.result.*
import de.kleinert.parsewisp.result.ParsesResult.LazyResultList
import de.kleinert.parsewisp.result.failure.FailureUtil
import de.kleinert.parsewisp.result.failure.ParseFailureReason
import de.kleinert.parsewisp.result.success.ParseMessage
import de.kleinert.parsewisp.trampoline.ListenerKey
import de.kleinert.parsewisp.trampoline.ListenerNode
import de.kleinert.parsewisp.trampoline.MsgCacheKey
import de.kleinert.parsewisp.trampoline.Tramp
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.IntFunction

/**
 * This class provides the general parsing algorithms.
 *
 * @since 0.9.7
 */
class Gll private constructor(
    private val tramp: Tramp,
    private val iterativeDeepening: Boolean,
    val grammarPrinter: GrammarPrinter,
    val stopEarlyOnEmptyRepetition: Boolean
) {
    fun tramp(): Tramp {
        return tramp
    }

    fun iterativeDeepening(): Boolean {
        return iterativeDeepening
    }

    private fun getOrCreateListenerNode(
        nodeKey: ListenerKey
    ): ListenerNode {
        var node = tramp.getNode(nodeKey)

        if (node != null) return node

        node = ListenerNode()
        tramp.addToNodes(nodeKey, node)
        return node
    }

    private fun listenerExists_Q(
        nodeKey: ListenerKey
    ): Boolean {
        val node = tramp.getNode(nodeKey)
        if (node == null) return false
        return !node.listeners.isEmpty()
    }

    private fun fullListenerExists_Q(
        nodeKey: ListenerKey
    ): Boolean {
        val node = tramp.getNode(nodeKey)
        if (node == null) return false
        return !node.listeners.isEmpty() || !node.fullListeners.isEmpty()
    }

    fun pushNegativeListener(
        creator: ListenerKey,
        negativeListener: NegativeListener
    ) {
        tramp.pushNegativeListener(creator, negativeListener)
    }

    private fun pushMessage(
        listener: Listener,
        result: ParseMessage
    ) {
        val i = result.index()
        val k = MsgCacheKey(i, listener)
        val c = tramp.getFromMsgCache(k, 0)
        val f = Procedure { listener.execute(result) }
        if (c > tramp.getGeneration1()) {
            tramp.addToNextStack(f)
        } else {
            tramp.addToStack(f)
        }
        tramp.addToMsgCache(k, c + 1)
    }

    private fun pushStack(item: Procedure) {
        tramp.addToStack(item)
    }

    private fun step() {
        val top = tramp.getStack1().get(tramp.getStack1().size - 1)
        tramp.popStack()
        top.execute()
    }

    private fun run(
        maxResults: Int
    ): LazyResultList {
        val foundResult = AtomicBoolean(false)
        return LazyResultList(IntFunction { i: Int -> run(foundResult) }, maxResults)
    }

    private fun run(
        foundResult: AtomicBoolean
    ): ParseTree? {
        while (true) {
            if (tramp.success != null) {
                val successResult: ParseMessage = tramp.success!!
                val resultTree = successResult.result
                check(resultTree is ParseTree) { successResult.toString() + " in instance " + this.instanceIdForDebug }
                tramp.success = null
                foundResult.set(true)
                return resultTree
            }
            val stack: MutableList<Procedure> = tramp.getStack1()
            if (!stack.isEmpty()) {
                step()
                continue  // Take it to the top.
            }

            val negativeListeners = tramp.getNegativeListeners1()
            if (!negativeListeners.isEmpty()) {
                val latestIndexAndNegListeners = negativeListeners.lastEntry()
                val latestNegListeners = latestIndexAndNegListeners.value
                val latestListener = latestNegListeners.removeAt(latestNegListeners.size - 1)

                latestListener.execute()

                if (latestNegListeners.isEmpty()) {
                    tramp.getNegativeListeners1().remove(latestIndexAndNegListeners.key)
                }

                continue  // Take it to the top.
            }

            if (foundResult.get()) {
                tramp.nextGeneration()
                foundResult.set(false)
                continue  // Take it to the top.
            }
            return null // Fail
        }
    }

    fun pushListener(
        nodeKey: ListenerKey,
        listener: Listener
    ) {
        val listenerAlreadyExists = listenerExists_Q(nodeKey)
        val node = getOrCreateListenerNode(nodeKey)
        node.listeners.add(listener)
        for (result in node.results) {
            pushMessage(listener, result)
        }
        for (fullResult in node.fullResults) {
            pushMessage(listener, fullResult)
        }
        if (!listenerAlreadyExists) {
            pushStack(Procedure { nodeKey.rule.parse(nodeKey.index, this) })
        }
    }

    fun pushFullListener(
        nodeKey: ListenerKey,
        listener: Listener
    ) {
        //GllParsers.pushFullListenerCallback.invoke(tramp, nodeKey, listener);
        val fullListenerAlreadyExists = fullListenerExists_Q(nodeKey)
        val node = getOrCreateListenerNode(nodeKey)
        node.fullListeners.add(listener)
        for (fullResult in node.fullResults) {
            pushMessage(listener, fullResult)
        }
        if (!fullListenerAlreadyExists) {
            pushStack(Procedure { nodeKey.rule.fullParse(nodeKey.index, this) })
        }
    }

    /**
     * Pushes a result into the trampoline's node.
     * Categorizes as either result or full result.
     * Schedules notification to all existing listeners of result.
     * (Full listeners only get notified about full results)
     */
    private fun pushResultHelper(
        nodeKey: ListenerKey,
        result: ParseMessage
    ) {
        var result = result
        val node = getOrCreateListenerNode(nodeKey)
        val rule = nodeKey.rule
        if (rule.isHiddenInResult) {
            result = result.reset()
        }
        if (rule.reduction.getReductionType1() != ReductionType.Type.INITIAL) {
            val tree: ParseTree = ParseTree.Companion.create(
                rule.reduction.getKey1(), result.result,
                nodeKey.index, result.index()
            )
            result = ParseMessage.Companion.create(result.index(), tree)
        }

        val reachedEndOfInput = tramp.getText1().length == result.index()

        val resultExisted: Boolean
        if (reachedEndOfInput) {
            resultExisted = node.fullResults.add(result)
        } else {
            resultExisted = node.results.add(result)
        }
        if (!resultExisted) {
            return
        }

        for (listener in node.listeners) {
            pushMessage(listener, result)
        }

        if (!reachedEndOfInput) {
            return
        }

        for (fullListener in node.fullListeners) {
            pushMessage(fullListener, result)
        }
    }

    private fun startParser(
        tramp: Tramp,
        rule: Rule,
        partial: Boolean
    ) {
        if (partial) {
            pushListener(ListenerKey(0, rule), Listener { success: ParseMessage -> tramp.success = success })
        } else {
            pushFullListener(ListenerKey(0, rule), Listener { success: ParseMessage -> tramp.success = success })
        }
    }

    fun pushErrorMessage(
        nodeKey: ListenerKey,
        result: ParseFailureNode,
        end: Int
    ) {
        pushResultHelper(nodeKey, ParseMessage.Companion.create(end, result))
    }

    fun pushSuccessMessage(
        nodeKey: ListenerKey,
        result: String,
        end: Int
    ) {
        val aps: ParseMessage = ParseMessage.Companion.create(end, result)
        pushResultHelper(nodeKey, aps)
    }

    fun pushSuccessMessageWithoutValue(
        nodeKey: ListenerKey,
        end: Int
    ) {
        pushResultHelper(nodeKey, ParseMessage.Companion.create(end))
    }

    fun pushSuccessMessage(
        nodeKey: ListenerKey,
        result: FlatResultSeq,
        end: Int
    ) {
        val aps: ParseMessage = ParseMessage.Companion.create(end, result)
        pushResultHelper(nodeKey, aps)
    }

    fun pushSuccessAgainWithNewKey(
        nodeKey: ListenerKey,
        message: ParseMessage
    ) {
        pushResultHelper(nodeKey, message)
    }

    fun fail(
        nodeKey: ListenerKey,
        index: Int,
        rule: Rule,
        untilEnd: Boolean
    ) {
        tramp.setFailure(
            FailureUtil.modifyFailureByIndex(
                tramp.getFailure(),
                ParseFailureReason.Companion.creatureFailReason(rule, untilEnd, this.grammarPrinter),
                index
            )
        )
        if (index == tramp.getFailIndex1()) {
            val subSeq = tramp.getText1().substring(index)
            val textLen = tramp.getText1().length
            pushErrorMessage(
                nodeKey,
                buildFailureNode(sym("failure"), subSeq, index, tramp.getText1().length),
                textLen
            )
        }
    }

    private fun buildFailureNode(
        key: Sym,
        text: String,
        start: Int,
        end: Int
    ): ParseFailureNode {
        return ParseFailureNode(text, key, start, end)
    }

    fun nodeListener(
        nodeKey: ListenerKey
    ): Listener {
        return Listener { result: ParseMessage? -> pushResultHelper(nodeKey, result!!) }
    }

    private val instanceIdForDebug: String?
        get() = this.toString()

    companion object {
        private fun parsesEmbedFailureAfterFail(
            grammar: Grammar,
            start: Sym,
            text: String,
            partial: Boolean,
            iterativeDeepening: Boolean,
            printer: GrammarPrinter,
            stopRepetitionsIfInfiniteRepetitionHappens: Boolean,
            resultLimit: Int
        ): ParsesResult {
            val tramp = Tramp(grammar, text, 0)
            val parser: NonTerminal = NonTerminal.Companion.create(start)
            val gll = Gll(tramp, iterativeDeepening, printer, stopRepetitionsIfInfiniteRepetitionHappens)
            gll.startParser(tramp, parser, partial)
            val allParses = gll.run(resultLimit)
            return ParsesResult.Companion.make(allParses)
        }

        /**
         * This method should not be called directly. Use [Parser.parses] or [Parser.parses] instead.
         *
         * @param grammar                    The grammar.
         * @param start                      The name of the start production.
         * @param text                       The text.
         * @param partial                    Whether to include partial results.
         * @param iterativeDeepening         Iteratively deepens the evaluation of [RegexTerm.parse].
         * @param errorIfEmpty               If true, return an error if the parsing failed. By default, an empty list would be returned.
         * @param printer                    The printer used for the parser. In the current version, it's used to format failure info if parsing fails.
         * @param stopEarlyOnEmptyRepetition Whether to end resolution of [VariableRepetitionRule] parses early when the rule first successfully parses an empty string.
         * @param resultLimit                Maximum number of results. See [ParsingOptions.resultLimit].
         * @return The parse forest.
         * @see Parser.parses
         * @see Parser.parses
         * @since 0.9.7
         */
        fun parses(
            grammar: Grammar,
            start: Sym,
            text: String,
            partial: Boolean,
            iterativeDeepening: Boolean,
            errorIfEmpty: Boolean,
            printer: GrammarPrinter,
            stopEarlyOnEmptyRepetition: Boolean,
            resultLimit: Int
        ): ParsesResult {
            val tramp = Tramp(grammar, text)
            val parser: NonTerminal = NonTerminal.Companion.create(start)
            val gll = Gll(tramp, iterativeDeepening, printer, stopEarlyOnEmptyRepetition)
            gll.startParser(tramp, parser, partial)
            val allParses = gll.run(resultLimit)
            if (errorIfEmpty && allParses.isEmpty()) {
                checkNotNull(tramp.getFailure()) { "Parsing failed. Perhaps the grammar was non-productive?" }
                val apf = FailureUtil.augmentFailure(tramp.getFailure()!!, text)
                return ParsesResult.Companion.make(apf)
            }
            return ParsesResult.Companion.make(allParses)
        }

        /**
         * This method should not be called directly. Use [Parser.parse] or [Parser.parse] instead.
         *
         * @param grammar            The grammar.
         * @param start              The name of the start production.
         * @param text               The text.
         * @param partial            Whether to include partial results.
         * @param iterativeDeepening Iteratively deepens the evaluation of [RegexTerm.parse].
         * @param printer            The printer used for the parser. In the current version, it's used to format failure info if parsing fails.
         * @return The parse tree or failure.
         * @see Parser.parse
         * @see Parser.parse
         * @since 0.9.7
         */
        fun parse(
            grammar: Grammar,
            start: Sym,
            text: String,
            partial: Boolean,
            iterativeDeepening: Boolean,
            printer: GrammarPrinter
        ): ParseResult {
            val tramp = Tramp(grammar, text)
            val gll = Gll(
                tramp, iterativeDeepening, printer,
                ParsingOptions.Companion.DEFAULT_STOP_EARLY_ON_EMPTY_REPETITION
            )
            val parser: NonTerminal = NonTerminal.Companion.create(start)
            gll.startParser(tramp, parser, partial)
            val allParses = gll.run(1)
            if (allParses.isEmpty()) {
                checkNotNull(tramp.getFailure()) { "Parsing failed. Perhaps the grammar was non-productive?" }
                return ParseResult.Companion.make(FailureUtil.augmentFailure(tramp.getFailure()!!, text))
            }
            return ParseResult.make(allParses.first())
        }

        /**
         * This method should not be called directly. Use [Parser.parses] with [ParsingOptions.embedFailureInParseTree] set to true instead.
         *
         * @param grammar                    The grammar.
         * @param start                      The name of the start production.
         * @param text                       The text.
         * @param partial                    Whether to include partial results.
         * @param iterativeDeepening         Iteratively deepens the evaluation of [RegexTerm.parse].
         * @param printer                    The printer used for the parser. In the current version, it's used to format failure info if parsing fails.
         * @param stopEarlyOnEmptyRepetition Whether to end resolution of [VariableRepetitionRule] parses early when the rule first successfully parses an empty string.
         * @param resultLimit                Maximum number of results. See [ParsingOptions.resultLimit].
         * @return The parse forest.
         * @see Parser.parses
         * @see ParsingOptions.embedFailureInParseTree
         * @since 0.9.7
         */
        fun parsesEmbedFailure(
            grammar: Grammar,
            start: Sym,
            text: String,
            partial: Boolean,
            iterativeDeepening: Boolean,
            printer: GrammarPrinter,
            stopEarlyOnEmptyRepetition: Boolean,
            resultLimit: Int
        ): ParsesResult {
            val allParses: ParsesResult = parses(
                grammar, start, text, partial, iterativeDeepening, false, printer,
                stopEarlyOnEmptyRepetition, resultLimit
            )
            if (!allParses.castToParsesSuccess().isEmpty()) return ParsesResult.Companion.make(allParses)
            return parsesEmbedFailureAfterFail(
                grammar, start, text, partial, iterativeDeepening, printer,
                stopEarlyOnEmptyRepetition, resultLimit
            )
        }

        private fun parseEmbedFailureAfterFail(
            grammar: Grammar,
            start: Sym,
            text: String,
            failIndex: Int,
            partial: Boolean,
            iterativeDeepening: Boolean,
            printer: GrammarPrinter
        ): ParseResult {
            val tramp = Tramp(grammar, text, failIndex)
            val parser: NonTerminal = NonTerminal.Companion.create(start)
            val gll =
                Gll(tramp, iterativeDeepening, printer, ParsingOptions.Companion.DEFAULT_STOP_EARLY_ON_EMPTY_REPETITION)
            gll.startParser(tramp, parser, partial)
            val allParses = gll.run(1)
            if (!allParses.isEmpty()) return ParseResult.make(allParses.first())
            return gll.buildFailureNode(start, text, 0, text.length)
        }

        /**
         * This method should not be called directly. Use [Parser.parse] with [ParsingOptions.embedFailureInParseTree] set to true instead.
         *
         * @param grammar            The grammar.
         * @param start              The name of the start production.
         * @param text               The text.
         * @param partial            Whether to include partial results.
         * @param iterativeDeepening Iteratively deepens the evaluation of [RegexTerm.parse].
         * @param printer            The printer used for the parser. In the current version, it's used to format failure info if parsing fails.
         * @return The parse tree or failure.
         * @see Parser.parse
         * @see ParsingOptions.embedFailureInParseTree
         * @since 0.9.7
         */
        fun parseEmbedFailure(
            grammar: Grammar,
            start: Sym,
            text: String,
            partial: Boolean,
            iterativeDeepening: Boolean,
            printer: GrammarPrinter
        ): ParseResult {
            val result: ParseResult = parse(grammar, start, text, partial, iterativeDeepening, printer)
            if (result !is ParseFailure) return result
            return parseEmbedFailureAfterFail(grammar, start, text, result.index, partial, iterativeDeepening, printer)
        }
    }
}
