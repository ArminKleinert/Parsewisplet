package de.kleinert.parsewisp.tests.typical

import de.kleinert.parsewisp.util.StrParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class StrParserTest {
    private val strParser = StrParser()

    @Test
    fun testStr1() {
        val s = strParser.processString("\"a\"")
        Assertions.assertEquals(1, s.length)
        Assertions.assertEquals("a", s)
    }

    @Test
    fun testStr2() {
        val s = strParser.processString("'a'")
        Assertions.assertEquals(1, s.length)
        Assertions.assertEquals("a", s)
    }

    @Test
    fun testStr3() {
        val s = strParser.processString("'\\u1234'")
        Assertions.assertEquals("ሴ", s)
    }

    @Test
    fun testReg1() {
        val s = strParser.processString("#\"[a]\"")
        Assertions.assertEquals(4, s.length)
        Assertions.assertEquals("\"[a]", s)
    }

    @Test
    fun testReg2() {
        val s = strParser.processString("#'[a]'")
        //Assertions.assertEquals(4, s.length());
        Assertions.assertEquals("'[a]", s)
    }
}
