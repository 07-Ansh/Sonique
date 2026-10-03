package com.sonique.domain.utils

private val GOOGLE_USER_CONTENT_SIZE_REGEX = Regex("=w\\d+-h\\d+[^\"'\\s]*|=s\\d+[^\"'\\s]*|/s\\d+-[^/]+/")
private val YT_VIDEO_THUMBNAIL_REGEX = Regex("/(default|mqdefault|hqdefault|sddefault)\\.jpg")

/**
 * Upgrades image URLs from Google User Content (lh3.googleusercontent.com, yt3.ggpht.com)
 * and YouTube video thumbnails (i.ytimg.com) to high-resolution formats.
 *
 * For Google User Content, replaces size parameters with `=w1200-h1200-l90-rj` (or equivalent).
 * For YouTube thumbnails, replaces standard low-res names with `maxresdefault.jpg`.
 */
fun String.toHighResThumbnailUrl(targetSize: Int = 544): String {
    if (isBlank()) return this
    return when {
        contains("googleusercontent.com") || contains("ggpht.com") -> {
            when {
                contains("=w") || contains("=s") ->
                    GOOGLE_USER_CONTENT_SIZE_REGEX.replace(this, "=w$targetSize-h$targetSize-l90-rj")
                contains("/s") ->
                    replace(Regex("/s\\d+/"), "/s$targetSize/")
                else -> "$this=w$targetSize-h$targetSize-l90-rj"
            }
        }
        contains("i.ytimg.com") -> {
            if (targetSize >= 1000) {
                replace(YT_VIDEO_THUMBNAIL_REGEX, "/maxresdefault.jpg")
            } else {
                replace(YT_VIDEO_THUMBNAIL_REGEX, "/hqdefault.jpg")
            }
        }
        else -> this
    }
}

/**
 * Checks if a thumbnail URL represents a 16:9 YouTube video thumbnail rather than square album art.
 */
fun isVideoThumbnailUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return url.contains("i.ytimg.com") || url.contains("hq720") || url.contains("maxresdefault") || url.contains("sddefault")
}

/**
 * Cleans video titles by stripping common music video tags (e.g. "[Official Video]", "(Music Video)", etc.)
 * to allow accurate song-matching searches on YouTube Music.
 */
fun cleanVideoTitle(title: String): String {
    if (title.isBlank()) return title
    val cleaned = title
        .replace(Regex("(?i)\\s*[\\[\\(](?:official|music|video|audio|lyric|lyrics|hd|4k|visualizer|live|performance|remix|version|video version)[^\\]\\)]*[\\]\\)]"), "")
        .replace(Regex("(?i)\\s*\\|.*$"), "")
        .replace(Regex("(?i)\\s*[-–—]\\s*(?:official|music|video|audio|lyrics?|visualizer|live|remix).*$"), "")
        .trim()
    return if (cleaned.isNotBlank()) cleaned else title.trim()
}

/**
 * Calculates a normalized similarity score (0.0 to 1.0) between two song titles.
 */
fun calculateTitleSimilarity(title1: String, title2: String): Float {
    val clean1 = cleanVideoTitle(title1).lowercase().replace(Regex("[^a-z0-9\\s]"), " ").replace(Regex("\\s+"), " ").trim()
    val clean2 = cleanVideoTitle(title2).lowercase().replace(Regex("[^a-z0-9\\s]"), " ").replace(Regex("\\s+"), " ").trim()

    if (clean1.isEmpty() || clean2.isEmpty()) return 0f
    if (clean1 == clean2) return 1f

    if (clean1.contains(clean2) || clean2.contains(clean1)) {
        val shorter = minOf(clean1.length, clean2.length).toFloat()
        val longer = maxOf(clean1.length, clean2.length).toFloat()
        val ratio = shorter / longer
        if (ratio >= 0.5f) return ratio
    }

    val words1 = clean1.split(" ").filter { it.isNotBlank() }.toSet()
    val words2 = clean2.split(" ").filter { it.isNotBlank() }.toSet()
    val intersection = words1.intersect(words2).size
    val union = words1.union(words2).size

    return if (union > 0) intersection.toFloat() / union.toFloat() else 0f
}

/**
 * Validates whether a candidate song from search results is a reliable match for a video song.
 */
fun isSongMatch(
    videoTitle: String,
    videoArtist: String?,
    candidateTitle: String,
    candidateArtists: List<String>?,
): Boolean {
    val titleScore = calculateTitleSimilarity(videoTitle, candidateTitle)
    if (titleScore < 0.5f) return false

    if (!videoArtist.isNullOrBlank() && !candidateArtists.isNullOrEmpty()) {
        val cleanVideoArtist = videoArtist.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
        val cleanVidTitle = cleanVideoTitle(videoTitle).lowercase().replace(Regex("[^a-z0-9]"), "").trim()
        val artistMatches = candidateArtists.any { candidateArtist ->
            val cleanCandidate = candidateArtist.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
            cleanCandidate.isNotEmpty() && (
                cleanVideoArtist.contains(cleanCandidate) ||
                cleanCandidate.contains(cleanVideoArtist) ||
                cleanVidTitle.contains(cleanCandidate)
            )
        }
        if (!artistMatches) {
            return false
        }
    }

    return true
}
