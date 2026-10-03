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
        .replace(Regex("(?i)\\s*[\\[\\(](?:official|music|video|audio|lyric|lyrics|hd|4k|visualizer|visualiser|live|performance|remix|version|video version|full song|from [^\\]\\)]*)[^\\]\\)]*[\\]\\)]"), "")
        .replace(Regex("(?i)\\s*\\|\\s*(?:official|music|video|audio|lyrics?|hd|4k|visualizer|visualiser|live|exclusive).*$"), "")
        .replace(Regex("(?i)\\s*[-–—]\\s*(?:official|music|video|audio|lyrics?|visualizer|visualiser|live|remix).*$"), "")
        .trim()
    return if (cleaned.isNotBlank()) cleaned else title.trim()
}

/**
 * Extracts the song title and artist from a video title.
 * Most YouTube music videos follow "Artist - Title", "Artist : Title", or "Artist | Title".
 * If no separator is present, returns the cleaned title and sanitized fallback artist.
 */
fun extractSongTitleAndArtist(videoTitle: String, fallbackArtist: String?): Pair<String, String> {
    val cleaned = cleanVideoTitle(videoTitle)
    val parts = cleaned.split(Regex("\\s+[-–—:]\\s+"), limit = 2)
    return if (parts.size == 2) {
        val artistPart = parts[0].trim()
        val titlePart = parts[1].trim()
        Pair(titlePart, artistPart)
    } else {
        val cleanFallback = fallbackArtist
            ?.replace(Regex("(?i)\\s*(?:vevo|official|records?|music|channel|topic)\\s*"), "")
            ?.trim() ?: ""
        Pair(cleaned, cleanFallback)
    }
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

private fun normalizeString(text: String): String {
    return text.lowercase()
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
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
    val (extractedTitle, extractedArtist) = extractSongTitleAndArtist(videoTitle, videoArtist)
    val normCandTitle = normalizeString(candidateTitle)
    val normExtTitle = normalizeString(extractedTitle)
    val normFullVidTitle = normalizeString(cleanVideoTitle(videoTitle))
    val normExtArtist = normalizeString(extractedArtist)
    val normFallbackArtist = normalizeString(videoArtist ?: "")

    if (normCandTitle.isEmpty()) return false

    // Check title match
    val titleMatches = normCandTitle == normExtTitle ||
        (normCandTitle.length >= 3 && normFullVidTitle.contains(normCandTitle)) ||
        normFullVidTitle.split(" ").contains(normCandTitle) ||
        calculateTitleSimilarity(extractedTitle, candidateTitle) >= 0.5f ||
        calculateTitleSimilarity(videoTitle, candidateTitle) >= 0.5f

    if (!titleMatches) return false

    // Check artist match if artists are available
    val hasCandidateArtists = !candidateArtists.isNullOrEmpty()
    val hasAnyVideoArtist = normExtArtist.isNotEmpty() || normFallbackArtist.isNotEmpty()

    if (hasCandidateArtists && hasAnyVideoArtist) {
        val cleanExtArtistAlpha = normExtArtist.replace(" ", "")
        val cleanFallbackAlpha = normFallbackArtist.replace(" ", "")

        val artistMatches = candidateArtists.any { candidateArtist ->
            val normCandArtist = normalizeString(candidateArtist)
            val cleanCandArtistAlpha = normCandArtist.replace(" ", "")
            if (cleanCandArtistAlpha.isEmpty()) return@any false

            // Match against extracted artist (e.g. "Alan Walker" vs "Alan Walker")
            normExtArtist == normCandArtist ||
                (cleanExtArtistAlpha.isNotEmpty() && (
                    cleanExtArtistAlpha.contains(cleanCandArtistAlpha) ||
                    cleanCandArtistAlpha.contains(cleanExtArtistAlpha)
                )) ||
                // Match against fallback/channel name (e.g. "Alan Walker" in "AlanWalkerVEVO")
                (cleanFallbackAlpha.isNotEmpty() && (
                    cleanFallbackAlpha.contains(cleanCandArtistAlpha) ||
                    cleanCandArtistAlpha.contains(cleanFallbackAlpha)
                )) ||
                // Candidate artist mentioned in full video title
                normFullVidTitle.contains(normCandArtist)
        }

        if (!artistMatches) {
            return false
        }
    }

    return true
}
