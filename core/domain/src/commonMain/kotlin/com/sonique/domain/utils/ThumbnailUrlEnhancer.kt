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
 * Known record labels, publishers, or generic video distribution channels.
 * When the video artist is one of these, it represents the channel/distributor, not the music performer.
 */
fun isRecordLabelOrDistributor(artistName: String?): Boolean {
    if (artistName.isNullOrBlank()) return false
    val clean = artistName.lowercase().trim()
    val labelKeywords = listOf(
        "t-series", "tseries", "series", "zee music", "zeemusic", "sony music", "sonymusic",
        "yrf", "yash raj", "saregama", "tips official", "tips music", "tips bhojpuri",
        "speed records", "white hill", "geet mp3", "universal music", "warner music",
        "warner records", "atlantic records", "columbia records", "def jam", "spinnin",
        "ultra records", "monstercat", "vevo", "records", "recordings", "entertainment",
        "films", "studios", "music company", "official channel", "topic", "channel",
        "bhakti", "desire music", "dm music", "jjust music", "svf", "bongo",
        "times music", "junglee music", "venus", "eros now", "pen movies"
    )
    return labelKeywords.any { clean.contains(it) }
}

/**
 * Parsed components of a video song title for high-accuracy audio counterpart discovery.
 */
data class ParsedVideoSong(
    val primaryTitle: String,
    val alternateTitle: String? = null,
    val artistOrContext: String? = null,
    val allSegments: List<String> = emptyList(),
    val searchQueries: List<String> = emptyList(),
    val isLabelChannel: Boolean = false,
)

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
 * Extracts structured metadata from a video title and fallback uploader channel.
 * Parses pipe segments ("|") and delimiter dashes ("-", ":") to accurately identify
 * the core song title, album/movie context, and performer.
 */
