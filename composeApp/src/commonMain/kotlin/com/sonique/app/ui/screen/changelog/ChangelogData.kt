package com.sonique.app.ui.screen.changelog

data class ChangelogRelease(
    val version: String,
    val releaseDate: String,
    val title: String = "What's New",
    val body: String = "",
    val highlights: List<String> = emptyList(),
    val htmlUrl: String? = null,
    val isCurrentVersion: Boolean = false,
)

fun formatReleaseDate(isoDate: String): String {
    if (isoDate.length < 10) return isoDate
    val datePart = isoDate.substring(0, 10)
    val parts = datePart.split("-")
    if (parts.size != 3) return datePart
    val year = parts[0]
    val month = when (parts[1].toIntOrNull()) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> return datePart
    }
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    return "$month $day, $year"
}

fun cleanMarkdownBody(rawBody: String, version: String): String {
    if (rawBody.isBlank()) return ""
    val lines = rawBody.lines()
    val cleanedLines = mutableListOf<String>()
    var skippingIntro = true

    for (rawLine in lines) {
        val line = rawLine.trim()
        // Skip duplicate title headers at the top (e.g. "# Sonique v4.1.0" or "## v4.1.0")
        if (skippingIntro && (line.startsWith("# ") || line.startsWith("## "))) {
            if (line.contains(version, ignoreCase = true) || line.contains("What's Changed", ignoreCase = true)) {
                continue
            }
        }
        skippingIntro = false

        // Skip standalone decorative horizontal rules
        if (line == "---" || line == "===" || line == "***") {
            continue
        }
        cleanedLines.add(rawLine)
    }

    return cleanedLines.joinToString("\n").trim()
}

fun parseReleaseNotes(
    version: String,
    publishedAt: String,
    name: String,
    body: String,
    htmlUrl: String?,
    installedVersion: String,
): ChangelogRelease {
    val cleanVersion = version.removePrefix("v").trim()
    val isCurrent = cleanVersion.isNotEmpty() && cleanVersion.equals(installedVersion.removePrefix("v").trim(), ignoreCase = true)
    val formattedDate = formatReleaseDate(publishedAt)
    val title = if (name.isNotBlank() && !name.equals(version, ignoreCase = true)) {
        name.replace(Regex("^#+\\s*"), "").trim()
    } else {
        "What's New in $version"
    }

    val cleanedBody = cleanMarkdownBody(body, version)

    // Generate fallback highlight bullets
    val lines = cleanedBody.lines().map { it.trim() }
    val bulletHighlights = mutableListOf<String>()

    for (line in lines) {
        if (line.isBlank() || line.startsWith("---") || line.startsWith("===") || line.startsWith("#")) continue
        if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")) {
            val content = line.substring(2).trim()
            if (content.isNotBlank()) {
                bulletHighlights.add(content)
            }
        }
    }

    if (bulletHighlights.isEmpty()) {
        val paragraphLines = lines.filter { line ->
            line.isNotBlank() &&
                !line.startsWith("#") &&
                !line.startsWith("---") &&
                !line.startsWith("===")
        }
        if (paragraphLines.isNotEmpty()) {
            bulletHighlights.addAll(paragraphLines)
        } else if (cleanedBody.isNotBlank()) {
            bulletHighlights.add(cleanedBody.trim())
        } else {
            bulletHighlights.add("Maintenance release with performance improvements and bug fixes.")
        }
    }

    val finalBody = if (cleanedBody.isNotBlank()) {
        cleanedBody
    } else {
        bulletHighlights.joinToString("\n") { "- $it" }
    }

    return ChangelogRelease(
        version = if (version.startsWith("v")) version else "v$version",
        releaseDate = formattedDate,
        title = title,
        body = finalBody,
        highlights = bulletHighlights,
        htmlUrl = htmlUrl,
        isCurrentVersion = isCurrent,
    )
}

object ChangelogData {
    val currentRelease = ChangelogRelease(
        version = "v4.1.0",
        releaseDate = "Oct 1, 2026",
        title = "Sonique v4.1.0 — Dynamic Palette & Smoother Queues",
        body = """
            ### 🚀 Highlights
            - **Adaptive Palette Theming**: Player dynamic color theming based on current album artwork.
            - **Fluid Queues**: Live smooth drag & drop queue reordering with fluid spring physics.
            - **Track Options**: Fresh track options menu featuring quick actions and artwork preview.

            ### ⚡ Performance & Polish
            - **Frosted Liquid Glass**: Refined liquid glass back navigation and geometry.
            - **Snappier UI**: Performance hardening and zero-jitter background prefetching across Library and Search.
            - **Expressive Motion**: Material expressive loading animations throughout the playback pipeline.
        """.trimIndent(),
        highlights = listOf(
            "Adaptive player dynamic color theming based on current album artwork.",
            "Live smooth drag & drop queue reordering with fluid spring physics.",
            "Fresh track options menu featuring quick actions and artwork preview.",
            "Frosted liquid glass back navigation and refined top bar geometry.",
            "Snappier UI transitions and performance hardening across Library and Search.",
            "Material expressive loading animations and zero-jitter background prefetching."
        ),
        htmlUrl = "https://github.com/07-Ansh/Sonique/releases/tag/v4.1.0",
        isCurrentVersion = true,
    )

    val fallbackReleases = listOf(
        currentRelease,
        ChangelogRelease(
            version = "v4.0.0",
            releaseDate = "Sep 19, 2026",
            title = "Sonique v4.0.0 — Major Architecture & Synced Lyrics Refresh",
            body = """
                ### 🎨 Major Refresh
                - **Material 3 Expressive**: Modern UI refresh across all primary tabs.
                - **High-Res Pipeline**: High-resolution album artwork pipeline and enhanced audio caching.

                ### 🎵 Player Enhancements
                - **Synced Lyrics**: Revamped player lyrics view with sync support and Romanization.
                - **Playback Controls**: Customizable playback speeds and sleep timer integration.
            """.trimIndent(),
            highlights = listOf(
                "Modern Material 3 Expressive UI refresh across all primary tabs.",
                "High-resolution album artwork pipeline and enhanced audio caching.",
                "Revamped player lyrics view with sync support and Romanization.",
                "Customizable playback speeds and sleep timer integration."
            ),
            htmlUrl = "https://github.com/07-Ansh/Sonique/releases/tag/v4.0.0",
        )
    )
}
