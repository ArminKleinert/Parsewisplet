package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.Parsewisp
import de.kleinert.parsewisp.Sym
import de.kleinert.parsewisp.parser_options.ParserCreationOptions
import de.kleinert.parsewisp.testutil.PT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class CharRangeTest {
    private val options = ParserCreationOptions.default

    @Test
    fun unicodeCodepointSingleParse() {
        val parser = Parsewisp.parser("S = %x1F381", options)
        Assertions.assertTrue(parser.parse("\uD83C\uDF80").isFailure)
        Assertions.assertEquals(PT.create("S", "🎁"), parser.parse("🎁"))
        Assertions.assertTrue(parser.parse("\uD83C\uDF82").isFailure)
        Assertions.assertTrue(parser.parse("\uD83C\uDF83").isFailure)
    }

    @Test
    fun unicodeCodepointSingleParse2() {
        val parser = Parsewisp.parser("S = %x1F381-1F381", options)
        Assertions.assertTrue(parser.parse("\uD83C\uDF80").isFailure)
        Assertions.assertEquals(PT.create("S", "🎁"), parser.parse("🎁"))
        Assertions.assertTrue(parser.parse("\uD83C\uDF82").isFailure)
        Assertions.assertTrue(parser.parse("\uD83C\uDF83").isFailure)
    }

    @Test
    fun unicodeCodepointShortRangeParse() {
        val parser = Parsewisp.parser("S = %x1F381-1F382", options)
        Assertions.assertTrue(parser.parse("\uD83C\uDF80").isFailure)
        Assertions.assertEquals(PT.create("S", "🎁"), parser.parse("🎁"))
        Assertions.assertEquals(PT.create("S", "\uD83C\uDF82"), parser.parse("\uD83C\uDF82"))
        Assertions.assertTrue(parser.parse("\uD83C\uDF83").isFailure)
    }

    @Test
    fun unicodeCodepointLongRangeParse() {
        val parser = Parsewisp.parser("S = %x41-1F382", options)
        Assertions.assertTrue(parser.parse("\uD83C\uDF83").isFailure)
        Assertions.assertEquals(PT.create("S", "A"), parser.parse("A"))
        Assertions.assertEquals(PT.create("S", "🎁"), parser.parse("🎁"))
        Assertions.assertEquals(PT.create("S", "\uD83C\uDF82"), parser.parse("\uD83C\uDF82"))
    }

    @Test
    fun unicodeCodepointSmallCharParse() {
        val parser = Parsewisp.parser("S = %x41-5A", options)
        Assertions.assertTrue(parser.parse("\uD83C\uDF80").isFailure)
        Assertions.assertEquals(PT.create("S", "A"), parser.parse("A"))
        Assertions.assertEquals(PT.create("S", "Z"), parser.parse("Z"))
        Assertions.assertTrue(parser.parse("\uD83C\uDF81").isFailure)
    }

    @Test
    fun unicodeCodepointBinaryDecimalHexEquivalenceRange() {
        val parserBin = Parsewisp.parser("S = %b1000001-1011010", options).grammar.getProduction(Sym.sym("S"))
        val parserDec = Parsewisp.parser("S = %d65-90", options).grammar.getProduction(Sym.sym("S"))
        val parserHex = Parsewisp.parser("S = %x41-5A", options).grammar.getProduction(Sym.sym("S"))
        Assertions.assertEquals(parserBin, parserDec)
        Assertions.assertEquals(parserBin, parserHex)
    }

    @Test
    fun unicodeCodepointBinaryDecimalHexEquivalenceSingle() {
        val parserBin = Parsewisp.parser("S = %b1000001", options).grammar.getProduction(Sym.sym("S"))
        val parserDec = Parsewisp.parser("S = %d65", options).grammar.getProduction(Sym.sym("S"))
        val parserHex = Parsewisp.parser("S = %x41", options).grammar.getProduction(Sym.sym("S"))
        Assertions.assertEquals(parserBin, parserDec)
        Assertions.assertEquals(parserBin, parserHex)
    }

    @Test
    fun unicodeCodepointBinaryDecimalHexEquivalenceRangeSingle() {
        val parserBin = Parsewisp.parser("S = %b1000001-1000001", options).grammar.getProduction(Sym.sym("S"))
        val parserDec = Parsewisp.parser("S = %d65-65", options).grammar.getProduction(Sym.sym("S"))
        val parserHex = Parsewisp.parser("S = %x41-41", options).grammar.getProduction(Sym.sym("S"))
        Assertions.assertEquals(parserBin, parserDec)
        Assertions.assertEquals(parserBin, parserHex)
    }

    @Test
    fun unicodeCodepointRulePrecedence() {
        val text = "🎁🎁🎁"
        val tree = PT.create("S", "🎁", "🎁", "🎁")
        Assertions.assertEquals(
            tree,
            Parsewisp.parser("S = %x1F381-1F381 %x1F381-1F381 %x1F381-1F381", options).parse(text)
        )
        Assertions.assertEquals(
            tree,
            Parsewisp.parser("S = %x1F381 %x1F381 %x1F381", options).parse(text)
        )
        Assertions.assertEquals(tree, Parsewisp.parser("S = %x1F381+", options).parse(text))
        Assertions.assertEquals(tree, Parsewisp.parser("S = %x1F381-1F381+", options).parse(text))
        Assertions.assertEquals(tree, Parsewisp.parser("S = %x1F381*", options).parse(text))
        Assertions.assertEquals(tree, Parsewisp.parser("S = %x1F381-1F381*", options).parse(text))
        Assertions.assertEquals(tree, Parsewisp.parser("S = %x1F381 {1,3}", options).parse(text))
        Assertions.assertEquals(tree, Parsewisp.parser("S = %x1F381-1F381 {1,3}", options).parse(text))
        Assertions.assertEquals(tree, Parsewisp.parser("S = %x1F381 {,3}", options).parse(text))
        Assertions.assertEquals(tree, Parsewisp.parser("S = %x1F381-1F381 {,3}", options).parse(text))
        Assertions.assertEquals(tree, Parsewisp.parser("S = %x1F381 {1,}", options).parse(text))
        Assertions.assertEquals(tree, Parsewisp.parser("S = %x1F381-1F381 {1,}", options).parse(text))
    }
}
