package com.sonique.app.extension

import kotlin.test.Test
import kotlin.test.assertEquals

class SongTitleCleanerTest {

    @Test
    fun testCleanSongTitle_stripsParentheses() {
        val raw = "Blinding Lights (Official Audio)"
        assertEquals("Blinding Lights", raw.cleanSongTitle())
    }

    @Test
    fun testCleanSongTitle_stripsBrackets() {
        val raw = "Starboy [Remix]"
        assertEquals("Starboy", raw.cleanSongTitle())
    }

    @Test
    fun testCleanSongTitle_stripsDashSuffixes() {
        val raw = "Hotel California - Live at the Forum"
        assertEquals("Hotel California", raw.cleanSongTitle())
    }

    @Test
    fun testCleanSongTitle_stripsEmDashSuffixes() {
        val raw = "Shape of You — Official Music Video"
        assertEquals("Shape of You", raw.cleanSongTitle())
    }

    @Test
    fun testCleanSongTitle_stripsPipeSuffixes() {
        val raw = "Midnight City | 4K Remaster"
        assertEquals("Midnight City", raw.cleanSongTitle())
    }

    @Test
    fun testCleanSongTitle_stripsSlashSuffixes() {
        val raw = "Stay / Official Visualizer"
        assertEquals("Stay", raw.cleanSongTitle())
    }

    @Test
    fun testCleanSongTitle_preservesCleanTitles() {
        val raw = "Save Your Tears"
        assertEquals("Save Your Tears", raw.cleanSongTitle())
    }

    @Test
    fun testCleanSongTitle_handlesEmptyOrBlank() {
        assertEquals("", "".cleanSongTitle())
        assertEquals("   ", "   ".cleanSongTitle())
    }
}
