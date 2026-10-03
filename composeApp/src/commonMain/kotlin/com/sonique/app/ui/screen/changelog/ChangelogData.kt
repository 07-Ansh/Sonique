package com.sonique.app.ui.screen.changelog

data class ChangelogRelease(
    val version: String,
    val releaseDate: String,
    val title: String = "What's New",
    val highlights: List<String>,
)

object ChangelogData {
    val currentRelease = ChangelogRelease(
        version = "4.1.0",
        releaseDate = "2026-10-03",
        title = "What's New",
        highlights = listOf(
            "Adaptive player dynamic color theming based on current album artwork.",
            "Live smooth drag & drop queue reordering with fluid spring physics.",
            "Fresh track options menu featuring quick actions and artwork preview.",
            "Frosted liquid glass back navigation and refined top bar geometry.",
            "Snappier UI transitions and performance hardening across Library and Search.",
            "Material expressive loading animations and zero-jitter background prefetching."
        )
    )

    val previousReleases = listOf(
        ChangelogRelease(
            version = "4.0.0",
            releaseDate = "2026-09-15",
            title = "What's New",
            highlights = listOf(
                "Modern Material 3 Expressive UI refresh across all primary tabs.",
                "High-resolution album artwork pipeline and enhanced audio caching.",
                "Revamped player lyrics view with sync support and Romanization.",
                "Customizable playback speeds and sleep timer integration."
            )
        )
    )
}
