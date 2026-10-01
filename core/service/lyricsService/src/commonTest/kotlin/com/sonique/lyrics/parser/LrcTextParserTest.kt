package com.sonique.lyrics.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LrcTextParserTest {

    @Test
    fun testParseSyncedLyrics_standardTwoDigitCentiseconds() {
        val lrc = """
            [00:01.00] Line one
            [01:05.50] Line two
        """.trimIndent()

        val result = parseSyncedLyrics(lrc)
        val lines = result.lyrics?.lines
        assertNotNull(lines)
        assertEquals(2, lines.size)

        assertEquals("1000", lines[0].startTimeMs)
        assertEquals("Line one", lines[0].words)

        // 1 min (60000) + 5 sec (5000) + 50 * 10 (500) = 65500 ms
        assertEquals("65500", lines[1].startTimeMs)
        assertEquals("Line two", lines[1].words)
        assertEquals("LINE_SYNCED", result.lyrics?.syncType)
    }

    @Test
    fun testParseSyncedLyrics_threeDigitMilliseconds() {
        val lrc = "[00:12.345] High precision line"
        val result = parseSyncedLyrics(lrc)
        val lines = result.lyrics?.lines
        assertNotNull(lines)
        assertEquals(1, lines.size)

        // 12000 + 345 = 12345 ms
        assertEquals("12345", lines[0].startTimeMs)
        assertEquals("High precision line", lines[0].words)
    }

    @Test
    fun testParseSyncedLyrics_blankLineReplacedWithMusicNote() {
        val lrc = "[00:10.00]    "
        val result = parseSyncedLyrics(lrc)
        val lines = result.lyrics?.lines
        assertNotNull(lines)
        assertEquals(1, lines.size)
        assertEquals("♫", lines[0].words)
    }

    @Test
    fun testParseSyncedLyrics_ignoresMetadataAndMalformedLines() {
        val lrc = """
            [ti:Song Title]
            [ar:Artist Name]
            [al:Album Name]
            Not an LRC timestamp line
            [00:02.00] Valid lyric line
        """.trimIndent()

        val result = parseSyncedLyrics(lrc)
        val lines = result.lyrics?.lines
        assertNotNull(lines)
        assertEquals(1, lines.size)
        assertEquals("2000", lines[0].startTimeMs)
        assertEquals("Valid lyric line", lines[0].words)
    }

    @Test
    fun testParseRichSyncLyrics_basicParsing() {
        val raw = "[00:01.00] <00:01.00>Word1 <00:01.50>Word2"
        val result = parseRichSyncLyrics(raw)
        val lines = result.lyrics?.lines
        assertNotNull(lines)
        assertTrue(lines.isNotEmpty())
    }
}