fun parseVideoSongMetadata(videoTitle: String, videoArtist: String?): ParsedVideoSong {
    val isLabel = isRecordLabelOrDistributor(videoArtist) || videoArtist.isNullOrBlank()

    // 1. Split on pipes "|" to isolate segments (e.g. "Song | Movie | Singers")
    val rawSegments = videoTitle.split(Regex("\\s*\\|\\s*"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val segment0 = rawSegments.firstOrNull() ?: videoTitle
    val otherSegments = if (rawSegments.size > 1) rawSegments.drop(1) else emptyList()

    // Filter out video descriptor tags from secondary context segments
    val meaningfulContext = otherSegments.filterNot { seg ->
        val lower = seg.lowercase()
        lower.contains("official") || lower.contains("video") || lower.contains("visualizer") ||
            lower.contains("visualiser") || lower.contains("lyrics") || lower.contains("audio") ||
            lower.contains("4k") || lower.contains("hd") || lower.contains("teaser") ||
            lower.contains("trailer") || isRecordLabelOrDistributor(seg)
    }

    val cleanSeg0 = cleanVideoTitle(segment0)

    var primaryTitle: String
    var alternateTitle: String? = null
    var artistPart: String? = null

    // 2. Check for dash / colon separators in segment 0 (e.g. "Artist - Title" or "Title - Movie")
    val dashParts = cleanSeg0.split(Regex("\\s+[-–—:]\\s+"), limit = 2)
    if (dashParts.size == 2) {
        val part0 = cleanVideoTitle(dashParts[0])
        val part1 = cleanVideoTitle(dashParts[1])

        val cleanChannel = videoArtist
            ?.replace(Regex("(?i)\\s*(?:vevo|official|records?|music|channel|topic)\\s*"), "")
            ?.trim() ?: ""

        val normChannel = normalizeString(cleanChannel).replace(" ", "")
        val normPart0 = normalizeString(part0).replace(" ", "")
        val normPart1 = normalizeString(part1).replace(" ", "")

        when {
            // Part 1 matches artist/channel (e.g. "Flowers - Miley Cyrus")
            normChannel.isNotEmpty() && normPart1.isNotEmpty() && (normChannel.contains(normPart1) || normPart1.contains(normChannel)) -> {
                primaryTitle = part0
                alternateTitle = part1
                artistPart = part1
            }
            // Multi-segment label video (Bollywood format: "Kesariya - Brahmāstra | Ranbir Kapoor | ...")
            rawSegments.size > 1 && isLabel -> {
                primaryTitle = part0
                alternateTitle = part1
                artistPart = part0
            }
            // Standard "Artist - Title" format (e.g. "Twenty One Pilots - Stressed Out", "Alan Walker - Faded")
            else -> {
                primaryTitle = part1
                alternateTitle = part0
                artistPart = part0
            }
        }
    } else {
        primaryTitle = cleanSeg0
        artistPart = if (!isLabel) {
            videoArtist
                ?.replace(Regex("(?i)\\s*(?:vevo|official|records?|music|channel|topic)\\s*"), "")
                ?.trim()
        } else {
            null
        }
    }

    // Strip feat / ft from primary title if present to get pure song name
    val coreCleanTitle = primaryTitle
        .replace(Regex("(?i)\\s*[\\[\\(]?(?:feat\\.?|ft\\.?|featuring)\\s+[^\\]\\)]*[\\]\\)]?"), "")
        .trim()
        .ifEmpty { primaryTitle }

    // 3. Build prioritized, clean search queries for YouTube Music
    val queries = mutableListOf<String>()

    // Priority 1: Primary title + known singer / artist
    if (!artistPart.isNullOrBlank() && !coreCleanTitle.contains(artistPart, ignoreCase = true)) {
        queries.add("$coreCleanTitle $artistPart")
    }

    // Priority 2: Primary title + alternate title (e.g. "Kesariya Brahmāstra" or "Twenty One Pilots Stressed Out")
    if (alternateTitle != null && !coreCleanTitle.contains(alternateTitle, ignoreCase = true)) {
        queries.add("$coreCleanTitle $alternateTitle")
    }

    // Priority 3: Primary title + first meaningful context segment (e.g. "Tum Ho Toh Saiyaara" or "Vaaroon Forever Anand Bhaskar")
    meaningfulContext.firstOrNull()?.let { ctx ->
        val cleanCtx = ctx.replace(Regex("(?i)\\s*(?:from|movie|starring|ft\\.?|feat\\.?).*$"), "").trim()
        if (cleanCtx.isNotBlank() && !coreCleanTitle.contains(cleanCtx, ignoreCase = true)) {
            val shortCtx = cleanCtx.split(",").firstOrNull()?.trim() ?: cleanCtx
            queries.add("$coreCleanTitle $shortCtx")
        }
    }

    // Priority 4: Fallback to pure core song title
    queries.add(coreCleanTitle)

    return ParsedVideoSong(
        primaryTitle = coreCleanTitle,
        alternateTitle = alternateTitle,
        artistOrContext = artistPart ?: meaningfulContext.firstOrNull(),
        allSegments = rawSegments,
        searchQueries = queries.distinct().filter { it.isNotBlank() },
        isLabelChannel = isLabel,
    )
}

/**
 * Extracts the song title and artist from a video title.
 * Backwards-compatible wrapper delegating to parseVideoSongMetadata.
 */
fun extractSongTitleAndArtist(videoTitle: String, fallbackArtist: String?): Pair<String, String> {
    val parsed = parseVideoSongMetadata(videoTitle, fallbackArtist)
    val artist = parsed.artistOrContext ?: fallbackArtist
        ?.replace(Regex("(?i)\\s*(?:vevo|official|records?|music|channel|topic)\\s*"), "")
        ?.trim() ?: ""
    return Pair(parsed.primaryTitle, artist)
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
 * Prevents picking wrong songs while ensuring official releases from record labels
 * (T-Series, Sony Music, Zee, etc.) are matched reliably.
 */
fun isSongMatch(
    videoTitle: String,
    videoArtist: String?,
    candidateTitle: String,
    candidateArtists: List<String>?,
): Boolean {
    val parsed = parseVideoSongMetadata(videoTitle, videoArtist)
    val normCandTitle = normalizeString(cleanVideoTitle(candidateTitle))
    val normPrimaryTitle = normalizeString(parsed.primaryTitle)
    val normAlternateTitle = parsed.alternateTitle?.let { normalizeString(it) }
    val normFullVidTitle = normalizeString(cleanVideoTitle(videoTitle))
    val candArtistList = candidateArtists?.map { normalizeString(it) } ?: emptyList()

    if (normCandTitle.isEmpty()) return false

    // 1. Title matching check
    val isTitleExact = normCandTitle == normPrimaryTitle ||
        (normAlternateTitle != null && normCandTitle == normAlternateTitle)

    val isTitlePrefixOrSuffix = normPrimaryTitle.startsWith(normCandTitle) ||
        normCandTitle.startsWith(normPrimaryTitle) ||
        (normAlternateTitle != null && (normAlternateTitle.startsWith(normCandTitle) || normCandTitle.startsWith(normAlternateTitle)))

    val isTitleInFullVideo = normCandTitle.length >= 3 && (
        normFullVidTitle == normCandTitle ||
        normFullVidTitle.startsWith("$normCandTitle ") ||
        normFullVidTitle.endsWith(" $normCandTitle") ||
        normFullVidTitle.contains(" $normCandTitle ") ||
        normFullVidTitle.contains(normCandTitle)
    )

    val simPrimary = calculateTitleSimilarity(parsed.primaryTitle, candidateTitle)
    val simAlternate = parsed.alternateTitle?.let { calculateTitleSimilarity(it, candidateTitle) } ?: 0f
    val simFull = calculateTitleSimilarity(videoTitle, candidateTitle)
    val maxSim = maxOf(simPrimary, simAlternate, simFull)

    val titleMatches = isTitleExact || isTitlePrefixOrSuffix || (isTitleInFullVideo && normCandTitle.length >= 3) || maxSim >= 0.45f

    if (!titleMatches) return false

    // 2. Artist validation check
    val isLabel = parsed.isLabelChannel

    if (isLabel) {
        // For record label uploads (T-Series, Sony, Zee, etc.):
        // Check if candidate artist appears anywhere in the video title or description segments
        val candArtistInVidTitle = candArtistList.any { artist ->
            artist.length >= 3 && normFullVidTitle.contains(artist)
        }
        val candArtistInSegments = parsed.allSegments.any { seg ->
            val normSeg = normalizeString(seg)
            candArtistList.any { artist -> artist.length >= 3 && normSeg.contains(artist) }
        }

        if (candArtistInVidTitle || candArtistInSegments) {
            return true
        }

        // If candidate artist is not in video title, accept if the song title match is strong
        // (YouTube Music FILTER_SONG returns the official audio for the exact title)
        val strongTitleMatch = isTitleExact || maxSim >= 0.65f || (normCandTitle.length >= 4 && isTitleInFullVideo)
        return strongTitleMatch
    } else {
        // For specific artist uploads (Alan Walker, Adele, Coldplay, etc.):
        val normVidArtist = normalizeString(videoArtist ?: "")
        val cleanVidArtistAlpha = normVidArtist.replace(" ", "").replace("vevo", "").replace("official", "")

        val artistMatchesChannel = candArtistList.any { cand ->
            val cleanCand = cand.replace(" ", "")
            (cleanCand.length >= 3 && (cleanVidArtistAlpha.contains(cleanCand) || cleanCand.contains(cleanVidArtistAlpha))) ||
                cand == normVidArtist
        }

        val artistMatchesVidTitle = candArtistList.any { cand ->
            cand.length >= 3 && normFullVidTitle.contains(cand)
        }

        val artistMatchesAlternate = normAlternateTitle != null && candArtistList.any { cand ->
            cand.length >= 3 && (normAlternateTitle.contains(cand) || cand.contains(normAlternateTitle))
        }

        // If artist channel is known and candidate artist doesn't match at all, reject to prevent wrong artwork
        return artistMatchesChannel || artistMatchesVidTitle || artistMatchesAlternate
    }
}

/**
 * Scores a candidate song to find the highest-confidence match among YouTube Music search results.
 */
fun scoreSongCandidate(
    candidateTitle: String,
    candidateArtists: List<String>?,
    parsed: ParsedVideoSong,
    fullVideoTitle: String,
    videoArtist: String?,
): Float {
    var score = 0f
    val normCandTitle = normalizeString(cleanVideoTitle(candidateTitle))
    val normPrimary = normalizeString(parsed.primaryTitle)
    val normAlternate = parsed.alternateTitle?.let { normalizeString(it) }
    val candArtists = candidateArtists?.map { normalizeString(it) } ?: emptyList()
    val normFullVid = normalizeString(cleanVideoTitle(fullVideoTitle))
    val normArtist = normalizeString(videoArtist ?: "").replace(" ", "")

    // Title score (up to 10 points)
    when {
        normCandTitle == normPrimary -> score += 10f
        normAlternate != null && normCandTitle == normAlternate -> score += 9f
        normPrimary.startsWith(normCandTitle) || normCandTitle.startsWith(normPrimary) -> score += 8f
        else -> score += calculateTitleSimilarity(parsed.primaryTitle, candidateTitle) * 7f
    }

    // Artist score (up to 10 points)
    val hasArtistInChannel = candArtists.any { artist ->
        val clean = artist.replace(" ", "")
        clean.length >= 3 && normArtist.contains(clean)
    }
    if (hasArtistInChannel) score += 6f

    val hasArtistInTitle = candArtists.any { artist ->
        artist.length >= 3 && normFullVid.contains(artist)
    }
    if (hasArtistInTitle) score += 5f

    // Segment score: does candidate title or album match any segment?
    val matchesSegment = parsed.allSegments.any { seg ->
        val normSeg = normalizeString(seg)
        normSeg.length >= 3 && (normSeg == normCandTitle || normCandTitle.contains(normSeg))
    }
    if (matchesSegment) score += 3f

    return score
}

