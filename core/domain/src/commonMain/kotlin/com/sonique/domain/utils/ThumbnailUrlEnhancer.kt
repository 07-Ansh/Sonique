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
fun String.toHighResThumbnailUrl(targetSize: Int = 1200): String {
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
            replace(YT_VIDEO_THUMBNAIL_REGEX, "/maxresdefault.jpg")
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
