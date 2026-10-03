package com.sonique.app.utils

import com.sonique.app.ui.screen.changelog.formatReleaseDate
import com.sonique.app.ui.screen.changelog.parseReleaseNotes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChangelogParserTest {

    @Test
    fun testFormatReleaseDate() {
        assertEquals("Oct 1, 2026", formatReleaseDate("2026-10-01T18:32:15Z"))
        assertEquals("Sep 19, 2026", formatReleaseDate("2026-09-19T14:25:53Z"))
        assertEquals("Jan 5, 2025", formatReleaseDate("2025-01-05"))
        assertEquals("invalid", formatReleaseDate("invalid"))
    }

    @Test
    fun testParseReleaseNotesWithBullets() {
        val markdown = """
            # 🎵 Sonique v4.1.0 — Dynamic Palette & Smoother Queues
            Welcome to Sonique!
            ---
            ### ✨ What's New
            - **Adaptive Color Theming**: Player controls adapt to artwork.
            - **Smooth Queues**: Fluid reordering physics.
        """.trimIndent()

        val parsed = parseReleaseNotes(
            version = "v4.1.0",
            publishedAt = "2026-10-01T18:32:15Z",
            name = "Sonique v4.1.0 — Dynamic Palette",
            body = markdown,
            htmlUrl = "https://github.com/07-Ansh/Sonique/releases/tag/v4.1.0",
            installedVersion = "4.1.0"
        )

        assertEquals("v4.1.0", parsed.version)
        assertEquals("Oct 1, 2026", parsed.releaseDate)
        assertEquals("Sonique v4.1.0 — Dynamic Palette", parsed.title)
        assertTrue(parsed.isCurrentVersion)
        assertTrue(parsed.body.contains("Adaptive Color Theming"))
        assertTrue(parsed.body.contains("Smooth Queues"))
        assertEquals(2, parsed.highlights.size)
        assertEquals("Adaptive Color Theming**: Player controls adapt to artwork.", parsed.highlights[0])
        assertEquals("Smooth Queues**: Fluid reordering physics.", parsed.highlights[1])
    }

    @Test
    fun testParseReleaseNotesWithoutBulletsFallback() {
        val markdown = """
            ## Minor Fixes & Performance Improvements
            This release includes minor fixes and performance improvements to make the app more stable.
        """.trimIndent()

        val parsed = parseReleaseNotes(
            version = "3.0.1",
            publishedAt = "2026-08-15T06:28:20Z",
            name = "",
            body = markdown,
            htmlUrl = null,
            installedVersion = "4.1.0"
        )

        assertEquals("v3.0.1", parsed.version)
        assertEquals("Aug 15, 2026", parsed.releaseDate)
        assertEquals(false, parsed.isCurrentVersion)
        assertEquals(1, parsed.highlights.size)
        assertEquals(
            "This release includes minor fixes and performance improvements to make the app more stable.",
            parsed.highlights[0]
        )
    }
}
