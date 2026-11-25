package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp.parser
import de.kleinert.parsewisp.Sym.Companion.sym
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class CharRangeTest {
    private val options = ParserCreationOptions.getDefault()

    @Test
    fun unicodeCodepointSingleParse() {
        val parser = parser("S = %x1F381", options)
        Assertions.assertTrue(parser.parse("\uD83C\uDF80").isFailure())
        Assertions.assertEquals(PT.create("S", "🎁"), parser.parse("🎁"))
        Assertions.assertTrue(parser.parse("\uD83C\uDF82").isFailure())
        Assertions.assertTrue(parser.parse("\uD83C\uDF83").isFailure())
    }

    @Test
    fun unicodeCodepointSingleParse2() {
        val parser = parser("S = %x1F381-1F381", options)
        Assertions.assertTrue(parser.parse("\uD83C\uDF80").isFailure())
        Assertions.assertEquals(PT.create("S", "🎁"), parser.parse("🎁"))
        Assertions.assertTrue(parser.parse("\uD83C\uDF82").isFailure())
        Assertions.assertTrue(parser.parse("\uD83C\uDF83").isFailure())
    }

    @Test
    fun unicodeCodepointShortRangeParse() {
        val parser = parser("S = %x1F381-1F382", options)
        Assertions.assertTrue(parser.parse("\uD83C\uDF80").isFailure())
        Assertions.assertEquals(PT.create("S", "🎁"), parser.parse("🎁"))
        Assertions.assertEquals(PT.create("S", "\uD83C\uDF82"), parser.parse("\uD83C\uDF82"))
        Assertions.assertTrue(parser.parse("\uD83C\uDF83").isFailure())
    }

    @Test
    fun unicodeCodepointLongRangeParse() {
        val parser = parser("S = %x41-1F382", options)
        Assertions.assertTrue(parser.parse("\uD83C\uDF83").isFailure())
        Assertions.assertEquals(PT.create("S", "A"), parser.parse("A"))
        Assertions.assertEquals(PT.create("S", "🎁"), parser.parse("🎁"))
        Assertions.assertEquals(PT.create("S", "\uD83C\uDF82"), parser.parse("\uD83C\uDF82"))
    }

    @Test
    fun unicodeCodepointSmallCharParse() {
        val parser = parser("S = %x41-5A", options)
        Assertions.assertTrue(parser.parse("\uD83C\uDF80").isFailure())
        Assertions.assertEquals(PT.create("S", "A"), parser.parse("A"))
        Assertions.assertEquals(PT.create("S", "Z"), parser.parse("Z"))
        Assertions.assertTrue(parser.parse("\uD83C\uDF81").isFailure())
    }

    @Test
    fun unicodeCodepointBinaryDecimalHexEquivalenceRange() {
        val parserBin = parser("S = %b1000001-1011010", options).grammar.getProduction(sym("S"))
        val parserDec = parser("S = %d65-90", options).grammar.getProduction(sym("S"))
        val parserHex = parser("S = %x41-5A", options).grammar.getProduction(sym("S"))
        Assertions.assertEquals(parserBin, parserDec)
        Assertions.assertEquals(parserBin, parserHex)
    }

    @Test
    fun unicodeCodepointBinaryDecimalHexEquivalenceSingle() {
        val parserBin = parser("S = %b1000001", options).grammar.getProduction(sym("S"))
        val parserDec = parser("S = %d65", options).grammar.getProduction(sym("S"))
        val parserHex = parser("S = %x41", options).grammar.getProduction(sym("S"))
        Assertions.assertEquals(parserBin, parserDec)
        Assertions.assertEquals(parserBin, parserHex)
    }

    @Test
    fun unicodeCodepointBinaryDecimalHexEquivalenceRangeSingle() {
        val parserBin = parser("S = %b1000001-1000001", options).grammar.getProduction(sym("S"))
        val parserDec = parser("S = %d65-65", options).grammar.getProduction(sym("S"))
        val parserHex = parser("S = %x41-41", options).grammar.getProduction(sym("S"))
        Assertions.assertEquals(parserBin, parserDec)
        Assertions.assertEquals(parserBin, parserHex)
    }

    @Test
    fun unicodeCodepointRulePrecedence() {
        val text = "🎁🎁🎁"
        val tree = PT.create("S", "🎁", "🎁", "🎁")
        Assertions.assertEquals(
            tree,
            parser("S = %x1F381-1F381 %x1F381-1F381 %x1F381-1F381", options).parse(text)
        )
        Assertions.assertEquals(
            tree,
            parser("S = %x1F381 %x1F381 %x1F381", options).parse(text)
        )
        Assertions.assertEquals(tree, parser("S = %x1F381+", options).parse(text))
        Assertions.assertEquals(tree, parser("S = %x1F381-1F381+", options).parse(text))
        Assertions.assertEquals(tree, parser("S = %x1F381*", options).parse(text))
        Assertions.assertEquals(tree, parser("S = %x1F381-1F381*", options).parse(text))
        Assertions.assertEquals(tree, parser("S = %x1F381 {1,3}", options).parse(text))
        Assertions.assertEquals(tree, parser("S = %x1F381-1F381 {1,3}", options).parse(text))
        Assertions.assertEquals(tree, parser("S = %x1F381 {,3}", options).parse(text))
        Assertions.assertEquals(tree, parser("S = %x1F381-1F381 {,3}", options).parse(text))
        Assertions.assertEquals(tree, parser("S = %x1F381 {1,}", options).parse(text))
        Assertions.assertEquals(tree, parser("S = %x1F381-1F381 {1,}", options).parse(text))
    }
}
