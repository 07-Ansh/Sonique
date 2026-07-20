package com.sonique.data.lyrics.providers

import com.sonique.domain.lyrics.LyricsProvider
import com.sonique.domain.lyrics.utils.TTMLParser
import com.sonique.ktorext.getEngine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLQueryComponent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

@Serializable
private data class PaxsenixSearchResult(
    val id: String,
    val songName: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Int? = null,
    val artwork: String? = null
) {
    val displayName: String get() = trackName ?: songName ?: ""
    val displayArtist: String get() = artistName ?: ""
}

@Serializable
private data class PaxsenixLyricText(
    val text: String,
    val timestamp: Long,
    val endtime: Long,
    val duration: Long,
    val part: Boolean = false
)

@Serializable
private data class PaxsenixLyricsContent(
    val timestamp: Long,
    val endtime: Long,
    val duration: Long,
    val structure: String? = null,
    val text: List<PaxsenixLyricText> = emptyList(),
    val background: Boolean = false,
    val backgroundText: List<PaxsenixLyricText> = emptyList(),
    val oppositeTurn: Boolean = false
)

@Serializable
private data class PaxsenixLyricsMetadata(
    val songwriters: List<String> = emptyList()
)

@Serializable
private data class PaxsenixLyricsResponse(
    val type: String? = null,
    val metadata: PaxsenixLyricsMetadata? = null,
    val content: List<PaxsenixLyricsContent> = emptyList(),
    val elrc: String? = null,
    val elrcMultiPerson: String? = null,
    val ttmlContent: String? = null,
    val plain: String? = null
)

@Serializable
private data class PaxsenixAppleMusicSearchResponse(
    val results: PaxsenixAppleMusicResults,
    val resources: PaxsenixAppleMusicResources? = null
)

@Serializable
private data class PaxsenixAppleMusicResults(
    val songs: PaxsenixAppleMusicSongsResult? = null
)

@Serializable
private data class PaxsenixAppleMusicSongsResult(
    val data: List<PaxsenixAppleMusicSongData> = emptyList()
)

@Serializable
private data class PaxsenixAppleMusicSongData(
    val id: String,
    val type: String
)

@Serializable
private data class PaxsenixAppleMusicResources(
    val songs: Map<String, PaxsenixAppleMusicSongDetail>? = null
)

@Serializable
private data class PaxsenixAppleMusicSongDetail(
    val attributes: PaxsenixAppleMusicSongAttributes
)

@Serializable
private data class PaxsenixAppleMusicSongAttributes(
    val name: String,
    val artistName: String,
    val albumName: String? = null,
    val artwork: PaxsenixAppleMusicArtwork? = null,
    val url: String? = null,
    val durationInMillis: Long? = null
)

@Serializable
private data class PaxsenixAppleMusicArtwork(
    val url: String
)

object PaxsenixLyricsProvider : LyricsProvider {
    override val name = "Paxsenix"

