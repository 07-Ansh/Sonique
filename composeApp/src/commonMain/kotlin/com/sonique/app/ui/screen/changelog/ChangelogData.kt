package com.sonique.app.ui.screen.changelog

data class ChangelogRelease(
    val version: String,
    val releaseDate: String,
    val title: String = "What's New",
    val highlights: List<String>,
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

    val lines = body.lines().map { it.trim() }
    val bulletHighlights = mutableListOf<String>()

    for (line in lines) {
        if (line.isBlank() || line.startsWith("---") || line.startsWith("===")) continue
        if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")) {
            val content = line.substring(2).trim()
            if (content.isNotBlank()) {
                val cleaned = content
                    .replace(Regex("^\\*\\*|\\*\\*$"), "")
                    .trim()
                if (cleaned.isNotBlank()) {
                    bulletHighlights.add(cleaned)
                }
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
        } else if (body.isNotBlank()) {
            bulletHighlights.add(body.trim())
        } else {
            bulletHighlights.add("Maintenance release with performance improvements and bug fixes.")
        }
    }

    return ChangelogRelease(
        version = if (version.startsWith("v")) version else "v$version",
        releaseDate = formattedDate,
        title = title,
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