    private val client by lazy {
        HttpClient(getEngine()) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }
            defaultRequest {
                url("https://lyrics.paxsenix.org")
                header("User-Agent", "Sonique/1.0")
            }
            expectSuccess = true
        }
    }

    private const val APPLE_MUSIC_API_BASE = "https://amp-api.music.apple.com/v1/catalog/us"
    private val appleJson = Json { ignoreUnknownKeys = true }
    private val appleTokenManager by lazy { AppleTokenManager(client) }

    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\([^)]*\d{4}[^)]*\)""", RegexOption.IGNORE_CASE),
    )

    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private suspend fun search(query: String): List<PaxsenixSearchResult> = runCatching {
        val token = appleTokenManager.getToken()
        return@runCatching searchWithToken(token, query)
    }.getOrElse { e ->
        if (e is ClientRequestException && e.response.status.value == 401) {
            appleTokenManager.clearToken()
            return@getOrElse runCatching {
                val newToken = appleTokenManager.getToken()
                searchWithToken(newToken, query)
            }.getOrElse { e2 ->
                emptyList()
            }
        }
        emptyList()
    }

    private suspend fun searchWithToken(token: String, query: String): List<PaxsenixSearchResult> {
        val encodedQuery = query.encodeURLQueryComponent(spaceToPlus = false)
        val response = client.get("$APPLE_MUSIC_API_BASE/search?term=$encodedQuery&types=songs&limit=25&l=en-US&platform=web&format[resources]=map&include[songs]=artists&extend=artistUrl") {
            header("Authorization", "Bearer $token")
            header("Origin", "https://music.apple.com")
            header("Referer", "https://music.apple.com/")
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0")
            header("Accept", "application/json")
            header("Accept-Language", "en-US,en;q=0.5")
            header("x-apple-renewal", "true")
        }
        val body = appleJson.decodeFromString<PaxsenixAppleMusicSearchResponse>(response.bodyAsText())
        val songs = body.results.songs?.data ?: return emptyList()
        return songs.mapNotNull { songData ->
            val detail = body.resources?.songs?.get(songData.id) ?: return@mapNotNull null
            val attr = detail.attributes
            PaxsenixSearchResult(
                id = songData.id,
                trackName = attr.name,
                artistName = attr.artistName,
                albumName = attr.albumName,
                duration = attr.durationInMillis?.toInt()?.div(1000),
                artwork = attr.artwork?.url?.replace("{w}", "100")?.replace("{h}", "100")?.replace("{f}", "png")
            )
        }
    }

    override fun isEnabled(): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = runCatching {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        
        val searchQueries = buildList {
            add("$cleanedTitle $cleanedArtist")
            add(cleanedTitle)
            if (!album.isNullOrBlank()) {
                add("$cleanedTitle $cleanedArtist $album")
            }
        }
        
        var allResults: List<Pair<PaxsenixSearchResult, Double>> = emptyList()
        for (query in searchQueries) {
            if (allResults.isEmpty()) {
                val searchResults = search(query)
                if (searchResults.isNotEmpty()) {
                    allResults = scoreAndFilterResults(searchResults, title, artist, duration)
                }
            }
        }
        
        if (allResults.isEmpty()) {
            throw IllegalStateException("No tracks found on Paxsenix")
        }

        var bestLyrics: String? = null
        var bestQuality = 0

        for ((result, score) in allResults.take(10)) {
            val lrc = fetchLyricsForTrack(result.id).getOrNull() ?: continue
            if (lrc.isEmpty()) continue
            val quality = getQuality(lrc)
            if (quality > bestQuality) {
                bestQuality = quality
                bestLyrics = lrc
            }
            if (bestQuality == 3) break
        }

        bestLyrics ?: throw IllegalStateException("No lyrics available from Paxsenix")
    }

    private fun getQuality(lrc: String): Int {
        if (lrc.isBlank()) return 0
        val hasWordTimings = (lrc.contains("<") && lrc.contains(">") && (lrc.contains("|") || lrc.contains(":"))) ||
                lrc.contains(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>"))
        if (hasWordTimings) return 3
        val hasLineTimings = lrc.contains(Regex("\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]")) ||
                lrc.contains(Regex("^\\[bg:.*\\]", RegexOption.MULTILINE))
        if (hasLineTimings) return 2
        return 1
    }

    private fun scoreAndFilterResults(
        results: List<PaxsenixSearchResult>,
        title: String,
        artist: String,
        duration: Int
    ): List<Pair<PaxsenixSearchResult, Double>> {
        val durationMs = duration * 1000
        val cleanupRegex = Regex("""\s*\(.*?\)|\s*\[.*?\]""")
        val cleanedTitle = title.replace(cleanupRegex, "").lowercase().trim()
        val cleanedArtist = cleanArtist(artist).lowercase()
        val targetIsMixed = title.contains("mixed", ignoreCase = true)
        val targetIsRemix = title.contains("remix", ignoreCase = true)
        
        return results.map { result ->
            var score = 0.0
            val resultTitle = result.displayName
            val resultArtist = result.displayArtist
            
            result.duration?.let { d ->
                val diff = abs(d * 1000 - durationMs)
                when {
                    diff <= 2000 -> score += 100
                    diff <= 5000 -> score += 50
                    diff <= 10000 -> score += 10
                    else -> score -= 50
                }
            }
            
            val resultTitleCleaned = resultTitle.replace(cleanupRegex, "").lowercase().trim()
            when {
                resultTitleCleaned == cleanedTitle -> score += 80
                resultTitleCleaned.contains(cleanedTitle) || cleanedTitle.contains(resultTitleCleaned) -> score += 40
            }
            
            val resultIsMixed = resultTitle.contains("mixed", ignoreCase = true)
            val resultIsRemix = resultTitle.contains("remix", ignoreCase = true)
            if (resultIsMixed && !targetIsMixed) score -= 60
            if (resultIsRemix && !targetIsRemix) score -= 40
            
            val resultArtistLower = resultArtist.lowercase()
            if (resultArtistLower.contains(cleanedArtist)) {
                score += 50
            } else {
                val artistWords = cleanedArtist.split(Regex("\\s+")).filter { it.length > 2 }
                if (artistWords.any { resultArtistLower.contains(it) }) {
                    score += 25
                }
            }
            result to score
        }.sortedByDescending { it.second }.filter { it.second > 0 }.take(10)
    }

    private suspend fun fetchLyricsForTrack(id: String): Result<String> = runCatching {
        val response = client.get("/apple-music/lyrics") {
            parameter("id", id)
        }.body<PaxsenixLyricsResponse>()
        
        if (!response.ttmlContent.isNullOrBlank()) {
            val lrc = convertTTMLToAppFormat(response.ttmlContent)
            if (lrc.isNotEmpty()) return@runCatching lrc
        }

        if (!response.elrcMultiPerson.isNullOrBlank()) return@runCatching response.elrcMultiPerson
        if (!response.elrc.isNullOrBlank()) return@runCatching response.elrc
        if (!response.plain.isNullOrBlank()) return@runCatching response.plain
        if (response.content.isEmpty()) throw IllegalStateException("No lyrics found")
        
        val hasWordLevel = response.type == "Syllable"
        if (!hasWordLevel) {
            return@runCatching response.content
                .map { line -> line.text.joinToString(" ") { it.text } }
                .filter { it.isNotBlank() }
                .joinToString("\n")
        }

        buildString {
            response.content.forEach { line ->
                val timeMs = line.timestamp
                val minutes = timeMs / 1000 / 60
                val seconds = (timeMs / 1000) % 60
                val centiseconds = (timeMs % 1000) / 10
                val agent = when {
                    line.background -> "{bg}"
                    line.oppositeTurn -> "{agent:v2}"
                    else -> "{agent:v1}"
                }
                val lineText = line.text.joinToString(" ") { it.text }
                if (lineText.isNotBlank()) {
                    append("[")
                    if (minutes < 10) append("0")
                    append(minutes).append(":")
                    if (seconds < 10) append("0")
                    append(seconds).append(".")
                    if (centiseconds < 10) append("0")
                    append(centiseconds).append("]").append(agent).append(lineText).append("\n")

                    if (line.text.isNotEmpty()) {
                        append("<")
                        line.text.forEachIndexed { i, word ->
                            append(word.text).append(":").append(word.timestamp.toDouble() / 1000).append(":").append(word.endtime.toDouble() / 1000)
                            if (i < line.text.lastIndex) append("|")
                        }
                        append(">\n")
                    }
                }
            }
        }
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val searchQueries = listOf(
            "$cleanedTitle $cleanedArtist",
            cleanedTitle
        )
        
        var scoredResults: List<Pair<PaxsenixSearchResult, Double>> = emptyList()
        for (query in searchQueries) {
            val results = search(query)
            if (results.isEmpty()) continue
            val filtered = scoreAndFilterResults(results, title, artist, duration)
            if (filtered.isNotEmpty()) {
                scoredResults = filtered
                break
            }
        }

        val collectedLyrics = mutableListOf<Pair<String, Int>>()
        for ((result, _) in scoredResults.take(5)) {
            val lrc = fetchLyricsForTrack(result.id).getOrNull() ?: continue
            if (lrc.isNotEmpty()) {
                val quality = getQuality(lrc)
                collectedLyrics.add(lrc to quality)
                if (quality == 3) break
            }
        }

        collectedLyrics.sortedByDescending { it.second }.forEach { (lrc, _) ->
            callback(lrc)
        }
    }

    private fun convertTTMLToAppFormat(ttml: String): String {
        return try {
            val parsedLines = TTMLParser.parseTTML(ttml)
            TTMLParser.toLRC(parsedLines)
        } catch (e: Exception) {
            ""
        }
    }

    private class AppleTokenManager(private val httpClient: HttpClient) {
        private var cachedToken: String? = null
        private val mutex = Mutex()

        suspend fun getToken(): String = mutex.withLock {
            cachedToken?.let { return it }
            try {
                val mainPageResponse = httpClient.get("https://beta.music.apple.com")
                val mainPageBody = mainPageResponse.bodyAsText()
                val indexJsRegex = Regex("""/assets/index~[^/]+\.js""")
                val indexJsMatch = indexJsRegex.find(mainPageBody)
                    ?: throw Exception("Could not find index JS URL")
                val indexJsUri = indexJsMatch.value
                val indexJsResponse = httpClient.get("https://beta.music.apple.com$indexJsUri")
                val indexJsBody = indexJsResponse.bodyAsText()
                val tokenRegex = Regex("""eyJ[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+""")
                val tokenMatch = tokenRegex.find(indexJsBody)
                    ?: throw Exception("Could not find token")
                val token = tokenMatch.value
                cachedToken = token
                return token
            } catch (e: Exception) {
                throw Exception("Error fetching Apple Music token: ${e.message}", e)
            }
        }

        fun clearToken() {
            cachedToken = null
        }
    }
}
